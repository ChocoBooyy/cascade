package dev.chocoboy.cascade.client;

import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import net.minecraft.world.phys.Vec3;

// a burst simulated on the gpu: particle state lives in an ssbo stepped by a compute shader, drawn from
// buffer without a cpu round trip. the compute path is raw gl and survives, but the draw path drove blend
// and depth through RenderSystem's immediate state api, which 26.1 removed for the RenderPipeline model.
// the backend is disabled on this port, so GpuSim.shouldRun returns false and large bursts fall back to the
// cpu path; this stub keeps the type so the seam compiles. the full implementation lives on the 1.0.0
// branch for the re-port. see the porting notes
public final class GpuBurstEffect implements RenderedEffect {

    private final Vec3 origin;
    private final int lifetime;
    private int age;

    public GpuBurstEffect(Vec3 origin, EmitterSpec spec, long seed) {
        this.origin = origin;
        this.lifetime = spec.lifetime();
    }

    @Override
    public boolean tick() {
        return ++age >= lifetime;
    }

    @Override
    public void render(VfxFrame frame) {
        PortStubs.warnOnce("gpu-burst");
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
