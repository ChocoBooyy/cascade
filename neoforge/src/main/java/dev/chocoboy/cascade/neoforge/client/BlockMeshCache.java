package dev.chocoboy.cascade.neoforge.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

// resolves a block id to the flat list of its baked model quads, cached per id. The model and atlas are
// client state, so this stays out of the engine. Unknown ids yield no quads, so a bad id draws nothing
final class BlockMeshCache {

    private static final Map<String, List<BakedQuad>> CACHE = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create(42L);

    private BlockMeshCache() {
    }

    static List<BakedQuad> quadsFor(String blockId) {
        return CACHE.computeIfAbsent(blockId, BlockMeshCache::bake);
    }

    private static List<BakedQuad> bake(String blockId) {
        ResourceLocation id = ResourceLocation.tryParse(blockId);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return List.of();
        }
        BlockState state = BuiltInRegistries.BLOCK.get(id).defaultBlockState();
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        List<BakedQuad> quads = new ArrayList<>();
        RANDOM.setSeed(42L);
        quads.addAll(model.getQuads(state, null, RANDOM, ModelData.EMPTY, null));
        for (Direction dir : Direction.values()) {
            RANDOM.setSeed(42L);
            quads.addAll(model.getQuads(state, dir, RANDOM, ModelData.EMPTY, null));
        }
        return quads;
    }
}
