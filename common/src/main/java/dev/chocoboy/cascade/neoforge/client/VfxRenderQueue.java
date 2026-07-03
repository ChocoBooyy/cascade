package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

// collects a frame's draw work grouped by render type, then plays it back one type at a time. the shared
// buffer source flushes every time the requested render type changes, so effects with mixed types
// interleaved would cost a draw per switch; grouping makes it one flush per type per frame
public final class VfxRenderQueue {

    // playback tiers: opaque geometry first, alpha blends over it, additive glow on top. within a tier,
    // types and writers keep submission order, which the manager feeds nearest first
    private static final int OPAQUE = 0;
    private static final int BLENDED = 1;
    private static final int GLOW = 2;

    public interface Writer {
        void write(VertexConsumer vc);
    }

    // for draws that pick their render types internally (item models); these skip type grouping and run
    // with the opaque tier
    public interface DirectWriter {
        void write(MultiBufferSource buffers);
    }

    // reports a writer that threw during playback, so the owning effect can be removed without the
    // whole pass going down
    interface FailureSink {
        void failed(Object owner, RuntimeException e);
    }

    private record Submission(Object owner, Writer writer) {
    }

    private record DirectSubmission(Object owner, DirectWriter writer) {
    }

    private final Map<RenderType, List<Submission>> groups = new LinkedHashMap<>();
    private final List<DirectSubmission> direct = new ArrayList<>();
    private Object owner;

    // tags following submissions with the effect making them, so a playback failure traces back to it
    void owner(Object owner) {
        this.owner = owner;
    }

    public void submit(RenderType type, Writer writer) {
        groups.computeIfAbsent(type, t -> new ArrayList<>()).add(new Submission(owner, writer));
    }

    public void submitDirect(DirectWriter writer) {
        direct.add(new DirectSubmission(owner, writer));
    }

    void play(MultiBufferSource.BufferSource buffers, FailureSink failures) {
        for (DirectSubmission s : direct) {
            try {
                s.writer().write(buffers);
            } catch (RuntimeException e) {
                failures.failed(s.owner(), e);
            }
        }
        // the sort is stable, so types in the same tier keep first-submission order
        List<RenderType> order = new ArrayList<>(groups.keySet());
        order.sort(Comparator.comparingInt(VfxRenderQueue::tier));
        for (RenderType type : order) {
            VertexConsumer vc = buffers.getBuffer(type);
            for (Submission s : groups.get(type)) {
                try {
                    s.writer().write(vc);
                } catch (RuntimeException e) {
                    failures.failed(s.owner(), e);
                }
            }
        }
    }

    private static int tier(RenderType type) {
        if (type == CascadeRenderTypes.solid() || type == CascadeRenderTypes.solidLit()
                || type == RenderType.cutout()) {
            return OPAQUE;
        }
        if (type == CascadeRenderTypes.additive() || type == CascadeRenderTypes.texturedAdditive()
                || type == CascadeRenderTypes.texturedAdditiveLit()) {
            return GLOW;
        }
        return BLENDED;
    }
}
