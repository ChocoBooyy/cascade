package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.engine.effect.BeamState;
import dev.chocoboy.cascade.engine.effect.CollisionProbe;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.ParticleSystem;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.neoforge.net.BeamPayload;
import dev.chocoboy.cascade.neoforge.net.DomePayload;
import dev.chocoboy.cascade.neoforge.net.EffectPayload;
import dev.chocoboy.cascade.neoforge.net.EmitterPayload;
import dev.chocoboy.cascade.neoforge.net.LightPayload;
import dev.chocoboy.cascade.neoforge.net.SdfPayload;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class CascadeClientHandler {

    private CascadeClientHandler() {
    }

    public static void handleEmitter(EmitterPayload payload) {
        spawnEmitter(payload.spec(), payload.origin(), payload.seed(), ParticleQuality.density());
    }

    // build one particle system per layered emitter at the shared origin, each seeded base+index so the
    // layers differ yet the whole effect replays identically for a given seed
    public static void handleEffect(EffectPayload payload) {
        float density = ParticleQuality.density();
        List<EmitterSpec> emitters = payload.spec().emitters();
        for (int i = 0; i < emitters.size(); i++) {
            spawnEmitter(emitters.get(i), payload.origin(), payload.seed() + i, density);
        }
    }

    public static void handleBeam(BeamPayload payload) {
        Vec3f from = new Vec3f((float) payload.from().x, (float) payload.from().y, (float) payload.from().z);
        Vec3f to = new Vec3f((float) payload.to().x, (float) payload.to().y, (float) payload.to().z);
        BeamState state = payload.spec().build(from, to, new Random(payload.seed()));
        VfxRenderManager.get().spawn(new BeamEffect(state, payload.spec().color(), payload.spec().width()));
    }

    public static void handleLight(LightPayload payload) {
        VfxRenderManager.get().spawn(
                new LightSplat(payload.pos(), payload.color(), payload.radius(), payload.duration()));
    }

    public static void handleDome(DomePayload payload) {
        VfxRenderManager.get().spawn(
                new DomeField(payload.pos(), payload.radius(), payload.color(), payload.duration()));
    }

    public static void handleSdf(SdfPayload payload) {
        VfxRenderManager.get().spawn(new SdfVolumeEffect(payload.pos(), payload.spec(), payload.seed()));
    }

    private static void spawnEmitter(EmitterSpec spec, Vec3 origin, long seed, float density) {
        // eligible specs run on the gpu backend when it is enabled; density is a cpu build-cost dial,
        // so the gpu path ignores it and keeps the full count
        if (GpuSim.shouldRun(spec)) {
            VfxRenderManager.get().spawn(new GpuBurstEffect(origin, spec, seed));
            return;
        }
        CollisionProbe probe = spec.collision().enabled()
                ? new LevelCollisionProbe(Minecraft.getInstance().level, origin)
                : null;
        ParticleSystem system = spec.build(new Random(seed), probe, density);
        VfxRenderManager.get().spawn(new ParticleBurstEffect(
                origin, system, spec.render(), spec.subEmitter(), 0));
    }
}
