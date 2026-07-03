package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.chocoboy.cascade.engine.effect.AttractorSpec;
import dev.chocoboy.cascade.engine.effect.BurstSampler;
import dev.chocoboy.cascade.engine.effect.ComponentSpec;
import dev.chocoboy.cascade.engine.effect.DragSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.GravitySpec;
import dev.chocoboy.cascade.engine.effect.VortexSpec;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL43C;

// a burst emitter simulated and drawn entirely on the gpu. spawn state is sampled on the cpu with the
// same seed and rng order as the cpu sim (BurstSampler), uploaded to an ssbo once, then a compute pass
// advances it per client tick and the draw pulls vertices straight from the buffer. never throws
// outward: any gl failure frees the buffer, disables the backend, and the effect removes itself
public final class GpuBurstEffect implements RenderedEffect {

    private static final int FLOATS_PER_PARTICLE = 8;
    private static final int GROUP_SIZE = 64;

    private final Vec3 origin;
    private final EmitterSpec spec;
    private final long seed;
    private final int count;
    private final int lifetime;

    // per-system force uniforms, precomputed once from the spec
    private final int forceCount;
    private final int[] forceTypes = new int[GpuSim.MAX_FORCES];
    private final float[] forceA = new float[GpuSim.MAX_FORCES * 4];
    private final float[] forceB = new float[GpuSim.MAX_FORCES * 4];
    private final float[] colorStops = new float[GpuSim.MAX_COLOR_STOPS * 3];
    private final int colorCount;

    private int age;
    private int ssbo = -1;
    private boolean dead;

    public GpuBurstEffect(Vec3 origin, EmitterSpec spec, long seed) {
        this.origin = origin;
        this.spec = spec;
        this.seed = seed;
        this.count = spec.count();
        this.lifetime = spec.lifetime();
        List<ComponentSpec> modifiers = spec.modifiers();
        this.forceCount = modifiers.size();
        for (int i = 0; i < forceCount; i++) {
            encodeForce(i, modifiers.get(i));
        }
        List<Integer> stops = spec.color().stops();
        this.colorCount = stops.size();
        for (int i = 0; i < colorCount; i++) {
            int rgb = stops.get(i);
            colorStops[i * 3] = ((rgb >> 16) & 0xFF) / 255f;
            colorStops[i * 3 + 1] = ((rgb >> 8) & 0xFF) / 255f;
            colorStops[i * 3 + 2] = (rgb & 0xFF) / 255f;
        }
    }

    private void encodeForce(int slot, ComponentSpec c) {
        int a = slot * 4;
        if (c instanceof GravitySpec g) {
            forceTypes[slot] = 1;
            forceA[a] = g.accel().x();
            forceA[a + 1] = g.accel().y();
            forceA[a + 2] = g.accel().z();
        } else if (c instanceof DragSpec d) {
            forceTypes[slot] = 2;
            forceA[a] = 1f - d.drag();
        } else if (c instanceof AttractorSpec at) {
            forceTypes[slot] = 3;
            forceA[a] = at.center().x();
            forceA[a + 1] = at.center().y();
            forceA[a + 2] = at.center().z();
            forceB[a] = at.strength();
        } else if (c instanceof VortexSpec v) {
            forceTypes[slot] = 4;
            forceA[a] = v.center().x();
            forceA[a + 1] = v.center().y();
            forceA[a + 2] = v.center().z();
            forceB[a] = v.strength();
        }
    }

    @Override
    public boolean tick() {
        if (dead || GpuSim.failed()) {
            free();
            return true;
        }
        try {
            if (ssbo == -1) {
                upload();
            }
            dispatch();
        } catch (RuntimeException e) {
            GpuSim.fail("ticking a gpu effect", e);
            free();
            return true;
        }
        // ages exactly like a cpu particle: after the tick that carries age to lifetime, the burst is dead
        if (++age >= lifetime) {
            free();
            return true;
        }
        return false;
    }

