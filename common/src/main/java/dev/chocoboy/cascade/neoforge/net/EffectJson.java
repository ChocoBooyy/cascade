package dev.chocoboy.cascade.neoforge.net;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.chocoboy.cascade.engine.effect.AttractorSpec;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.CollisionSpec;
import dev.chocoboy.cascade.engine.effect.ComponentSpec;
import dev.chocoboy.cascade.engine.effect.CurlSpec;
import dev.chocoboy.cascade.engine.effect.DragSpec;
import dev.chocoboy.cascade.engine.effect.EffectSpec;
import dev.chocoboy.cascade.engine.effect.EmissionSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.GravitySpec;
import dev.chocoboy.cascade.engine.effect.MeshId;
import dev.chocoboy.cascade.engine.effect.RenderSpec;
import dev.chocoboy.cascade.engine.effect.RotationSpec;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.effect.SubEmitterSpec;
import dev.chocoboy.cascade.engine.effect.TrailSpec;
import dev.chocoboy.cascade.engine.effect.TurbulenceSpec;
import dev.chocoboy.cascade.engine.effect.VelocitySpec;
import dev.chocoboy.cascade.engine.effect.VortexSpec;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorSpec;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// DataFixerUpper codecs, the JSON twin of the network SpecCodecs, so datapacks can author effects.
// Enums serialize by name for readable JSON; fields with natural defaults are optional so authors
// only spell out what they change.
public final class EffectJson {

    private static <E extends Enum<E>> Codec<E> byName(Class<E> type) {
        return Codec.STRING.xmap(s -> Enum.valueOf(type, s), Enum::name);
    }

