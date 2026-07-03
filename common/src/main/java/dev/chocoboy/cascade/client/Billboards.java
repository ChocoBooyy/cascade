package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chocoboy.cascade.engine.effect.Particle;
import dev.chocoboy.cascade.engine.math.Vec3f;
import net.minecraft.world.phys.Vec3;
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

    // same quad on the PARTICLE vertex format, carrying a packed lightmap coord so the particle shader
    // tints it by world light
    static void litQuad(PoseStack pose, VertexConsumer vc, Quaternionf camRotation,
            float x, float y, float z, float hx, float hy, float rotation, float[] uv, int r, int g, int b, int a, int light) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(camRotation);
        Matrix4f m = pose.last().pose();
        float cs = (float) Math.cos(rotation);
        float sn = (float) Math.sin(rotation);
        vc.addVertex(m, -hx * cs + hy * sn, -hx * sn - hy * cs, 0f).setUv(uv[0], uv[3]).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, -hx * cs - hy * sn, -hx * sn + hy * cs, 0f).setUv(uv[0], uv[1]).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, hx * cs - hy * sn, hx * sn + hy * cs, 0f).setUv(uv[2], uv[1]).setColor(r, g, b, a).setLight(light);
        vc.addVertex(m, hx * cs + hy * sn, hx * sn - hy * cs, 0f).setUv(uv[2], uv[3]).setColor(r, g, b, a).setLight(light);
        pose.popPose();
    }

    // a camera-facing ribbon through a particle's position history, tapering from the moving head back
    // to nothing at the oldest point so the trail fades out. The sprite cell's mid column is sampled
    // across the strip width so the edges stay soft. Positions are camera relative, like the quads above.
    static void ribbon(Matrix4f m, VertexConsumer vc, Particle p, Vec3 origin, Vec3 cam,
            float[] uv, int r, int g, int b, float headAlpha, float size) {
        float uMid = (uv[0] + uv[2]) * 0.5f;
        float v0 = uv[1];
        float v1 = uv[3];
        int last = p.trailCount - 1;
        for (int i = 0; i < last; i++) {
            Vec3f lp0 = p.trailPoint(i);
            Vec3f lp1 = p.trailPoint(i + 1);
            Vec3 a = new Vec3(origin.x + lp0.x() - cam.x, origin.y + lp0.y() - cam.y, origin.z + lp0.z() - cam.z);
            Vec3 c = new Vec3(origin.x + lp1.x() - cam.x, origin.y + lp1.y() - cam.y, origin.z + lp1.z() - cam.z);
            Vec3 seg = c.subtract(a);
            double len = seg.length();
            if (len < 1e-6) {
                continue;
            }
            Vec3 dir = seg.scale(1.0 / len);
            Vec3 side = dir.cross(a.scale(-1.0).normalize()).normalize();
            float t0 = (float) i / last;
            float t1 = (float) (i + 1) / last;
            Vec3 s0 = side.scale(size * t0);
            Vec3 s1 = side.scale(size * t1);
            int a0 = (int) (headAlpha * t0 * 255f);
            int a1 = (int) (headAlpha * t1 * 255f);
            vc.addVertex(m, (float) (a.x - s0.x), (float) (a.y - s0.y), (float) (a.z - s0.z)).setUv(uMid, v0).setColor(r, g, b, a0);
            vc.addVertex(m, (float) (a.x + s0.x), (float) (a.y + s0.y), (float) (a.z + s0.z)).setUv(uMid, v1).setColor(r, g, b, a0);
            vc.addVertex(m, (float) (c.x + s1.x), (float) (c.y + s1.y), (float) (c.z + s1.z)).setUv(uMid, v1).setColor(r, g, b, a1);
            vc.addVertex(m, (float) (c.x - s1.x), (float) (c.y - s1.y), (float) (c.z - s1.z)).setUv(uMid, v0).setColor(r, g, b, a1);
        }
    }
}
