package dev.chocoboy.cascade.client;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// block- and item-model debris ride vanilla's model pipelines, both rebuilt in 26.1 (BlockStateModel /
// ChunkSectionLayer for blocks, ItemStackRenderState for items). those paths are not ported yet, so an
// effect that asks for mesh debris still fires but draws no debris. this warns once per kind so a spec
// author sees why, without spamming the log every frame. remove when the paths are restored
final class MeshDebris {

    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");
    private static final Set<String> WARNED = new HashSet<>();

    private MeshDebris() {
    }

    static void warnOnce(String kind) {
        if (WARNED.add(kind)) {
            LOGGER.warn("Cascade {}-model debris is not yet ported to 26.1; the effect will draw no debris", kind);
        }
    }
}
