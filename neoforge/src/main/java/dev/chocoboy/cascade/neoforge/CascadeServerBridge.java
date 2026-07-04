package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.CascadeEffects;
import dev.chocoboy.cascade.VfxSequencer;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// drives the shared sequencer and registers the effect reload listener off neoforge's game bus
public final class CascadeServerBridge {

    // 26.1 keys reload listeners by id so their order can be sorted against other packs' listeners
    private static final Identifier EFFECTS = Identifier.fromNamespaceAndPath("cascade", "effects");

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        VfxSequencer.get().tick();
    }

    @SubscribeEvent
    public void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(EFFECTS, new CascadeEffects());
    }
}
