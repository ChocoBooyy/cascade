package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.CollisionSpec;
import dev.chocoboy.cascade.engine.effect.EmissionSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.MeshId;
import dev.chocoboy.cascade.engine.effect.ModifierSpec;
import dev.chocoboy.cascade.engine.effect.RenderSpec;
import dev.chocoboy.cascade.engine.effect.RotationSpec;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.effect.SubEmitterSpec;
import dev.chocoboy.cascade.engine.effect.TrailSpec;
import dev.chocoboy.cascade.engine.effect.VelocitySpec;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorSpec;
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
    private ColorSpec color;
    private final List<ModifierSpec> modifiers = new ArrayList<>();
    private EmissionSpec emission = EmissionSpec.burst();
    private BlendMode blend = BlendMode.ADDITIVE;
    private SpriteId sprite = SpriteId.GLOW;
    private float stretch;
    private boolean animate;
    private boolean lit;
    private MeshId mesh = MeshId.NONE;
    private RotationSpec rotation = RotationSpec.NONE;
    private CollisionSpec collision = CollisionSpec.NONE;
    private SubEmitterSpec subEmitter;
    private TrailSpec trail = TrailSpec.NONE;
    private VelocitySpec velocity = VelocitySpec.RADIAL;

    // start from the default burst so a caller overrides only what it wants, with one source of truth
    VfxEmitter() {
        EmitterSpec d = EmitterSpec.defaultBurst();
        this.shape = d.shape();
        this.count = d.count();
        this.lifetime = d.lifetime();
        this.speed = d.speed();
        this.size = d.size();
        this.alpha = d.alpha();
        this.color = d.color();
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
        this.color = ColorSpec.of(start, end, ease);
        return this;
    }

    // blend through several colors over a particle's life instead of just two, for fire and plasma
    public VfxEmitter gradient(Easings ease, int... colors) {
        this.color = ColorSpec.gradient(ease, colors);
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

    // pull particles toward a point in emitter-local space; negative strength pushes them away
    public VfxEmitter attractor(float x, float y, float z, float strength) {
        return modifier(ModifierSpec.attractor(new Vec3f(x, y, z), strength));
    }

    // swirl particles around the vertical axis through the given emitter-local point
    public VfxEmitter vortex(float x, float y, float z, float strength) {
        return modifier(ModifierSpec.vortex(new Vec3f(x, y, z), strength));
    }

    // divergence-free curl noise, a smoother fluid-like flow than plain turbulence
    public VfxEmitter curl(float strength, float frequency) {
        return modifier(ModifierSpec.curl(strength, frequency));
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

    // elongate fast particles along their velocity into streaks; 0 keeps them round
    public VfxEmitter stretch(float stretch) {
        this.stretch = stretch;
        return this;
    }

    // play the sprite's animation frames across each particle's life instead of holding the still frame
    public VfxEmitter animate() {
        this.animate = true;
        return this;
    }

    // tint by world light, for smoke and dust that should sit in shadow instead of glowing full bright
    public VfxEmitter lit() {
        this.lit = true;
        return this;
    }

    // draw particles as solid tumbling cubes instead of billboards, for chunky debris. pair with a spin
    public VfxEmitter cube() {
        this.mesh = MeshId.CUBE;
        return this;
    }

    // draw particles as elongated splinters, for shards and shrapnel. pair with a spin
    public VfxEmitter shard() {
        this.mesh = MeshId.SHARD;
        return this;
    }

    // bounce particles off solid blocks. bounce is the speed kept on a hit, friction sheds sliding speed
    public VfxEmitter collide(float bounce, float friction) {
        this.collision = CollisionSpec.bouncy(bounce, friction);
        return this;
    }

    // spawn the child system wherever one of this emitter's particles dies, for fireworks and trails
    public VfxEmitter burstOnDeath(VfxEmitter child) {
        this.subEmitter = new SubEmitterSpec(child.spec());
        return this;
    }

    // spawn the child system at the first block contact of each parent particle, for impact sparks and splashes
    public VfxEmitter burstOnCollision(VfxEmitter child) {
        this.subEmitter = new SubEmitterSpec(child.spec(), SubEmitterSpec.Trigger.COLLISION);
        return this;
    }

    // draw a ribbon through each particle's last length positions, for comets and streaking sparks
    public VfxEmitter trail(int length) {
        this.trail = TrailSpec.of(length);
        return this;
    }

    // fire particles inward toward the shape center instead of outward, for implosions
    public VfxEmitter implode() {
        this.velocity = VelocitySpec.inward();
        return this;
    }

    // fire particles along a direction within a cone of the given half angle in radians, for jets
    public VfxEmitter jet(float x, float y, float z, float spread) {
        this.velocity = VelocitySpec.directional(new Vec3f(x, y, z), spread);
        return this;
    }

    // fire particles tangent to the vertical axis so they circle the center, for swirls and discs
    public VfxEmitter orbit() {
        this.velocity = VelocitySpec.orbital();
        return this;
    }

    public EmitterSpec spec() {
        return new EmitterSpec(shape, count, lifetime, speed, size, alpha, color,
                List.copyOf(modifiers), emission, new RenderSpec(blend, sprite, stretch, animate, lit, mesh), rotation, collision,
                subEmitter, trail, velocity);
    }

    public void play(ServerLevel level, Vec3 pos) {
        Vfx.emit(level, pos, spec());
    }
}
