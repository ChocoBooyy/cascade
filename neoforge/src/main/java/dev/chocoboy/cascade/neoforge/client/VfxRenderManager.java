package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;

public final class VfxRenderManager {

    private static final VfxRenderManager INSTANCE = new VfxRenderManager();

    private final List<Quad> quads = new ArrayList<>();

    private VfxRenderManager() {
    }

    public static VfxRenderManager get() {
        return INSTANCE;
    }

    public void addQuad(Vec3 pos, int rgb, float size, int lifetime) {
        quads.add(new Quad(pos, rgb, size, lifetime));
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        quads.removeIf(q -> ++q.age >= q.lifetime);
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || quads.isEmpty()) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        Quaternionf rotation = camera.rotation();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(VfxRenderTypes.ADDITIVE);
        for (Quad q : quads) {
            int r = (q.rgb >> 16) & 0xFF;
            int g = (q.rgb >> 8) & 0xFF;
            int b = q.rgb & 0xFF;
            Billboards.quad(pose, vc, rotation,
                    (float) (q.pos.x - cam.x), (float) (q.pos.y - cam.y), (float) (q.pos.z - cam.z),
                    q.size, r, g, b, 255);
        }
        buffers.endBatch(VfxRenderTypes.ADDITIVE);
    }

    private static final class Quad {
        private final Vec3 pos;
        private final int rgb;
        private final float size;
        private final int lifetime;
        private int age;

        private Quad(Vec3 pos, int rgb, float size, int lifetime) {
            this.pos = pos;
            this.rgb = rgb;
            this.size = size;
            this.lifetime = lifetime;
        }
    }
}
