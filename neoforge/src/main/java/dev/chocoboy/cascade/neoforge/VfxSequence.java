package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.engine.seq.Step;
import dev.chocoboy.cascade.engine.seq.Steps;
import dev.chocoboy.cascade.engine.seq.Timeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class VfxSequence {

    private final ServerLevel level;
    private final List<Step> steps = new ArrayList<>();

    VfxSequence(ServerLevel level) {
        this.level = level;
    }

    public VfxSequence burst(Vec3 pos) {
        steps.add(Steps.run(() -> Vfx.burst(level, pos)));
        return this;
    }

    public VfxSequence beam(Vec3 from, Vec3 to) {
        steps.add(Steps.run(() -> Vfx.beam(level, from, to)));
        return this;
    }

    public VfxSequence delay(int ticks) {
        steps.add(Steps.delay(ticks));
        return this;
    }

    public void play() {
        VfxSequencer.get().schedule(new Timeline(Steps.sequence(steps.toArray(new Step[0]))));
    }
}
