package dev.chocoboy.cascade;

import com.mojang.serialization.MapCodec;
import dev.chocoboy.cascade.engine.effect.BeamSpec;
import dev.chocoboy.cascade.engine.effect.ComponentSpec;
import dev.chocoboy.cascade.engine.effect.EffectSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.SdfSpec;
import dev.chocoboy.cascade.net.BeamPayload;
import dev.chocoboy.cascade.net.ComponentCodecs;
import dev.chocoboy.cascade.net.DomePayload;
import dev.chocoboy.cascade.net.EffectJson;
import dev.chocoboy.cascade.net.EffectPayload;
import dev.chocoboy.cascade.net.EmitterPayload;
import dev.chocoboy.cascade.net.LightPayload;
import dev.chocoboy.cascade.net.SdfPayload;
import dev.chocoboy.cascade.net.ShakePayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Entry point for playing Cascade effects from server-side code. Every method sends to players within range
 * of the target point; the client simulates and renders. Obtain the fluent builders with {@link #emitter()},
 * {@link #effect()}, {@link #beam()}, {@link #sdf()} and {@link #at(ServerLevel)}, or play a datapack-authored
 * effect by id with {@link #play}.
 */
public final class Vfx {

    private static final double RADIUS = 64.0;

    private static NetworkSender sender;

    private Vfx() {
    }

    /** Installs the loader's send strategy. Called once at loader init, before any effect can be played. */
    public static void sender(NetworkSender networkSender) {
        sender = networkSender;
    }

    /**
     * Registers a custom {@link ComponentSpec} kind on both the wire and the JSON codec, so it resolves on the
     * server, on the client, and in effect JSON. Call once at loader init, before any effect is sent or any
     * datapack loads.
     *
     * @param typeId  stable id for the component, e.g. {@code "mymod:swirl"}
     * @param network stream codec carrying the spec to the client
     * @param json    map codec parsing the spec from effect JSON
     */
    public static void registerComponent(String typeId, StreamCodec<RegistryFriendlyByteBuf, ? extends ComponentSpec> network,
            MapCodec<? extends ComponentSpec> json) {
        ComponentCodecs.register(typeId, network);
        EffectJson.register(typeId, json);
    }

    /** Shakes the camera of nearby players. Magnitude is peak offset in blocks, duration is in ticks. */
    public static void shake(ServerLevel level, Vec3 pos, float magnitude, int duration) {
        sender.sendNear(level, pos.x, pos.y, pos.z, ShakePayload.RADIUS, new ShakePayload(pos, magnitude, duration));
    }

    /** A faked cast light: a glow pooled on the ground under {@code pos}, fading over {@code duration} ticks. */
    public static void light(ServerLevel level, Vec3 pos, int color, float radius, int duration) {
        sender.sendNear(level, pos.x, pos.y, pos.z, RADIUS, new LightPayload(pos, color, radius, duration));
    }

    /** A translucent energy hemisphere centered on {@code pos}, for force fields and zones. */
    public static void dome(ServerLevel level, Vec3 pos, float radius, int color, int duration) {
        sender.sendNear(level, pos.x, pos.y, pos.z, RADIUS, new DomePayload(pos, radius, color, duration));
    }

    /** Starts a timeline of effects on {@code level}. See {@link VfxSequence}. */
    public static VfxSequence at(ServerLevel level) {
        return new VfxSequence(level);
    }

    /** A fresh particle emitter builder, pre-loaded with the default burst. */
    public static VfxEmitter emitter() {
        return new VfxEmitter();
    }

    /** A layered effect builder that plays several emitters at one point. */
    public static VfxEffect effect() {
        return new VfxEffect();
    }

    /** A fresh beam builder, pre-loaded with the default bolt. */
    public static VfxBeam beam() {
        return new VfxBeam();
    }

    /** A raymarched signed-distance volume builder. */
    public static VfxSdf sdf() {
        return new VfxSdf();
    }

    /** Plays the built-in default burst at {@code pos}, the quickest one-liner. */
    public static void burst(ServerLevel level, Vec3 pos) {
        emit(level, pos, EmitterSpec.defaultBurst());
    }

    /** Plays an effect authored in a datapack JSON. Unknown ids are ignored, so a missing pack is not fatal. */
    public static void play(ServerLevel level, Vec3 pos, Identifier id) {
        EffectSpec spec = CascadeEffects.get(id);
        if (spec != null) {
            effect(level, pos, spec);
        }
    }

    /** Plays the default bolt between two points. */
    public static void beam(ServerLevel level, Vec3 from, Vec3 to) {
        beam(level, from, to, BeamSpec.defaultBolt());
    }

    static void effect(ServerLevel level, Vec3 pos, EffectSpec spec) {
        sender.sendNear(level, pos.x, pos.y, pos.z, RADIUS, new EffectPayload(spec, pos, level.getGameTime()));
    }

    static void emit(ServerLevel level, Vec3 pos, EmitterSpec spec) {
        sender.sendNear(level, pos.x, pos.y, pos.z, RADIUS, new EmitterPayload(spec, pos, level.getGameTime()));
    }

    static void beam(ServerLevel level, Vec3 from, Vec3 to, BeamSpec spec) {
        sender.sendNear(level, from.x, from.y, from.z, RADIUS, new BeamPayload(spec, from, to, level.getGameTime()));
    }

    static void sdf(ServerLevel level, Vec3 pos, SdfSpec spec) {
        sender.sendNear(level, pos.x, pos.y, pos.z, RADIUS, new SdfPayload(pos, spec, level.getGameTime()));
    }
}
