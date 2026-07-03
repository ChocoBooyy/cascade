package dev.chocoboy.cascade.fabric;

import dev.chocoboy.cascade.NetworkSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class FabricNetworkSender implements NetworkSender {

    @Override
    public void sendNear(ServerLevel level, double x, double y, double z, double radius, CustomPacketPayload payload) {
        for (ServerPlayer player : PlayerLookup.around(level, new Vec3(x, y, z), radius)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
