package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.NetworkSender;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoNetworkSender implements NetworkSender {

    @Override
    public void sendNear(ServerLevel level, double x, double y, double z, double radius, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, radius, payload);
    }
}
