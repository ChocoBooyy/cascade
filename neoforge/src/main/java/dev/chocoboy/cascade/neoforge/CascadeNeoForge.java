package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.CascadeCommon;
import dev.chocoboy.cascade.Vfx;
import dev.chocoboy.cascade.neoforge.client.CascadeClient;
import dev.chocoboy.cascade.neoforge.net.CascadeNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CascadeCommon.MOD_ID)
public final class CascadeNeoForge {

    public CascadeNeoForge(IEventBus modBus) {
        CascadeCommon.init();
        Vfx.sender(new NeoNetworkSender());
        modBus.addListener(CascadeNetwork::register);
        NeoForge.EVENT_BUS.register(new CascadeServerBridge());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CascadeClient.init(modBus);
        }
    }
}
