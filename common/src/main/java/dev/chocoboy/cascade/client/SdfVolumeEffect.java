package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.chocoboy.cascade.engine.effect.SdfSpec;
import java.util.List;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL32C;

// a raymarched signed-distance volume: one camera-facing quad whose fragment shader sphere-traces the
// spec's smooth-union of shapes. shape data is baked to uniform arrays once; per frame only the spin
// angle and fade envelope change. never throws outward: any gl failure disables the sdf backend and
// the effect removes itself
public final class SdfVolumeEffect implements RenderedEffect {

    private final Vec3 origin;
    private final SdfSpec spec;
    private final float bound;
    // seed only offsets the spin phase, so replays of the same spec do not rotate in lockstep
    private final float phase;

    private final int shapeCount;
    private final int[] shapeTypes = new int[SdfSpec.MAX_SHAPES];
    private final float[] shapeCenters = new float[SdfSpec.MAX_SHAPES * 3];
    private final float[] shapeSizes = new float[SdfSpec.MAX_SHAPES * 3];
    private final float[] colorStops = new float[SdfFx.MAX_COLOR_STOPS * 3];
    private final int colorCount;

    private int age;

    public SdfVolumeEffect(Vec3 origin, SdfSpec spec, long seed) {
        this.origin = origin;
        this.spec = spec;
        this.bound = spec.boundRadius();
        this.phase = new Random(seed).nextFloat() * (float) (Math.PI * 2.0);
        List<SdfSpec.SdfShape> shapes = spec.shapes();
        this.shapeCount = shapes.size();
        for (int i = 0; i < shapeCount; i++) {
            SdfSpec.SdfShape s = shapes.get(i);
            shapeTypes[i] = s.type().ordinal();
            shapeCenters[i * 3] = s.center().x();
            shapeCenters[i * 3 + 1] = s.center().y();
            shapeCenters[i * 3 + 2] = s.center().z();
            shapeSizes[i * 3] = s.size().x();
            shapeSizes[i * 3 + 1] = s.size().y();
            shapeSizes[i * 3 + 2] = s.size().z();
        }
        List<Integer> stops = spec.color().stops();
        this.colorCount = Math.min(stops.size(), SdfFx.MAX_COLOR_STOPS);
        for (int i = 0; i < colorCount; i++) {
            int rgb = stops.get(i);
            colorStops[i * 3] = ((rgb >> 16) & 0xFF) / 255f;
            colorStops[i * 3 + 1] = ((rgb >> 8) & 0xFF) / 255f;
            colorStops[i * 3 + 2] = (rgb & 0xFF) / 255f;
        }
    }

    @Override
    public boolean tick() {
        return SdfFx.failed() || ++age >= spec.duration();
    }

    @Override
    public void render(VfxFrame frame) {
        if (SdfFx.failed()) {
            return;
        }
        // bake the frame's pose into the modelview like the cpu vertex path does, and copy it now: the
        // queue plays this back after submission, when the stack may have moved on
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(frame.pose().last().pose());
        Vec3 cam = frame.cameraPos();
        Quaternionf camRot = frame.cameraRotation();
        frame.queue().submitSelf(() -> draw(modelView, cam, camRot));
    }

