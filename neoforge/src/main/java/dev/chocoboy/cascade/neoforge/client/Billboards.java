package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

final class Billboards {

    private Billboards() {
    }

    // draws a camera-facing quad of half-extent size at a camera-relative position
    static void quad(PoseStack pose, VertexConsumer vc, Quaternionf camRotation,
            float x, float y, float z, float size, int r, int g, int b, int a) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(camRotation);
        Matrix4f m = pose.last().pose();
        vc.addVertex(m, -size, -size, 0f).setColor(r, g, b, a);
        vc.addVertex(m, -size, size, 0f).setColor(r, g, b, a);
        vc.addVertex(m, size, size, 0f).setColor(r, g, b, a);
        vc.addVertex(m, size, -size, 0f).setColor(r, g, b, a);
        pose.popPose();
    }
}
