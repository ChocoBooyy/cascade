package dev.chocoboy.cascade.neoforge.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
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
import dev.chocoboy.cascade.engine.effect.FlockSpec;
import dev.chocoboy.cascade.engine.effect.GravitySpec;
import dev.chocoboy.cascade.engine.effect.MeshId;
import dev.chocoboy.cascade.engine.effect.ParticleModifier;
import dev.chocoboy.cascade.engine.effect.RenderSpec;
import dev.chocoboy.cascade.engine.effect.RotationSpec;
import dev.chocoboy.cascade.engine.effect.SdfSpec;
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
import dev.chocoboy.cascade.neoforge.Vfx;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// the round-trip guard for the wire and json spec paths. the gametest server exits before any datapack
// reload, so nothing else machine-checks that what an author writes is what a client decodes; this suite
// runs under fml junit with the game bootstrapped, so real registry byte bufs and json ops are available
class CodecRoundTripTest {

    // a third-party style component defined only here, proving the public registration seam end to end
    private record SpeedCapSpec(float max) implements ComponentSpec {
        @Override
        public ParticleModifier toModifier() {
            return p -> {
                if (p.vel.length() > max) {
                    p.vel = p.vel.normalize().scale(max);
                }
            };
        }

        @Override
        public String typeId() {
            return "speedcap";
        }
    }

    @BeforeAll
    static void registerCustomComponent() {
        Vfx.registerComponent("speedcap",
                StreamCodec.composite(ByteBufCodecs.FLOAT, SpeedCapSpec::max, SpeedCapSpec::new),
                RecordCodecBuilder.<SpeedCapSpec>mapCodec(i -> i.group(
                        Codec.FLOAT.fieldOf("max").forGetter(SpeedCapSpec::max)
                ).apply(i, SpeedCapSpec::new)));
    }

    private static RegistryFriendlyByteBuf buf() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    private static final List<ComponentSpec> ALL_COMPONENTS = List.of(
            new GravitySpec(new Vec3f(0f, -0.02f, 0f)),
            new DragSpec(0.1f),
            new TurbulenceSpec(0.05f, 1.5f),
            new AttractorSpec(new Vec3f(0f, 1f, 0f), 0.01f),
            new VortexSpec(new Vec3f(0.5f, 0f, -0.5f), 0.02f),
            new CurlSpec(0.03f, 2f),
            new FlockSpec(2f, 0.02f, 0.015f, 0.01f, 0.2f),
            new SpeedCapSpec(0.4f));

    // every optional sub-spec populated with a non-default value, so a field the codec forgets to
    // carry cannot hide behind its default
    private static EmitterSpec fullEmitter() {
        EmitterSpec child = new EmitterSpec(ShapeSpec.point(), 6, 10, 0.05f,
                new CurveSpec(0.1f, 0f, Easings.LINEAR),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                ColorSpec.of(0xFFFFFF, 0x808080, Easings.LINEAR),
                List.of(new DragSpec(0.05f)), EmissionSpec.burst(), RenderSpec.DEFAULT,
                RotationSpec.NONE, CollisionSpec.NONE, null, TrailSpec.NONE, VelocitySpec.RADIAL);
        return new EmitterSpec(ShapeSpec.cone(1.5f, 2f), 40, 30, 0.12f,
                new CurveSpec(0.3f, 0.05f, Easings.EASE_OUT_CUBIC),
                new CurveSpec(1f, 0f, Easings.EASE_IN_QUAD),
                new ColorSpec(List.of(0xFFE9A8, 0xFF9C33, 0xB03A9C, 0x3A2C86), Easings.LINEAR),
                ALL_COMPONENTS,
                EmissionSpec.rate(3f, 40),
                new RenderSpec(BlendMode.ALPHA, SpriteId.SMOKE, 1.5f, true, true, MeshId.BLOCK, "minecraft:stone", true),
                RotationSpec.spin(0.3f), CollisionSpec.bouncy(0.4f, 0.2f),
                new SubEmitterSpec(child, SubEmitterSpec.Trigger.COLLISION),
                TrailSpec.of(5), VelocitySpec.directional(new Vec3f(0f, 1f, 0f), 0.4f));
    }

