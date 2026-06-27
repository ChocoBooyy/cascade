package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.neoforge.net.CascadeEffects;
import dev.chocoboy.cascade.neoforge.net.EffectPayload;
import dev.chocoboy.cascade.neoforge.net.ShakePayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Vfx {

    private static final double RADIUS = 64.0;
    private static final double SHAKE_RADIUS = 32.0;

    private Vfx() {
    }

    public static void shake(ServerLevel level, Vec3 pos, float magnitude, int duration) {
        PacketDistributor.sendToPlayersNear(level, null, pos.x, pos.y, pos.z, SHAKE_RADIUS,
                new ShakePayload(pos, magnitude, duration));
    }

    public static VfxSequence at(ServerLevel level) {
        return new VfxSequence(level);
    }

    public static void burst(ServerLevel level, Vec3 pos) {
        send(level, pos, new EffectPayload(CascadeEffects.BURST, pos, pos, level.getGameTime()));
    }

    public static void beam(ServerLevel level, Vec3 from, Vec3 to) {
        send(level, from, new EffectPayload(CascadeEffects.BEAM, from, to, level.getGameTime()));
    }

    private static void send(ServerLevel level, Vec3 around, EffectPayload payload) {
        PacketDistributor.sendToPlayersNear(level, null, around.x, around.y, around.z, RADIUS, payload);
    }
}
