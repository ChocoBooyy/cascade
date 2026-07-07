package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.chocoboy.cascade.engine.effect.AttractorSpec;
import dev.chocoboy.cascade.engine.effect.BurstSampler;
import dev.chocoboy.cascade.engine.effect.ComponentSpec;
import dev.chocoboy.cascade.engine.effect.DragSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.GravitySpec;
import dev.chocoboy.cascade.engine.effect.VortexSpec;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

// a burst emitter drawn entirely on the gpu. spawn state is sampled on the cpu with the same seed and
// rng order as the cpu sim (BurstSampler) and uploaded once as a per-effect vertex buffer, six vertices
// per particle; each frame the vertex shader re-integrates the force stack from spawn to the burst's
// age, so no state persists on the gpu and no compute pass is needed. never throws outward: any gpu
// failure frees the buffer, disables the backend, and the effect removes itself
public final class GpuBurstEffect implements RenderedEffect {

    // two vec4 attributes, repeated for each of the six corners of a particle's quad
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_PARTICLE = 6;

    private final Vec3 origin;
    private final EmitterSpec spec;
    private final long seed;
    private final int count;
    private final int lifetime;

    // per-system force slots, precomputed once from the spec
    private final int forceCount;
    private final int[] forceTypes = new int[GpuSim.MAX_FORCES];
    private final float[] forceA = new float[GpuSim.MAX_FORCES * 4];
    private final float[] forceB = new float[GpuSim.MAX_FORCES * 4];
    private final float[] colorStops = new float[GpuSim.MAX_COLOR_STOPS * 3];
    private final int colorCount;

    private int age;
    private GpuBuffer spawns;
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
        // ages exactly like a cpu particle: after the tick that carries age to lifetime, the burst is dead
        if (++age >= lifetime) {
            free();
            return true;
        }
        return false;
    }

    @Override
    public void render(VfxFrame frame) {
        if (dead || GpuSim.failed()) {
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
        if (dead) {
            return;
        }
        try {
            if (spawns == null) {
                upload();
            }
            Vector3f right = camRot.transform(new Vector3f(1f, 0f, 0f));
            Vector3f up = camRot.transform(new Vector3f(0f, 1f, 0f));
            float life = lifetime <= 0 ? 1f : (float) age / lifetime;
            boolean animate = spec.render().animate();
            int frames = animate ? ParticleAtlas.FRAMES : 1;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                Std140Builder config = Std140Builder.onStack(stack, GpuSim.CONFIG_BYTES)
                        .putMat4f(modelView)
                        .putVec4((float) (origin.x - cam.x), (float) (origin.y - cam.y),
                                (float) (origin.z - cam.z), life)
                        .putVec4(right.x, right.y, right.z, age)
                        .putVec4(up.x, up.y, up.z, frames)
                        .putVec4(spec.size().start(), spec.size().end(),
                                spec.alpha().start(), spec.alpha().end())
                        .putIVec4(spec.size().ease().ordinal(), spec.alpha().ease().ordinal(),
                                spec.color().ease().ordinal(), colorCount)
                        .putIVec4(forceCount, 0, 0, 0);
                for (int i = 0; i < GpuSim.MAX_FORCES; i++) {
                    config.putIVec4(forceTypes[i], 0, 0, 0);
                }
                for (int i = 0; i < GpuSim.MAX_FORCES; i++) {
                    config.putVec4(forceA[i * 4], forceA[i * 4 + 1], forceA[i * 4 + 2], forceA[i * 4 + 3]);
                }
                for (int i = 0; i < GpuSim.MAX_FORCES; i++) {
                    config.putVec4(forceB[i * 4], forceB[i * 4 + 1], forceB[i * 4 + 2], forceB[i * 4 + 3]);
                }
                for (int i = 0; i < GpuSim.MAX_COLOR_STOPS; i++) {
                    config.putVec4(colorStops[i * 3], colorStops[i * 3 + 1], colorStops[i * 3 + 2], 0f);
                }
                putUvCells(config, frames);
                GpuSim.draw(spawns, config.get(), count * VERTICES_PER_PARTICLE);
            }
        } catch (RuntimeException e) {
            GpuSim.fail("drawing a gpu effect", e);
            free();
        }
    }

    // the flipbook's atlas cells; unused slots stay at the still frame so an out-of-range index is benign
    private void putUvCells(Std140Builder config, int frames) {
        float[] still = ParticleAtlas.uv(spec.render().sprite());
        for (int f = 0; f < 4; f++) {
            float[] uv = f < frames && frames > 1 ? ParticleAtlas.uv(spec.render().sprite(), f) : still;
            config.putVec4(uv[0], uv[1], uv[2], uv[3]);
        }
    }

    // samples spawn state in the cpu sim's exact rng order and expands it to six vertices per particle
    private void upload() {
        ByteBuffer data = BufferUtils.createByteBuffer(count * VERTICES_PER_PARTICLE * FLOATS_PER_VERTEX * 4);
        BurstSampler.sample(spec, new Random(seed), count,
                (px, py, pz, vx, vy, vz, rotation, spin) -> {
                    for (int v = 0; v < VERTICES_PER_PARTICLE; v++) {
                        data.putFloat(px).putFloat(py).putFloat(pz).putFloat(rotation)
                                .putFloat(vx).putFloat(vy).putFloat(vz).putFloat(spin);
                    }
                });
        data.flip();
        spawns = RenderSystem.getDevice().createBuffer(() -> "cascade gpu burst spawns",
                GpuBuffer.USAGE_VERTEX, data);
    }

    @Override
    public Vec3 position() {
        return origin;
    }

    // the gpu owns the vertices, so the cpu vertex budget does not apply
    @Override
    public int drawCount() {
        return 0;
    }

    private void free() {
        dead = true;
        if (spawns != null) {
            spawns.close();
            spawns = null;
        }
    }
}
