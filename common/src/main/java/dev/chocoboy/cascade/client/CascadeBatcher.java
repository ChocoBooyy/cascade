package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;

// owns a reusable native arena and a gpu vertex buffer per render type, so a frame's grouped work becomes
// one build, one upload, and one draw per type without touching the shared buffer source. buffers persist
// across frames: the arena recycles itself once its mesh is uploaded, so steady state never reallocates
final class CascadeBatcher {

    // fits a few thousand textured quads up front; the arena grows on its own if a frame needs more
    private static final int INITIAL_BYTES = 786432;

    private record Slot(ByteBufferBuilder arena, VertexBuffer vbo) {
    }

    private static final Map<RenderType, Slot> SLOTS = new HashMap<>();

    private CascadeBatcher() {
    }

    static void draw(RenderType type, List<VfxRenderQueue.Submission> submissions,
            VfxRenderQueue.FailureSink failures) {
        Slot slot = SLOTS.computeIfAbsent(type, t ->
                new Slot(new ByteBufferBuilder(INITIAL_BYTES), new VertexBuffer(VertexBuffer.Usage.DYNAMIC)));
        BufferBuilder builder = new BufferBuilder(slot.arena(), type.mode(), type.format());
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
        if (type.sortOnUpload()) {
            mesh.sortQuads(slot.arena(), RenderSystem.getVertexSorting());
        }
        // uploading closes the mesh, which frees its slice of the arena for the next frame
        type.setupRenderState();
        try {
            slot.vbo().bind();
            slot.vbo().upload(mesh);
            slot.vbo().drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(),
                    RenderSystem.getShader());
        } finally {
            VertexBuffer.unbind();
            type.clearRenderState();
        }
    }
}
