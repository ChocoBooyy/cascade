package dev.chocoboy.cascade.client;

import dev.chocoboy.cascade.engine.effect.SdfSpec;
import net.minecraft.world.phys.Vec3;

// a raymarched signed-distance volume. the full effect sphere-traces the spec in a fragment shader through
// hand-managed gl state (RenderSystem's immediate blend/depth toggles, raw glUseProgram). 26.1 removed that
// immediate gl api in favour of the RenderPipeline / GpuDevice model, so the backend is disabled on this
// port: the effect still fires and expires on schedule, it just draws nothing. the full implementation
// lives on the 1.0.0 branch for the re-port. see the porting notes
public final class SdfVolumeEffect implements RenderedEffect {

    private final Vec3 origin;
    private final int duration;
    private int age;

    public SdfVolumeEffect(Vec3 origin, SdfSpec spec, long seed) {
        this.origin = origin;
        this.duration = spec.duration();
    }

    @Override
    public boolean tick() {
        return ++age >= duration;
    }

    @Override
    public void render(VfxFrame frame) {
        PortStubs.warnOnce("sdf-volume");
    }

    @Override
    public Vec3 position() {
        return origin;
    }

    @Override
    public int drawCount() {
        return 0;
    }
}
