package dev.chocoboy.cascade;

import dev.chocoboy.cascade.engine.effect.SdfSpec;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Builds a raymarched signed-distance volume: primitive shapes in effect-local space fused by a smooth union.
 * Obtain with {@code Vfx.sdf()}. Shape offsets and sizes are in blocks; add at least one shape, then
 * {@link #play}.
 */
public final class VfxSdf {

    private final List<SdfSpec.SdfShape> shapes = new ArrayList<>();
    private float smoothness = 0.25f;
    private ColorSpec color = ColorSpec.of(0x66CCFF, 0xFFFFFF, Easings.LINEAR);
    private float alpha = 1f;
    private int duration = 100;
    private float rotateSpeed;

    VfxSdf() {
    }

    /** Adds a sphere of the given radius centered at the effect-local offset. */
    public VfxSdf sphere(float x, float y, float z, float radius) {
        shapes.add(new SdfSpec.SdfShape(SdfSpec.Type.SPHERE, new Vec3f(x, y, z),
                new Vec3f(radius, 0f, 0f)));
        return this;
    }

    /** Adds a box with the given half extents centered at the effect-local offset. */
    public VfxSdf box(float x, float y, float z, float halfX, float halfY, float halfZ) {
        shapes.add(new SdfSpec.SdfShape(SdfSpec.Type.BOX, new Vec3f(x, y, z),
                new Vec3f(halfX, halfY, halfZ)));
        return this;
    }

    /** Adds a torus of the given major (ring) and minor (tube) radius centered at the effect-local offset. */
    public VfxSdf torus(float x, float y, float z, float major, float minor) {
        shapes.add(new SdfSpec.SdfShape(SdfSpec.Type.TORUS, new Vec3f(x, y, z),
                new Vec3f(major, minor, 0f)));
        return this;
    }

    /** Blend width of the smooth union; larger values fuse nearby shapes into one mass. */
    public VfxSdf smoothness(float width) {
        this.smoothness = width;
        return this;
    }

    /** Rim gradient: the first color faces the camera, the last paints the silhouette. */
    public VfxSdf gradient(Easings ease, int... colors) {
        this.color = ColorSpec.gradient(ease, colors);
        return this;
    }

    public VfxSdf alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public VfxSdf duration(int ticks) {
        this.duration = ticks;
        return this;
    }

    /** Spins the whole volume about its vertical axis at this many radians per tick. */
    public VfxSdf rotate(float radiansPerTick) {
        this.rotateSpeed = radiansPerTick;
        return this;
    }

    public SdfSpec spec() {
        return new SdfSpec(shapes, smoothness, color, alpha, duration, rotateSpeed);
    }

    /** Sends the volume to players near {@code pos}. */
    public void play(ServerLevel level, Vec3 pos) {
        Vfx.sdf(level, pos, spec());
    }
}
