package dev.chocoboy.cascade;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;

// the one server-to-client send Cascade needs, factored out so the shared Vfx api does not depend on a
// loader's packet distributor. each loader installs its own sender at init
public interface NetworkSender {

    void sendNear(ServerLevel level, double x, double y, double z, double radius, CustomPacketPayload payload);
}