    @Test
    void everyComponentSurvivesTheWire() {
        for (ComponentSpec spec : ALL_COMPONENTS) {
            RegistryFriendlyByteBuf buf = buf();
            ComponentCodecs.DISPATCH.encode(buf, spec);
            assertEquals(spec, ComponentCodecs.DISPATCH.decode(buf), spec.typeId());
        }
    }

    @Test
    void everyComponentSurvivesJson() {
        for (ComponentSpec spec : ALL_COMPONENTS) {
            JsonElement json = EffectJson.COMPONENT.encodeStart(JsonOps.INSTANCE, spec)
                    .getOrThrow(msg -> new AssertionError(spec.typeId() + " encode: " + msg));
            ComponentSpec decoded = EffectJson.COMPONENT.parse(JsonOps.INSTANCE, json)
                    .getOrThrow(msg -> new AssertionError(spec.typeId() + " parse: " + msg));
            assertEquals(spec, decoded, spec.typeId());
        }
    }

    @Test
    void fullyPopulatedEmitterSurvivesTheWire() {
        EmitterSpec spec = fullEmitter();
        RegistryFriendlyByteBuf buf = buf();
        SpecCodecs.EMITTER.encode(buf, spec);
        assertEquals(spec, SpecCodecs.EMITTER.decode(buf));
    }

    @Test
    void fullyPopulatedEmitterSurvivesJson() {
        EmitterSpec spec = fullEmitter();
        JsonElement json = EffectJson.EMITTER.encodeStart(JsonOps.INSTANCE, spec)
                .getOrThrow(msg -> new AssertionError("encode: " + msg));
        EmitterSpec decoded = EffectJson.EMITTER.parse(JsonOps.INSTANCE, json)
                .getOrThrow(msg -> new AssertionError("parse: " + msg));
        assertEquals(spec, decoded);
    }

    @Test
    void multiEmitterEffectSurvivesBothPaths() {
        EffectSpec spec = new EffectSpec(List.of(fullEmitter(), EmitterSpec.defaultBurst()));
        RegistryFriendlyByteBuf buf = buf();
        SpecCodecs.EFFECT.encode(buf, spec);
        assertEquals(spec, SpecCodecs.EFFECT.decode(buf));
        JsonElement json = EffectJson.EFFECT.encodeStart(JsonOps.INSTANCE, spec)
                .getOrThrow(msg -> new AssertionError("encode: " + msg));
        EffectSpec decoded = EffectJson.EFFECT.parse(JsonOps.INSTANCE, json)
                .getOrThrow(msg -> new AssertionError("parse: " + msg));
        assertEquals(spec, decoded);
    }

    @Test
    void sdfSpecSurvivesTheWire() {
        SdfSpec spec = new SdfSpec(List.of(
                new SdfSpec.SdfShape(SdfSpec.Type.SPHERE, new Vec3f(0.5f, 0f, 0f), new Vec3f(0.6f, 0f, 0f)),
                new SdfSpec.SdfShape(SdfSpec.Type.BOX, new Vec3f(-0.4f, 0.2f, 0f), new Vec3f(0.3f, 0.3f, 0.3f)),
                new SdfSpec.SdfShape(SdfSpec.Type.TORUS, Vec3f.ZERO, new Vec3f(0.8f, 0.15f, 0f))),
                0.5f, ColorSpec.of(0x22EEFF, 0xCC33FF, Easings.LINEAR), 0.8f, 300, 0.02f);
        RegistryFriendlyByteBuf buf = buf();
        SpecCodecs.SDF.encode(buf, spec);
        assertEquals(spec, SpecCodecs.SDF.decode(buf));
    }

    @Test
    void shippedDatapackEffectsStillParse() {
        // the phase d lesson: firework.json shipped broken for days because nothing parsed it
        for (String name : List.of("firework", "component")) {
            JsonElement json = readShipped(name);
            EffectSpec decoded = EffectJson.EFFECT.parse(JsonOps.INSTANCE, json)
                    .getOrThrow(msg -> new AssertionError(name + ".json no longer parses: " + msg));
            assertTrue(decoded.emitters().size() >= 1, name);
        }
    }

    private static JsonElement readShipped(String name) {
        String path = "/data/cascade/cascade/effects/" + name + ".json";
        try (InputStream in = CodecRoundTripTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new AssertionError("missing shipped effect " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
