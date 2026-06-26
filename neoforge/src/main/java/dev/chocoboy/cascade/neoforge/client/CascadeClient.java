package dev.chocoboy.cascade.neoforge.client;

import net.neoforged.neoforge.common.NeoForge;

public final class CascadeClient {

    private CascadeClient() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.register(VfxRenderManager.get());
    }
}
