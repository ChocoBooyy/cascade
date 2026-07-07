package dev.chocoboy.cascade;

import dev.chocoboy.cascade.engine.seq.Step;
import dev.chocoboy.cascade.engine.seq.Steps;
import dev.chocoboy.cascade.engine.seq.Timeline;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Builds a timeline of effects on one level. Obtain with {@code Vfx.at(level)}. Each call appends a step;
 * {@link #delay} spaces them out and {@link #parallel} runs whole branches at once, so a composite effect
 * (charge, then beam, then burst) reads top to bottom. Finish with {@link #play}.
 */
public final class VfxSequence {

    private final ServerLevel level;
    private final List<Step> steps = new ArrayList<>();

    VfxSequence(ServerLevel level) {
        this.level = level;
    }

    public VfxSequence burst(Vec3 pos) {
        return run(() -> Vfx.burst(level, pos));
    }

    public VfxSequence emit(Vec3 pos, VfxEmitter emitter) {
        return run(() -> emitter.play(level, pos));
    }

    public VfxSequence effect(Vec3 pos, VfxEffect effect) {
        return run(() -> effect.play(level, pos));
    }

    public VfxSequence beam(Vec3 from, Vec3 to) {
        return run(() -> Vfx.beam(level, from, to));
    }

    public VfxSequence beam(Vec3 from, Vec3 to, VfxBeam beam) {
        return run(() -> beam.play(level, from, to));
    }

    /** Adds a camera shake step. NeoForge clients only; Fabric clients ignore it (see {@link Vfx#shake}). */
    public VfxSequence shake(Vec3 pos, float magnitude, int duration) {
        return run(() -> Vfx.shake(level, pos, magnitude, duration));
    }

    public VfxSequence light(Vec3 pos, int color, float radius, int duration) {
        return run(() -> Vfx.light(level, pos, color, radius, duration));
    }

    public VfxSequence dome(Vec3 pos, float radius, int color, int duration) {
        return run(() -> Vfx.dome(level, pos, radius, color, duration));
    }

    /** Waits {@code ticks} ticks before the next step runs. */
    public VfxSequence delay(int ticks) {
        steps.add(Steps.delay(ticks));
        return this;
    }

    /** Runs an arbitrary action at this point in the timeline, the escape hatch for anything not covered above. */
    public VfxSequence run(Runnable action) {
        steps.add(Steps.run(action));
        return this;
    }

    /** Plays several branches at once; the timeline continues only after the longest branch finishes. */
    @SafeVarargs
    public final VfxSequence parallel(Consumer<VfxSequence>... branches) {
        Step[] branchSteps = new Step[branches.length];
        for (int i = 0; i < branches.length; i++) {
            VfxSequence branch = new VfxSequence(level);
            branches[i].accept(branch);
            branchSteps[i] = Steps.sequence(branch.steps.toArray(new Step[0]));
        }
        steps.add(Steps.parallel(branchSteps));
        return this;
    }

    /** Schedules the timeline; it begins running on the next server tick. */
    public void play() {
        VfxSequencer.get().schedule(new Timeline(Steps.sequence(steps.toArray(new Step[0]))));
    }
}
