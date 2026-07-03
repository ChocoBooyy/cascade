package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.CascadeEffects;
import dev.chocoboy.cascade.VfxSequencer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// drives the shared sequencer and registers the effect reload listener off neoforge's game bus
public final class CascadeServerBridge {

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        VfxSequencer.get().tick();
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new CascadeEffects());
    }
}
