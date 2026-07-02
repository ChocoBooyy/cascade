package dev.chocoboy.cascade.fabric;

import dev.chocoboy.cascade.neoforge.CascadeEffects;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

// fabric's reload hook wants an identifiable listener, so give the shared effects loader an id
public final class FabricEffectsReload extends CascadeEffects implements IdentifiableResourceReloadListener {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cascade", "effects");

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
