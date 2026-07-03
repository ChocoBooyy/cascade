package dev.chocoboy.cascade;

import dev.chocoboy.cascade.engine.effect.EffectSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Collects several {@link VfxEmitter}s into one layered effect played at a single point, the fluent twin of
 * {@code VfxEmitter}. Obtain with {@code Vfx.effect()}, add emitters, then {@link #play}.
 */
public final class VfxEffect {

    private final List<EmitterSpec> emitters = new ArrayList<>();

    VfxEffect() {
    }

    /** Adds an emitter as one layer of the effect. */
    public VfxEffect add(VfxEmitter emitter) {
        emitters.add(emitter.spec());
        return this;
    }

    /** Sends the assembled effect to players near {@code pos}. */
    public void play(ServerLevel level, Vec3 pos) {
        Vfx.effect(level, pos, new EffectSpec(emitters));
    }
}