    public static final Codec<Vec3f> VEC3F = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("x").forGetter(Vec3f::x),
            Codec.FLOAT.fieldOf("y").forGetter(Vec3f::y),
            Codec.FLOAT.fieldOf("z").forGetter(Vec3f::z)
    ).apply(i, Vec3f::new));

    public static final Codec<ShapeSpec> SHAPE = RecordCodecBuilder.create(i -> i.group(
            byName(ShapeSpec.Kind.class).fieldOf("kind").forGetter(ShapeSpec::kind),
            Codec.FLOAT.optionalFieldOf("radius", 0f).forGetter(ShapeSpec::radius),
            Codec.FLOAT.optionalFieldOf("height", 0f).forGetter(ShapeSpec::height),
            VEC3F.optionalFieldOf("a", Vec3f.ZERO).forGetter(ShapeSpec::a),
            VEC3F.optionalFieldOf("b", Vec3f.ZERO).forGetter(ShapeSpec::b)
    ).apply(i, ShapeSpec::new));

    public static final Codec<CurveSpec> CURVE = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("start").forGetter(CurveSpec::start),
            Codec.FLOAT.fieldOf("end").forGetter(CurveSpec::end),
            byName(Easings.class).fieldOf("ease").forGetter(CurveSpec::ease)
    ).apply(i, CurveSpec::new));

    public static final Codec<ColorSpec> COLOR = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.listOf().fieldOf("stops").forGetter(ColorSpec::stops),
            byName(Easings.class).optionalFieldOf("ease", Easings.LINEAR).forGetter(ColorSpec::ease)
    ).apply(i, ColorSpec::new));

    public static final Codec<VelocitySpec> VELOCITY = RecordCodecBuilder.create(i -> i.group(
            byName(VelocitySpec.Mode.class).optionalFieldOf("mode", VelocitySpec.Mode.RADIAL).forGetter(VelocitySpec::mode),
            VEC3F.optionalFieldOf("direction", Vec3f.ZERO).forGetter(VelocitySpec::direction),
            Codec.FLOAT.optionalFieldOf("spread", 0f).forGetter(VelocitySpec::spread)
    ).apply(i, VelocitySpec::new));

    private static final MapCodec<GravitySpec> GRAVITY = RecordCodecBuilder.mapCodec(i -> i.group(
            VEC3F.fieldOf("accel").forGetter(GravitySpec::accel)
    ).apply(i, GravitySpec::new));

    private static final MapCodec<DragSpec> DRAG = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("drag").forGetter(DragSpec::drag)
    ).apply(i, DragSpec::new));

    private static final MapCodec<TurbulenceSpec> TURBULENCE = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("strength").forGetter(TurbulenceSpec::strength),
            Codec.FLOAT.fieldOf("frequency").forGetter(TurbulenceSpec::frequency)
    ).apply(i, TurbulenceSpec::new));

    private static final MapCodec<AttractorSpec> ATTRACTOR = RecordCodecBuilder.mapCodec(i -> i.group(
            VEC3F.fieldOf("center").forGetter(AttractorSpec::center),
            Codec.FLOAT.fieldOf("strength").forGetter(AttractorSpec::strength)
    ).apply(i, AttractorSpec::new));

    private static final MapCodec<VortexSpec> VORTEX = RecordCodecBuilder.mapCodec(i -> i.group(
            VEC3F.fieldOf("center").forGetter(VortexSpec::center),
            Codec.FLOAT.fieldOf("strength").forGetter(VortexSpec::strength)
    ).apply(i, VortexSpec::new));

    private static final MapCodec<CurlSpec> CURL = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("strength").forGetter(CurlSpec::strength),
            Codec.FLOAT.fieldOf("frequency").forGetter(CurlSpec::frequency)
    ).apply(i, CurlSpec::new));

    private static final Map<String, MapCodec<? extends ComponentSpec>> COMPONENTS = Map.of(
            "gravity", GRAVITY,
            "drag", DRAG,
            "turbulence", TURBULENCE,
            "attractor", ATTRACTOR,
            "vortex", VORTEX,
            "curl", CURL);

    public static final Codec<ComponentSpec> COMPONENT = Codec.STRING.<ComponentSpec>partialDispatch(
            "type",
            spec -> DataResult.success(spec.typeId()),
            type -> {
                MapCodec<? extends ComponentSpec> codec = COMPONENTS.get(type);
                return codec != null
                        ? DataResult.success(codec)
                        : DataResult.error(() -> "unknown component " + type);
            });

    public static final Codec<EmissionSpec> EMISSION = RecordCodecBuilder.create(i -> i.group(
            byName(EmissionSpec.Mode.class).fieldOf("mode").forGetter(EmissionSpec::mode),
            Codec.FLOAT.optionalFieldOf("rate", 0f).forGetter(EmissionSpec::rate),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(EmissionSpec::duration)
    ).apply(i, EmissionSpec::new));

    public static final Codec<RenderSpec> RENDER = RecordCodecBuilder.create(i -> i.group(
            byName(BlendMode.class).optionalFieldOf("blend", BlendMode.ADDITIVE).forGetter(RenderSpec::blend),
            byName(SpriteId.class).optionalFieldOf("sprite", SpriteId.GLOW).forGetter(RenderSpec::sprite),
            Codec.FLOAT.optionalFieldOf("stretch", 0f).forGetter(RenderSpec::stretch),
            Codec.BOOL.optionalFieldOf("animate", false).forGetter(RenderSpec::animate),
            Codec.BOOL.optionalFieldOf("lit", false).forGetter(RenderSpec::lit),
            byName(MeshId.class).optionalFieldOf("mesh", MeshId.NONE).forGetter(RenderSpec::mesh),
            Codec.STRING.optionalFieldOf("model", "").forGetter(RenderSpec::meshModel),
            Codec.BOOL.optionalFieldOf("soft", false).forGetter(RenderSpec::soft)
    ).apply(i, RenderSpec::new));

    public static final Codec<RotationSpec> ROTATION = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("angle_range", 0f).forGetter(RotationSpec::angleRange),
            Codec.FLOAT.optionalFieldOf("spin_range", 0f).forGetter(RotationSpec::spinRange)
    ).apply(i, RotationSpec::new));

    public static final Codec<CollisionSpec> COLLISION = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(CollisionSpec::enabled),
            Codec.FLOAT.optionalFieldOf("bounce", 0f).forGetter(CollisionSpec::bounce),
            Codec.FLOAT.optionalFieldOf("friction", 0f).forGetter(CollisionSpec::friction)
    ).apply(i, CollisionSpec::new));

    public static final Codec<TrailSpec> TRAIL = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(TrailSpec::enabled),
            Codec.INT.optionalFieldOf("length", 0).forGetter(TrailSpec::length)
    ).apply(i, TrailSpec::new));

    // Recursive because a sub-emitter wraps a child EmitterSpec. Codec.recursive hands back a self
    // reference for the sub_emitter field; the child has no default, so it stays an Optional that the
    // getter wraps and the constructor unwraps with orElse(null).
    public static final Codec<EmitterSpec> EMITTER = Codec.recursive("EmitterSpec", self -> {
        Codec<SubEmitterSpec.Trigger> subTrigger = Codec.STRING.xmap(
                s -> SubEmitterSpec.Trigger.valueOf(s.toUpperCase(java.util.Locale.ROOT)),
                t -> t.name().toLowerCase(java.util.Locale.ROOT));
        Codec<SubEmitterSpec> subEmitterCodec = RecordCodecBuilder.create(i -> i.group(
                self.fieldOf("child").forGetter(SubEmitterSpec::child),
                subTrigger.optionalFieldOf("trigger", SubEmitterSpec.Trigger.DEATH).forGetter(SubEmitterSpec::trigger)
        ).apply(i, SubEmitterSpec::new));
        return RecordCodecBuilder.create(i -> i.group(
                SHAPE.fieldOf("shape").forGetter(EmitterSpec::shape),
                Codec.INT.fieldOf("count").forGetter(EmitterSpec::count),
                Codec.INT.fieldOf("lifetime").forGetter(EmitterSpec::lifetime),
                Codec.FLOAT.fieldOf("speed").forGetter(EmitterSpec::speed),
                CURVE.fieldOf("size").forGetter(EmitterSpec::size),
                CURVE.fieldOf("alpha").forGetter(EmitterSpec::alpha),
                COLOR.fieldOf("color").forGetter(EmitterSpec::color),
                COMPONENT.listOf().optionalFieldOf("modifiers", List.of()).forGetter(EmitterSpec::modifiers),
                EMISSION.optionalFieldOf("emission", EmissionSpec.burst()).forGetter(EmitterSpec::emission),
                RENDER.optionalFieldOf("render", RenderSpec.DEFAULT).forGetter(EmitterSpec::render),
                ROTATION.optionalFieldOf("rotation", RotationSpec.NONE).forGetter(EmitterSpec::rotation),
                COLLISION.optionalFieldOf("collision", CollisionSpec.NONE).forGetter(EmitterSpec::collision),
                subEmitterCodec.optionalFieldOf("sub_emitter").forGetter(s -> Optional.ofNullable(s.subEmitter())),
                TRAIL.optionalFieldOf("trail", TrailSpec.NONE).forGetter(EmitterSpec::trail),
                VELOCITY.optionalFieldOf("velocity", VelocitySpec.RADIAL).forGetter(EmitterSpec::velocity)
        ).apply(i, (shape, count, lifetime, speed, size, alpha, color, modifiers, emission,
                render, rotation, collision, subEmitter, trail, velocity) ->
                new EmitterSpec(shape, count, lifetime, speed, size, alpha, color, modifiers,
                        emission, render, rotation, collision, subEmitter.orElse(null), trail, velocity)));
    });

    // an effect is either an explicit {"emitters":[...]} list, or a bare single emitter object for
    // back compat with effects authored before layering existed. Either tries the list shape first.
    public static final Codec<EffectSpec> EFFECT = Codec.either(
            RecordCodecBuilder.<EffectSpec>create(i -> i.group(
                    EMITTER.listOf().fieldOf("emitters").forGetter(EffectSpec::emitters)
            ).apply(i, EffectSpec::new)),
            EMITTER
    ).xmap(
            either -> either.map(java.util.function.Function.identity(), e -> EffectSpec.of(e)),
            effect -> effect.emitters().size() == 1
                    ? com.mojang.datafixers.util.Either.right(effect.emitters().get(0))
                    : com.mojang.datafixers.util.Either.left(effect));

    private EffectJson() {
    }
}
