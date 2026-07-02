package dev.chocoboy.cascade.neoforge;

import com.mojang.serialization.MapCodec;
import dev.chocoboy.cascade.engine.effect.BeamSpec;
import dev.chocoboy.cascade.engine.effect.ComponentSpec;
import dev.chocoboy.cascade.engine.effect.EffectSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.neoforge.net.BeamPayload;
import dev.chocoboy.cascade.neoforge.net.ComponentCodecs;
import dev.chocoboy.cascade.neoforge.net.DomePayload;
import dev.chocoboy.cascade.neoforge.net.EffectJson;
import dev.chocoboy.cascade.neoforge.net.EffectPayload;
import dev.chocoboy.cascade.neoforge.net.EmitterPayload;
import dev.chocoboy.cascade.neoforge.net.LightPayload;
import dev.chocoboy.cascade.neoforge.net.ShakePayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class Vfx {

    private static final double RADIUS = 64.0;

    private static NetworkSender sender;

    private Vfx() {
    }

    // the loader installs its send strategy at init, before any effect can be played
    public static void sender(NetworkSender networkSender) {
        sender = networkSender;
    }

    // register a custom component on both client and server at init, so it resolves on both ends of the wire
    public static void registerComponent(String typeId, StreamCodec<RegistryFriendlyByteBuf, ? extends ComponentSpec> network,
            MapCodec<? extends ComponentSpec> json) {
        ComponentCodecs.register(typeId, network);
        EffectJson.register(typeId, json);
    }

    public static void shake(ServerLevel level, Vec3 pos, float magnitude, int duration) {
        sender.sendNear(level, pos.x, pos.y, pos.z, ShakePayload.RADIUS, new ShakePayload(pos, magnitude, duration));
    }

    // a faked cast light: a glow pooled on the ground under pos, fading over duration ticks
    public static void light(ServerLevel level, Vec3 pos, int color, float radius, int duration) {
        sender.sendNear(level, pos.x, pos.y, pos.z, RADIUS, new LightPayload(pos, color, radius, duration));
    }

    // a translucent energy hemisphere centered on pos, for force fields and zones
    public static void dome(ServerLevel level, Vec3 pos, float radius, int color, int duration) {
        sender.sendNear(level, pos.x, pos.y, pos.z, RADIUS, new DomePayload(pos, radius, color, duration));
    }

    public static VfxSequence at(ServerLevel level) {
        return new VfxSequence(level);
    }

    public static VfxEmitter emitter() {
        return new VfxEmitter();
    }

    public static VfxEffect effect() {
        return new VfxEffect();
    }

    public static VfxBeam beam() {
        return new VfxBeam();
    }

    public static void burst(ServerLevel level, Vec3 pos) {
        emit(level, pos, EmitterSpec.defaultBurst());
    }

    // play an effect authored in a datapack json. Unknown ids are ignored so a missing pack is not fatal
    public static void play(ServerLevel level, Vec3 pos, ResourceLocation id) {
        EffectSpec spec = CascadeEffects.get(id);
        if (spec != null) {
            effect(level, pos, spec);
        }
    }

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
}
