package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.client.CascadeRenderTypes;
import dev.chocoboy.cascade.client.CorePipelines;
import dev.chocoboy.cascade.client.CoreRenderTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class CascadeClient {

    private CascadeClient() {
    }

    public static void init(IEventBus modBus) {
        CascadeRenderTypes.install(new CoreRenderTypes());
        NeoForge.EVENT_BUS.register(new VfxRenderBridge());
        NeoForge.EVENT_BUS.register(ShakeController.get());
        // pipelines compile lazily on first draw either way; registering them on the mod bus lets the gpu
        // device compile them upfront instead of hitching the first effect
        modBus.addListener((RegisterRenderPipelinesEvent event) ->
                CorePipelines.all().forEach(event::registerPipeline));
    }
}