    private void upload() {
        GpuSim.ensureBuilt();
        Uniforms.ensure();
        FloatBuffer data = BufferUtils.createFloatBuffer(count * FLOATS_PER_PARTICLE);
        BurstSampler.sample(spec, new Random(seed), count,
                (px, py, pz, vx, vy, vz, rotation, spin) -> data.put(px).put(py).put(pz).put(rotation)
                        .put(vx).put(vy).put(vz).put(spin));
        data.flip();
        ssbo = GL43C.glGenBuffers();
        GL43C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, ssbo);
        GL43C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, data, GL43C.GL_DYNAMIC_COPY);
        GL43C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
    }

    private void dispatch() {
        GL43C.glUseProgram(GpuSim.computeProgram);
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, ssbo);
        GL43C.glUniform1i(Uniforms.cForceCount, forceCount);
        GL43C.glUniform1iv(Uniforms.cForceType, forceTypes);
        GL43C.glUniform4fv(Uniforms.cForceA, forceA);
        GL43C.glUniform4fv(Uniforms.cForceB, forceB);
        GL43C.glUniform1ui(Uniforms.cCount, count);
        GL43C.glDispatchCompute((count + GROUP_SIZE - 1) / GROUP_SIZE, 1, 1);
        GL43C.glMemoryBarrier(GL43C.GL_SHADER_STORAGE_BARRIER_BIT);
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, 0);
        GL43C.glUseProgram(0);
    }

    @Override
    public void render(VfxFrame frame) {
        if (ssbo == -1) {
            return;
        }
        ParticleAtlas.ensureUploaded();
        // bake the frame's pose into the modelview like the cpu vertex path does, and copy it now: the
        // queue plays this back after submission, when the stack may have moved on
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(frame.pose().last().pose());
        Vec3 cam = frame.cameraPos();
        Quaternionf camRot = frame.cameraRotation();
        frame.queue().submitSelf(() -> draw(modelView, cam, camRot));
    }

    private void draw(Matrix4f modelView, Vec3 cam, Quaternionf camRot) {
        if (dead || ssbo == -1) {
            return;
        }
        try {
            Vector3f right = camRot.transform(new Vector3f(1f, 0f, 0f));
            Vector3f up = camRot.transform(new Vector3f(0f, 1f, 0f));
            float life = lifetime <= 0 ? 1f : (float) age / lifetime;

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            // billboards draw both ways like the NO_CULL render types; with culling left on, a winding
            // mistake silently erases every quad
            RenderSystem.disableCull();
            int texture = Minecraft.getInstance().getTextureManager()
                    .getTexture(ParticleAtlas.textureId()).getId();
            GlStateManager._activeTexture(GL43C.GL_TEXTURE0);
            GlStateManager._bindTexture(texture);

            GL43C.glUseProgram(GpuSim.drawProgram);
            GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, ssbo);
            GL43C.glUniformMatrix4fv(Uniforms.dModelView, false, modelView.get(new float[16]));
            GL43C.glUniformMatrix4fv(Uniforms.dProj, false, RenderSystem.getProjectionMatrix().get(new float[16]));
            GL43C.glUniform3f(Uniforms.dOffset, (float) (origin.x - cam.x), (float) (origin.y - cam.y),
                    (float) (origin.z - cam.z));
            GL43C.glUniform3f(Uniforms.dCamRight, right.x, right.y, right.z);
            GL43C.glUniform3f(Uniforms.dCamUp, up.x, up.y, up.z);
            GL43C.glUniform1f(Uniforms.dLife, life);
            GL43C.glUniform2f(Uniforms.dSize, spec.size().start(), spec.size().end());
            GL43C.glUniform1i(Uniforms.dSizeEase, spec.size().ease().ordinal());
            GL43C.glUniform2f(Uniforms.dAlpha, spec.alpha().start(), spec.alpha().end());
            GL43C.glUniform1i(Uniforms.dAlphaEase, spec.alpha().ease().ordinal());
            GL43C.glUniform3fv(Uniforms.dColorStops, colorStops);
            GL43C.glUniform1i(Uniforms.dColorCount, colorCount);
            GL43C.glUniform1i(Uniforms.dColorEase, spec.color().ease().ordinal());
            uploadUvCells();
            GL43C.glUniform1i(Uniforms.dSampler, 0);
            GL43C.glBindVertexArray(GpuSim.vao);
            GL43C.glDrawArrays(GL43C.GL_TRIANGLES, 0, count * 6);
            GL43C.glBindVertexArray(0);
            GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, 0);
            GL43C.glUseProgram(0);
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        } catch (RuntimeException e) {
            GpuSim.fail("drawing a gpu effect", e);
            free();
        }
    }

    private void uploadUvCells() {
        boolean animate = spec.render().animate();
        int frames = animate ? ParticleAtlas.FRAMES : 1;
        float[] cells = new float[4 * 4];
        for (int f = 0; f < frames; f++) {
            float[] uv = animate ? ParticleAtlas.uv(spec.render().sprite(), f)
                    : ParticleAtlas.uv(spec.render().sprite());
            System.arraycopy(uv, 0, cells, f * 4, 4);
        }
        GL43C.glUniform4fv(Uniforms.dUvCell, cells);
        GL43C.glUniform1i(Uniforms.dFrames, frames);
    }

    private void free() {
        dead = true;
        if (ssbo != -1) {
            GL43C.glDeleteBuffers(ssbo);
            ssbo = -1;
        }
    }

    @Override
    public Vec3 position() {
        return origin;
    }

    // gpu particles never touch the cpu vertex budget, which exists to cap frame build cost
    @Override
    public int drawCount() {
        return 0;
    }

    // uniform locations, looked up once after the shared programs exist
    private static final class Uniforms {
        static int cForceCount;
        static int cForceType;
        static int cForceA;
        static int cForceB;
        static int cCount;
        static int dModelView;
        static int dProj;
        static int dOffset;
        static int dCamRight;
        static int dCamUp;
        static int dLife;
        static int dSize;
        static int dSizeEase;
        static int dAlpha;
        static int dAlphaEase;
        static int dColorStops;
        static int dColorCount;
        static int dColorEase;
        static int dUvCell;
        static int dFrames;
        static int dSampler;
        private static boolean cached;

        static void ensure() {
            if (cached) {
                return;
            }
            int c = GpuSim.computeProgram;
            cForceCount = GL43C.glGetUniformLocation(c, "ForceCount");
            cForceType = GL43C.glGetUniformLocation(c, "ForceType");
            cForceA = GL43C.glGetUniformLocation(c, "ForceA");
            cForceB = GL43C.glGetUniformLocation(c, "ForceB");
            cCount = GL43C.glGetUniformLocation(c, "Count");
            int d = GpuSim.drawProgram;
            dModelView = GL43C.glGetUniformLocation(d, "ModelViewMat");
            dProj = GL43C.glGetUniformLocation(d, "ProjMat");
            dOffset = GL43C.glGetUniformLocation(d, "Offset");
            dCamRight = GL43C.glGetUniformLocation(d, "CamRight");
            dCamUp = GL43C.glGetUniformLocation(d, "CamUp");
            dLife = GL43C.glGetUniformLocation(d, "Life");
            dSize = GL43C.glGetUniformLocation(d, "Size");
            dSizeEase = GL43C.glGetUniformLocation(d, "SizeEase");
            dAlpha = GL43C.glGetUniformLocation(d, "Alpha");
            dAlphaEase = GL43C.glGetUniformLocation(d, "AlphaEase");
            dColorStops = GL43C.glGetUniformLocation(d, "ColorStops");
            dColorCount = GL43C.glGetUniformLocation(d, "ColorCount");
            dColorEase = GL43C.glGetUniformLocation(d, "ColorEase");
            dUvCell = GL43C.glGetUniformLocation(d, "UvCell");
            dFrames = GL43C.glGetUniformLocation(d, "Frames");
            dSampler = GL43C.glGetUniformLocation(d, "Sampler0");
            cached = true;
        }
    }
}
