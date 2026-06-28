package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

final class Billboards {

    private Billboards() {
    }

    // draws a camera-facing textured quad of the given half-width and half-height at a camera-relative
    // position, rolled by rotation radians about the view axis. A square quad is a round sprite; a wider
    // hx than hy is a velocity-aligned streak. uv is the cell rect {u0, v0, u1, v1}, v0 the top edge.
    static void quad(PoseStack pose, VertexConsumer vc, Quaternionf camRotation,
            float x, float y, float z, float hx, float hy, float rotation, float[] uv, int r, int g, int b, int a) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(camRotation);
        Matrix4f m = pose.last().pose();
        // rotate the corners in plane so we do not allocate a quaternion per particle
        float cs = (float) Math.cos(rotation);
        float sn = (float) Math.sin(rotation);
        vc.addVertex(m, -hx * cs + hy * sn, -hx * sn - hy * cs, 0f).setUv(uv[0], uv[3]).setColor(r, g, b, a);
        vc.addVertex(m, -hx * cs - hy * sn, -hx * sn + hy * cs, 0f).setUv(uv[0], uv[1]).setColor(r, g, b, a);
        vc.addVertex(m, hx * cs - hy * sn, hx * sn + hy * cs, 0f).setUv(uv[2], uv[1]).setColor(r, g, b, a);
        vc.addVertex(m, hx * cs + hy * sn, hx * sn - hy * cs, 0f).setUv(uv[2], uv[3]).setColor(r, g, b, a);
        pose.popPose();
    }
}
