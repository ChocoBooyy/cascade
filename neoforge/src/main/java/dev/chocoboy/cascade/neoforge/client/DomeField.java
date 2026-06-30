package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

// an actual surface primitive, not a cloud of particles: a translucent hemisphere mesh for force fields and
// zones. Brightness rides a fresnel term (bright at grazing angles, faint head on) so it reads as a shell of
// energy rather than a solid cap. Additive blended, so overlapping geometry glows.
public final class DomeField implements RenderedEffect {

    private static final int RINGS = 16;
    private static final int SEGMENTS = 32;
    private static final float HALF_PI = (float) (Math.PI / 2.0);
    private static final float TAU = (float) (Math.PI * 2.0);

    private final Vec3 center;
    private final float radius;
    private final int color;
    private final int duration;
    private int age;

    public DomeField(Vec3 center, float radius, int color, int duration) {
        this.center = center;
        this.radius = radius;
        this.color = color;
        this.duration = duration;
    }

    @Override
    public boolean tick() {
        return ++age >= duration;
    }

    @Override
    public Vec3 position() {
        return center;
    }

    @Override
    public int drawCount() {
        return RINGS * SEGMENTS;
    }

    @Override
    public void render(VfxFrame frame) {
        float fade = fade();
        if (fade <= 0f) {
            return;
        }
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        Vec3 cam = frame.cameraPos();
        Matrix4f m = frame.pose().last().pose();
        VertexConsumer vc = frame.buffers().getBuffer(VfxRenderTypes.ADDITIVE);
        for (int i = 0; i < RINGS; i++) {
            float lat0 = HALF_PI * i / RINGS;
            float lat1 = HALF_PI * (i + 1) / RINGS;
            for (int j = 0; j < SEGMENTS; j++) {
                float lon0 = TAU * j / SEGMENTS;
                float lon1 = TAU * (j + 1) / SEGMENTS;
                vertex(vc, m, cam, lat0, lon0, r, g, b, fade);
                vertex(vc, m, cam, lat1, lon0, r, g, b, fade);
                vertex(vc, m, cam, lat1, lon1, r, g, b, fade);
                vertex(vc, m, cam, lat0, lon1, r, g, b, fade);
            }
        }
    }

    private void vertex(VertexConsumer vc, Matrix4f m, Vec3 cam, float lat, float lon,
            int r, int g, int b, float fade) {
        float ringR = (float) Math.sin(lat);
        float ox = (float) Math.cos(lon) * ringR;
        float oz = (float) Math.sin(lon) * ringR;
        float oy = (float) Math.cos(lat);
        // the offset is already unit length, so it doubles as the surface normal
        double wx = center.x + ox * radius;
        double wy = center.y + oy * radius;
        double wz = center.z + oz * radius;
        double vx = cam.x - wx;
        double vy = cam.y - wy;
        double vz = cam.z - wz;
        double vlen = Math.sqrt(vx * vx + vy * vy + vz * vz);
        double facing = vlen < 1e-6 ? 1.0 : Math.abs((ox * vx + oy * vy + oz * vz) / vlen);
        float fresnel = (float) (1.0 - facing);
        int a = (int) (fade * (0.15f + 0.85f * fresnel) * 255f);
        vc.addVertex(m, (float) (wx - cam.x), (float) (wy - cam.y), (float) (wz - cam.z)).setColor(r, g, b, a);
    }

    // ramp in, hold with a gentle pulse, ramp out
    private float fade() {
        float t = (float) age / duration;
        float env = t < 0.15f ? t / 0.15f : (t > 0.7f ? Math.max(0f, (1f - t) / 0.3f) : 1f);
        float pulse = 0.78f + 0.22f * (float) Math.sin(age * 0.4f);
        return env * pulse * 0.7f;
    }
}
