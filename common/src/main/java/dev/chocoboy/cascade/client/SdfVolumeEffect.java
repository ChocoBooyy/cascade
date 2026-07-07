package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.chocoboy.cascade.engine.effect.SdfSpec;
import java.util.List;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

// a raymarched signed-distance volume: one camera-facing quad whose fragment shader sphere-traces the
// spec's smooth-union of shapes. shape data is baked to the packed config once; per frame only the spin
// angle and fade envelope change. never throws outward: any gpu failure disables the sdf backend and
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
            Vector3f right = camRot.transform(new Vector3f(1f, 0f, 0f));
            Vector3f up = camRot.transform(new Vector3f(0f, 1f, 0f));
            float life = (float) age / spec.duration();
            // fade in fast, out slow, so a volume never pops
            float envelope = Math.min(1f, Math.min(life / 0.1f, (1f - life) / 0.25f));
            float angle = phase + age * spec.rotateSpeed();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                Std140Builder config = Std140Builder.onStack(stack, SdfFx.CONFIG_BYTES)
                        .putMat4f(modelView)
                        // margin so the sphere's screen footprint near the camera still fits the quad
                        .putVec4((float) (origin.x - cam.x), (float) (origin.y - cam.y),
                                (float) (origin.z - cam.z), bound * 1.2f)
                        .putVec4(right.x, right.y, right.z, 0f)
                        .putVec4(up.x, up.y, up.z, 0f)
                        // the polynomial smooth min divides by its width, so zero must not reach the shader
                        .putVec4(angle, Math.max(spec.smoothness(), 1e-4f), spec.alpha() * envelope, 0f)
                        .putIVec4(shapeCount, colorCount, spec.color().ease().ordinal(), 0);
                for (int i = 0; i < SdfSpec.MAX_SHAPES; i++) {
                    config.putIVec4(shapeTypes[i], 0, 0, 0);
                }
                for (int i = 0; i < SdfSpec.MAX_SHAPES; i++) {
                    config.putVec4(shapeCenters[i * 3], shapeCenters[i * 3 + 1], shapeCenters[i * 3 + 2], 0f);
                }
                for (int i = 0; i < SdfSpec.MAX_SHAPES; i++) {
                    config.putVec4(shapeSizes[i * 3], shapeSizes[i * 3 + 1], shapeSizes[i * 3 + 2], 0f);
                }
                for (int i = 0; i < SdfFx.MAX_COLOR_STOPS; i++) {
                    config.putVec4(colorStops[i * 3], colorStops[i * 3 + 1], colorStops[i * 3 + 2], 0f);
                }
                SdfFx.draw(config.get());
            }
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
}
