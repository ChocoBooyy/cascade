package dev.chocoboy.cascade.neoforge.net;

import dev.chocoboy.cascade.engine.effect.BeamSpec;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.CollisionSpec;
import dev.chocoboy.cascade.engine.effect.EmissionSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.ModifierSpec;
import dev.chocoboy.cascade.engine.effect.RenderSpec;
import dev.chocoboy.cascade.engine.effect.RotationSpec;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.effect.SubEmitterSpec;
import dev.chocoboy.cascade.engine.effect.TrailSpec;
import dev.chocoboy.cascade.engine.effect.VelocitySpec;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.ColorSpec;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class SpecCodecs {

    private static final StreamCodec<ByteBuf, Easings> EASING =
            ByteBufCodecs.idMapper(i -> Easings.values()[i], Enum::ordinal);

    private static final StreamCodec<ByteBuf, ShapeSpec.Kind> SHAPE_KIND =
            ByteBufCodecs.idMapper(i -> ShapeSpec.Kind.values()[i], Enum::ordinal);

    private static final StreamCodec<RegistryFriendlyByteBuf, ShapeSpec> SHAPE = StreamCodec.composite(
            SHAPE_KIND, ShapeSpec::kind,
            ByteBufCodecs.FLOAT, ShapeSpec::radius,
            ByteBufCodecs.FLOAT, ShapeSpec::height,
            NetCodecs.VEC3F, ShapeSpec::a,
            NetCodecs.VEC3F, ShapeSpec::b,
            ShapeSpec::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, CurveSpec> CURVE = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CurveSpec::start,
            ByteBufCodecs.FLOAT, CurveSpec::end,
            EASING, CurveSpec::ease,
            CurveSpec::new);

    private static final StreamCodec<ByteBuf, List<Integer>> COLOR_STOPS =
            ByteBufCodecs.INT.apply(ByteBufCodecs.collection(ArrayList::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, ColorSpec> COLOR = StreamCodec.composite(
            COLOR_STOPS, ColorSpec::stops,
            EASING, ColorSpec::ease,
            ColorSpec::new);

    private static final StreamCodec<ByteBuf, ModifierSpec.Kind> MODIFIER_KIND =
            ByteBufCodecs.idMapper(i -> ModifierSpec.Kind.values()[i], Enum::ordinal);

    private static final StreamCodec<RegistryFriendlyByteBuf, ModifierSpec> MODIFIER = StreamCodec.composite(
            MODIFIER_KIND, ModifierSpec::kind,
            NetCodecs.VEC3F, ModifierSpec::vec,
            ByteBufCodecs.FLOAT, ModifierSpec::a,
            ByteBufCodecs.FLOAT, ModifierSpec::b,
            ModifierSpec::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, List<ModifierSpec>> MODIFIERS =
            MODIFIER.apply(ByteBufCodecs.collection(ArrayList::new));

    private static final StreamCodec<ByteBuf, EmissionSpec.Mode> EMISSION_MODE =
            ByteBufCodecs.idMapper(i -> EmissionSpec.Mode.values()[i], Enum::ordinal);

    private static final StreamCodec<RegistryFriendlyByteBuf, EmissionSpec> EMISSION = StreamCodec.composite(
            EMISSION_MODE, EmissionSpec::mode,
            ByteBufCodecs.FLOAT, EmissionSpec::rate,
            ByteBufCodecs.VAR_INT, EmissionSpec::duration,
            EmissionSpec::new);

    private static final StreamCodec<ByteBuf, BlendMode> BLEND =
            ByteBufCodecs.idMapper(i -> BlendMode.values()[i], Enum::ordinal);

    private static final StreamCodec<ByteBuf, SpriteId> SPRITE =
            ByteBufCodecs.idMapper(i -> SpriteId.values()[i], Enum::ordinal);

    private static final StreamCodec<RegistryFriendlyByteBuf, RenderSpec> RENDER = StreamCodec.composite(
            BLEND, RenderSpec::blend,
            SPRITE, RenderSpec::sprite,
            ByteBufCodecs.FLOAT, RenderSpec::stretch,
            ByteBufCodecs.BOOL, RenderSpec::animate,
            ByteBufCodecs.BOOL, RenderSpec::lit,
            RenderSpec::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, RotationSpec> ROTATION = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RotationSpec::angleRange,
            ByteBufCodecs.FLOAT, RotationSpec::spinRange,
            RotationSpec::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, CollisionSpec> COLLISION = StreamCodec.composite(
            ByteBufCodecs.BOOL, CollisionSpec::enabled,
            ByteBufCodecs.FLOAT, CollisionSpec::bounce,
            ByteBufCodecs.FLOAT, CollisionSpec::friction,
            CollisionSpec::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, TrailSpec> TRAIL = StreamCodec.composite(
            ByteBufCodecs.BOOL, TrailSpec::enabled,
            ByteBufCodecs.VAR_INT, TrailSpec::length,
            TrailSpec::new);

    private static final StreamCodec<ByteBuf, VelocitySpec.Mode> VELOCITY_MODE =
            ByteBufCodecs.idMapper(i -> VelocitySpec.Mode.values()[i], Enum::ordinal);

    private static final StreamCodec<RegistryFriendlyByteBuf, VelocitySpec> VELOCITY = StreamCodec.composite(
            VELOCITY_MODE, VelocitySpec::mode,
            NetCodecs.VEC3F, VelocitySpec::direction,
            ByteBufCodecs.FLOAT, VelocitySpec::spread,
            VelocitySpec::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, EmitterSpec> EMITTER = StreamCodec.of(
            (buf, s) -> {
                SHAPE.encode(buf, s.shape());
                buf.writeVarInt(s.count());
                buf.writeVarInt(s.lifetime());
                buf.writeFloat(s.speed());
                CURVE.encode(buf, s.size());
                CURVE.encode(buf, s.alpha());
                COLOR.encode(buf, s.color());
                MODIFIERS.encode(buf, s.modifiers());
                EMISSION.encode(buf, s.emission());
                RENDER.encode(buf, s.render());
                ROTATION.encode(buf, s.rotation());
                COLLISION.encode(buf, s.collision());
                SubEmitterSpec sub = s.subEmitter();
                buf.writeBoolean(sub != null);
                if (sub != null) {
                    encodeNested(buf, sub.child());
                }
                TRAIL.encode(buf, s.trail());
                VELOCITY.encode(buf, s.velocity());
            },
            buf -> {
                ShapeSpec shape = SHAPE.decode(buf);
                int count = buf.readVarInt();
                int lifetime = buf.readVarInt();
                float speed = buf.readFloat();
                CurveSpec size = CURVE.decode(buf);
                CurveSpec alpha = CURVE.decode(buf);
                ColorSpec color = COLOR.decode(buf);
                List<ModifierSpec> modifiers = MODIFIERS.decode(buf);
                EmissionSpec emission = EMISSION.decode(buf);
                RenderSpec render = RENDER.decode(buf);
                RotationSpec rotation = ROTATION.decode(buf);
                CollisionSpec collision = COLLISION.decode(buf);
                SubEmitterSpec sub = buf.readBoolean() ? new SubEmitterSpec(decodeNested(buf)) : null;
                TrailSpec trail = TRAIL.decode(buf);
                VelocitySpec velocity = VELOCITY.decode(buf);
                return new EmitterSpec(shape, count, lifetime, speed, size, alpha, color,
                        modifiers, emission, render, rotation, collision, sub, trail, velocity);
            });

    // a sub-emitter nests a child EmitterSpec. These hops keep that recursion out of the EMITTER field
    // initializer itself, which Java forbids from naming the field it is building. Recursion terminates
    // because real specs nest finitely.
    private static void encodeNested(RegistryFriendlyByteBuf buf, EmitterSpec child) {
        EMITTER.encode(buf, child);
    }

    private static EmitterSpec decodeNested(RegistryFriendlyByteBuf buf) {
        return EMITTER.decode(buf);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, BeamSpec> BEAM = StreamCodec.composite(
            ByteBufCodecs.INT, BeamSpec::color,
            ByteBufCodecs.FLOAT, BeamSpec::width,
            ByteBufCodecs.FLOAT, BeamSpec::arc,
            ByteBufCodecs.VAR_INT, BeamSpec::duration,
            ByteBufCodecs.VAR_INT, BeamSpec::segments,
            BeamSpec::new);

    private SpecCodecs() {
    }
}
