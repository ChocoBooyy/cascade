package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.EmissionSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.ModifierSpec;
import dev.chocoboy.cascade.engine.effect.RotationSpec;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class VfxEmitter {

    private ShapeSpec shape;
    private int count;
    private int lifetime;
    private float speed;
    private CurveSpec size;
    private CurveSpec alpha;
    private int colorStart;
    private int colorEnd;
    private Easings colorEase;
    private final List<ModifierSpec> modifiers = new ArrayList<>();
    private EmissionSpec emission = EmissionSpec.burst();
    private BlendMode blend = BlendMode.ADDITIVE;
    private SpriteId sprite = SpriteId.GLOW;
    private RotationSpec rotation = RotationSpec.NONE;

    // start from the default burst so a caller overrides only what it wants, with one source of truth
    VfxEmitter() {
        EmitterSpec d = EmitterSpec.defaultBurst();
        this.shape = d.shape();
        this.count = d.count();
        this.lifetime = d.lifetime();
        this.speed = d.speed();
        this.size = d.size();
        this.alpha = d.alpha();
        this.colorStart = d.colorStart();
        this.colorEnd = d.colorEnd();
        this.colorEase = d.colorEase();
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

    public VfxEmitter gravity(float x, float y, float z) {
        return modifier(ModifierSpec.gravity(new Vec3f(x, y, z)));
    }

    public VfxEmitter drag(float drag) {
        return modifier(ModifierSpec.drag(drag));
    }

    public VfxEmitter turbulence(float strength, float frequency) {
        return modifier(ModifierSpec.turbulence(strength, frequency));
    }

    public VfxEmitter modifier(ModifierSpec modifier) {
        modifiers.add(modifier);
        return this;
    }

    // emit perTick particles each tick for durationTicks instead of all at once; count is then ignored
    public VfxEmitter rate(float perTick, int durationTicks) {
        this.emission = EmissionSpec.rate(perTick, durationTicks);
        return this;
    }

    public VfxEmitter blend(BlendMode blend) {
        this.blend = blend;
        return this;
    }

    public VfxEmitter sprite(SpriteId sprite) {
        this.sprite = sprite;
        return this;
    }

    // random initial roll plus a per-tick spin within the given magnitude, so sprites tumble
    public VfxEmitter spin(float spinRange) {
        this.rotation = RotationSpec.spin(spinRange);
        return this;
    }

    public EmitterSpec spec() {
        return new EmitterSpec(shape, count, lifetime, speed, size, alpha, colorStart, colorEnd, colorEase,
                List.copyOf(modifiers), emission, blend, sprite, rotation);
    }

    public void play(ServerLevel level, Vec3 pos) {
        Vfx.emit(level, pos, spec());
    }
}
