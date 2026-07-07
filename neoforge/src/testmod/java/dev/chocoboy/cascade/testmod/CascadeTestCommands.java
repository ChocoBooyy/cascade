package dev.chocoboy.cascade.testmod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// neoforge registration glue for the /cascade command and its world-load reset; every effect lives in the
// shared CascadeDemos, so the fabric testmod builds the same command
public final class CascadeTestCommands {

    private CascadeTestCommands() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        CascadeDemos.resetTour();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var root = CascadeDemos.serverTree();
        CascadeDemos.addToggles(root, () -> FMLEnvironment.dist.isClient());
        event.getDispatcher().register(root);
    }
}
