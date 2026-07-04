package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.client.CascadeRenderTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class CascadeClient {

    private CascadeClient() {
    }

    public static void init(IEventBus modBus) {
        CascadeRenderTypes.install(new NeoRenderTypes());
        NeoForge.EVENT_BUS.register(new VfxRenderBridge());
        NeoForge.EVENT_BUS.register(ShakeController.get());
        // the custom render pipelines register on the mod bus so the gpu device compiles them; the soft
        // particle core shaders are not ported to 26.1 yet, so the soft render types fall back to their
        // hard-edged twins. see the porting notes
        modBus.addListener(VfxRenderTypes::registerPipelines);
    }
}
