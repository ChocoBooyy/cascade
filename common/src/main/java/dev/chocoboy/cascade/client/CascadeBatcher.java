package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;

// owns a reusable native arena per render type, so a frame's grouped work becomes one build and one draw
// per type without touching the shared buffer source. the arena recycles itself once its mesh is drawn, so
// steady state never reallocates. 26.1 folds the upload and draw into RenderType.draw, so there is no
// longer a persistent gpu vertex buffer to bind and manage here
final class CascadeBatcher {

    // fits a few thousand textured quads up front; the arena grows on its own if a frame needs more
    private static final int INITIAL_BYTES = 786432;

    private static final Map<RenderType, ByteBufferBuilder> ARENAS = new HashMap<>();

    private CascadeBatcher() {
    }

    static void draw(RenderType type, List<VfxRenderQueue.Submission> submissions,
            VfxRenderQueue.FailureSink failures) {
        ByteBufferBuilder arena = ARENAS.computeIfAbsent(type, t -> new ByteBufferBuilder(INITIAL_BYTES));
        BufferBuilder builder = new BufferBuilder(arena, type.mode(), type.format());
        for (VfxRenderQueue.Submission s : submissions) {
            try {
                s.writer().write(builder);
            } catch (RuntimeException e) {
                failures.failed(s.owner(), e);
            }
        }
        MeshData mesh = builder.build();
        if (mesh == null) {
            return;
        }
        // the geometry is authored camera relative, so distance to origin is distance to the camera
        if (type.sortOnUpload()) {
            mesh.sortQuads(arena, VertexSorting.DISTANCE_TO_ORIGIN);
        }
        // draw uploads the mesh, binds the type's pipeline and state, draws, then closes the mesh, which
        // frees its slice of the arena for the next frame
        type.draw(mesh);
    }
}
