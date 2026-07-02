package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.engine.effect.BeamSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

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

    public VfxBeam arc(float arc) {
        this.arc = arc;
        return this;
    }

    public VfxBeam duration(int ticks) {
        this.duration = ticks;
        return this;
    }

    public VfxBeam segments(int segments) {
        this.segments = segments;
        return this;
    }

    public BeamSpec spec() {
        return new BeamSpec(color, width, arc, duration, segments);
    }

    public void play(ServerLevel level, Vec3 from, Vec3 to) {
        Vfx.beam(level, from, to, spec());
    }
}