    private void draw(Matrix4f modelView, Vec3 cam, Quaternionf camRot) {
        try {
            SdfFx.ensureBuilt();
            Uniforms.ensure();
            Vector3f right = camRot.transform(new Vector3f(1f, 0f, 0f));
            Vector3f up = camRot.transform(new Vector3f(0f, 1f, 0f));
            float life = (float) age / spec.duration();
            // fade in fast, out slow, so a volume never pops
            float envelope = Math.min(1f, Math.min(life / 0.1f, (1f - life) / 0.25f));
            float angle = phase + age * spec.rotateSpeed();

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            // billboards draw both ways like the NO_CULL render types; with culling left on, a winding
            // mistake silently erases the quad
            RenderSystem.disableCull();

            GL32C.glUseProgram(SdfFx.program);
            GL32C.glUniformMatrix4fv(Uniforms.modelView, false, modelView.get(new float[16]));
            GL32C.glUniformMatrix4fv(Uniforms.proj, false, RenderSystem.getProjectionMatrix().get(new float[16]));
            GL32C.glUniform3f(Uniforms.center, (float) (origin.x - cam.x), (float) (origin.y - cam.y),
                    (float) (origin.z - cam.z));
            GL32C.glUniform3f(Uniforms.camRight, right.x, right.y, right.z);
            GL32C.glUniform3f(Uniforms.camUp, up.x, up.y, up.z);
            // margin so the sphere's screen footprint near the camera still fits the quad
            GL32C.glUniform1f(Uniforms.bound, bound * 1.2f);
            GL32C.glUniform1f(Uniforms.angle, angle);
            GL32C.glUniform1i(Uniforms.shapeCount, shapeCount);
            GL32C.glUniform1iv(Uniforms.shapeType, shapeTypes);
            GL32C.glUniform3fv(Uniforms.shapeCenter, shapeCenters);
            GL32C.glUniform3fv(Uniforms.shapeSize, shapeSizes);
            // the polynomial smooth min divides by its width, so zero must not reach the shader
            GL32C.glUniform1f(Uniforms.smooth, Math.max(spec.smoothness(), 1e-4f));
            GL32C.glUniform3fv(Uniforms.colorStops, colorStops);
            GL32C.glUniform1i(Uniforms.colorCount, colorCount);
            GL32C.glUniform1i(Uniforms.colorEase, spec.color().ease().ordinal());
            GL32C.glUniform1f(Uniforms.alpha, spec.alpha() * envelope);
            GL32C.glBindVertexArray(SdfFx.vao);
            GL32C.glDrawArrays(GL32C.GL_TRIANGLES, 0, 6);
            GL32C.glBindVertexArray(0);
            GL32C.glUseProgram(0);

            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        } catch (RuntimeException e) {
            SdfFx.fail("drawing a volume", e);
        }
    }

    @Override
    public Vec3 position() {
        return origin;
    }

    // the raymarch costs fragments, not vertices, so the cpu vertex budget does not apply
    @Override
    public int drawCount() {
        return 0;
    }

    // uniform locations, looked up once after the shared program exists
    private static final class Uniforms {
        static int modelView;
        static int proj;
        static int center;
        static int camRight;
        static int camUp;
        static int bound;
        static int angle;
        static int shapeCount;
        static int shapeType;
        static int shapeCenter;
        static int shapeSize;
        static int smooth;
        static int colorStops;
        static int colorCount;
        static int colorEase;
        static int alpha;
        private static boolean cached;

        static void ensure() {
            if (cached) {
                return;
            }
            int p = SdfFx.program;
            modelView = GL32C.glGetUniformLocation(p, "ModelViewMat");
            proj = GL32C.glGetUniformLocation(p, "ProjMat");
            center = GL32C.glGetUniformLocation(p, "Center");
            camRight = GL32C.glGetUniformLocation(p, "CamRight");
            camUp = GL32C.glGetUniformLocation(p, "CamUp");
            bound = GL32C.glGetUniformLocation(p, "Bound");
            angle = GL32C.glGetUniformLocation(p, "Angle");
            shapeCount = GL32C.glGetUniformLocation(p, "ShapeCount");
            shapeType = GL32C.glGetUniformLocation(p, "ShapeType");
            shapeCenter = GL32C.glGetUniformLocation(p, "ShapeCenter");
            shapeSize = GL32C.glGetUniformLocation(p, "ShapeSize");
            smooth = GL32C.glGetUniformLocation(p, "Smooth");
            colorStops = GL32C.glGetUniformLocation(p, "ColorStops");
            colorCount = GL32C.glGetUniformLocation(p, "ColorCount");
            colorEase = GL32C.glGetUniformLocation(p, "ColorEase");
            alpha = GL32C.glGetUniformLocation(p, "Alpha");
            cached = true;
        }
    }
}
