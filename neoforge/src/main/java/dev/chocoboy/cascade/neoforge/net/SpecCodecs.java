package dev.chocoboy.cascade.neoforge.net;

import dev.chocoboy.cascade.engine.effect.BeamSpec;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.EmissionSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.ModifierSpec;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
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

    public static final StreamCodec<RegistryFriendlyByteBuf, EmitterSpec> EMITTER = StreamCodec.of(
            (buf, s) -> {
                SHAPE.encode(buf, s.shape());
                buf.writeVarInt(s.count());
                buf.writeVarInt(s.lifetime());
                buf.writeFloat(s.speed());
                CURVE.encode(buf, s.size());
                CURVE.encode(buf, s.alpha());
                buf.writeInt(s.colorStart());
                buf.writeInt(s.colorEnd());
                EASING.encode(buf, s.colorEase());
                MODIFIERS.encode(buf, s.modifiers());
                EMISSION.encode(buf, s.emission());
                BLEND.encode(buf, s.blend());
                SPRITE.encode(buf, s.sprite());
            },
            buf -> new EmitterSpec(
                    SHAPE.decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readFloat(),
                    CURVE.decode(buf), CURVE.decode(buf),
                    buf.readInt(), buf.readInt(), EASING.decode(buf), MODIFIERS.decode(buf), EMISSION.decode(buf),
                    BLEND.decode(buf), SPRITE.decode(buf)));

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
