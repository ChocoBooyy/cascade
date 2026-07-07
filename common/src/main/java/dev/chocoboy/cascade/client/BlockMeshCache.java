package dev.chocoboy.cascade.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

// resolves a block id to the flat list of its baked model quads, cached per id. The model and atlas are
// client state, so this stays out of the engine. Unknown ids yield no quads, so a bad id draws nothing.
// 26.1 rebuilt the block model pipeline: a state maps to a BlockStateModel whose collectParts picks the
// random variant, and each part exposes its quads per cull face; a fixed seed keeps the pick stable so
// every debris chunk of one block id shares one mesh, like the pre-26.1 fixed-seed getQuads did
final class BlockMeshCache {

    private static final Map<String, List<BakedQuad>> CACHE = new HashMap<>();

    private BlockMeshCache() {
    }

    static List<BakedQuad> quadsFor(String blockId) {
        return CACHE.computeIfAbsent(blockId, BlockMeshCache::bake);
    }

    private static List<BakedQuad> bake(String blockId) {
        Identifier id = Identifier.tryParse(blockId);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return List.of();
        }
        BlockState state = BuiltInRegistries.BLOCK.getValue(id).defaultBlockState();
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            quads.addAll(part.getQuads(null));
            for (Direction dir : Direction.values()) {
                quads.addAll(part.getQuads(dir));
            }
        }
        return quads;
    }
}
