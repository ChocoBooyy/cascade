package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

final class Billboards {

    private Billboards() {
    }

    // draws a camera-facing textured quad of half-extent size at a camera-relative position. uv is the
    // sprite's cell rect as {u0, v0, u1, v1}, with v0 the top edge.
    static void quad(PoseStack pose, VertexConsumer vc, Quaternionf camRotation,
            float x, float y, float z, float size, float[] uv, int r, int g, int b, int a) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(camRotation);
        Matrix4f m = pose.last().pose();
        vc.addVertex(m, -size, -size, 0f).setUv(uv[0], uv[3]).setColor(r, g, b, a);
        vc.addVertex(m, -size, size, 0f).setUv(uv[0], uv[1]).setColor(r, g, b, a);
        vc.addVertex(m, size, size, 0f).setUv(uv[2], uv[1]).setColor(r, g, b, a);
        vc.addVertex(m, size, -size, 0f).setUv(uv[2], uv[3]).setColor(r, g, b, a);
        pose.popPose();
    }
}
