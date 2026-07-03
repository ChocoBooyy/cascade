package dev.chocoboy.cascade;

import dev.chocoboy.cascade.engine.effect.EffectSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

// collects several emitters into one layered effect played at a single point, the fluent twin of VfxEmitter
public final class VfxEffect {

    private final List<EmitterSpec> emitters = new ArrayList<>();

    VfxEffect() {
    }

    public VfxEffect add(VfxEmitter emitter) {
        emitters.add(emitter.spec());
        return this;
    }

    public void play(ServerLevel level, Vec3 pos) {
        Vfx.effect(level, pos, new EffectSpec(emitters));
    }
}
