package dev.chocoboy.cascade.neoforge.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// resolves an item id to a cached one-count ItemStack for its model. Unknown ids yield an empty stack, so a
// bad id draws nothing. The model and atlas are client state, so this stays out of the engine
final class ItemMeshCache {

    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    private ItemMeshCache() {
    }

    static ItemStack stackFor(String itemId) {
        return CACHE.computeIfAbsent(itemId, ItemMeshCache::resolve);
    }

    private static ItemStack resolve(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }
}
