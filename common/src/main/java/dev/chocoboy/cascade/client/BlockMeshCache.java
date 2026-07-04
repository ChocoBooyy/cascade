package dev.chocoboy.cascade.client;

import java.util.List;
import net.minecraft.client.resources.model.geometry.BakedQuad;

// resolves a block id to the flat list of its baked model quads. 26.1 rebuilt the block model pipeline
// around BlockStateModel.collectParts and ChunkSectionLayer, so the old getBlockModel/getQuads path is
// gone; block-model debris is disabled on this port until it is re-implemented. quadsFor returns no quads,
// so the debris path draws nothing. see the porting notes and MeshDebris
final class BlockMeshCache {

    private BlockMeshCache() {
    }

    static List<BakedQuad> quadsFor(String blockId) {
        return List.of();
    }
}
