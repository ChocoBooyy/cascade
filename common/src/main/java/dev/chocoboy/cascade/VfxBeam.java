package dev.chocoboy.cascade;

import dev.chocoboy.cascade.engine.effect.BeamSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Builds a lightning-style beam between two points. Obtain with {@code Vfx.beam()}; every setter starts from
 * the built-in bolt, so override only what differs, then {@link #play}.
 */
public final class VfxBeam {

    private int color;
    private float width;
    private float arc;
    private int duration;
    private int segments;

    // start from the default bolt so a caller overrides only what it wants, with one source of truth
    VfxBeam() {
        BeamSpec d = BeamSpec.defaultBolt();
        this.color = d.color();
        this.width = d.width();
        this.arc = d.arc();
        this.duration = d.duration();
        this.segments = d.segments();
    }

    public VfxBeam color(int color) {
        this.color = color;
        return this;
    }

    public VfxBeam width(float width) {
        this.width = width;
        return this;
    }

    /** How far the bolt bows off the straight line between its endpoints. 0 is a taut line. */
    public VfxBeam arc(float arc) {
        this.arc = arc;
        return this;
    }

    public VfxBeam duration(int ticks) {
        this.duration = ticks;
        return this;
    }

    /** Number of straight pieces the bolt is split into. More segments read as a jaggeder arc. */
    public VfxBeam segments(int segments) {
        this.segments = segments;
        return this;
    }

    public BeamSpec spec() {
        return new BeamSpec(color, width, arc, duration, segments);
    }

    /** Sends the beam from {@code from} to {@code to} for nearby players. */
    public void play(ServerLevel level, Vec3 from, Vec3 to) {
        Vfx.beam(level, from, to, spec());
    }
}
