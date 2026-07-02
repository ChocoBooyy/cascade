package dev.chocoboy.cascade.neoforge.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class CascadeClient {

    private CascadeClient() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(VfxRenderManager.get());
        NeoForge.EVENT_BUS.register(ShakeController.get());
        // core shaders register on the mod bus, not the game bus, so the soft particle shader loads with the rest
        modBus.addListener(CascadeShaders::onRegisterShaders);
    }
}
