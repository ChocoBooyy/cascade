package dev.chocoboy.cascade.neoforge.net;

import dev.chocoboy.cascade.engine.effect.AttractorSpec;
import dev.chocoboy.cascade.engine.effect.ComponentSpec;
import dev.chocoboy.cascade.engine.effect.CurlSpec;
import dev.chocoboy.cascade.engine.effect.DragSpec;
import dev.chocoboy.cascade.engine.effect.GravitySpec;
import dev.chocoboy.cascade.engine.effect.TurbulenceSpec;
import dev.chocoboy.cascade.engine.effect.VortexSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// Per-type network codecs for component specs. The dispatch writes the type id, then the matching
// composite, so new kinds ride the same wire format without touching the fixed EmitterSpec codec.
public final class ComponentCodecs {

    private static final Map<String, StreamCodec<RegistryFriendlyByteBuf, ComponentSpec>> CODECS = new HashMap<>();

    static {
        register("gravity", StreamCodec.composite(
                NetCodecs.VEC3F, GravitySpec::accel,
                GravitySpec::new));
        register("drag", StreamCodec.composite(
                ByteBufCodecs.FLOAT, DragSpec::drag,
                DragSpec::new));
        register("turbulence", StreamCodec.composite(
                ByteBufCodecs.FLOAT, TurbulenceSpec::strength,
                ByteBufCodecs.FLOAT, TurbulenceSpec::frequency,
                TurbulenceSpec::new));
        register("attractor", StreamCodec.composite(
                NetCodecs.VEC3F, AttractorSpec::center,
                ByteBufCodecs.FLOAT, AttractorSpec::strength,
                AttractorSpec::new));
        register("vortex", StreamCodec.composite(
                NetCodecs.VEC3F, VortexSpec::center,
                ByteBufCodecs.FLOAT, VortexSpec::strength,
                VortexSpec::new));
        register("curl", StreamCodec.composite(
                ByteBufCodecs.FLOAT, CurlSpec::strength,
                ByteBufCodecs.FLOAT, CurlSpec::frequency,
                CurlSpec::new));
    }

    @SuppressWarnings("unchecked")
    private static <T extends ComponentSpec> void register(String type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        CODECS.put(type, (StreamCodec<RegistryFriendlyByteBuf, ComponentSpec>) codec);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ComponentSpec> DISPATCH = StreamCodec.of(
            (buf, spec) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, spec.typeId());
                CODECS.get(spec.typeId()).encode(buf, spec);
            },
            buf -> {
                String type = ByteBufCodecs.STRING_UTF8.decode(buf);
                StreamCodec<RegistryFriendlyByteBuf, ComponentSpec> codec = CODECS.get(type);
                if (codec == null) {
                    throw new IllegalArgumentException("unknown component " + type);
                }
                return codec.decode(buf);
            });

    public static final StreamCodec<RegistryFriendlyByteBuf, List<ComponentSpec>> LIST =
            DISPATCH.apply(ByteBufCodecs.collection(ArrayList::new));

    private ComponentCodecs() {
    }
}
