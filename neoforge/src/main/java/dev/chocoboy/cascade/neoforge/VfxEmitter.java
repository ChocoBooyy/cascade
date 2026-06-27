package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class VfxEmitter {

    private ShapeSpec shape = ShapeSpec.sphere(1.5f);
    private int count = 120;
    private int lifetime = 30;
    private float speed = 0.08f;
    private CurveSpec size = new CurveSpec(0.25f, 0f, Easings.EASE_OUT_QUAD);
    private CurveSpec alpha = new CurveSpec(1f, 0f, Easings.LINEAR);
    private int colorStart = 0xFFCC33;
    private int colorEnd = 0xFF3300;
    private Easings colorEase = Easings.LINEAR;

    VfxEmitter() {
    }

    public VfxEmitter shape(ShapeSpec shape) {
        this.shape = shape;
        return this;
    }

    public VfxEmitter count(int count) {
        this.count = count;
        return this;
    }

    public VfxEmitter lifetime(int ticks) {
        this.lifetime = ticks;
        return this;
    }

    public VfxEmitter speed(float speed) {
        this.speed = speed;
        return this;
    }

    public VfxEmitter size(float start, float end, Easings ease) {
        this.size = new CurveSpec(start, end, ease);
        return this;
    }

    public VfxEmitter alpha(float start, float end, Easings ease) {
        this.alpha = new CurveSpec(start, end, ease);
        return this;
    }

    public VfxEmitter color(int start, int end, Easings ease) {
        this.colorStart = start;
        this.colorEnd = end;
        this.colorEase = ease;
        return this;
    }

    public EmitterSpec spec() {
        return new EmitterSpec(shape, count, lifetime, speed, size, alpha, colorStart, colorEnd, colorEase);
    }

    public void play(ServerLevel level, Vec3 pos) {
        Vfx.emit(level, pos, spec());
    }
}
