package dev.chocoboy.cascade.fabric;

import dev.chocoboy.cascade.CascadeEffects;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.Identifier;

// fabric's reload hook wants an identifiable listener, so give the shared effects loader an id
public final class FabricEffectsReload extends CascadeEffects implements IdentifiableResourceReloadListener {

    private static final Identifier ID = Identifier.fromNamespaceAndPath("cascade", "effects");

    @Override
    public Identifier getFabricId() {
        return ID;
    }
}
