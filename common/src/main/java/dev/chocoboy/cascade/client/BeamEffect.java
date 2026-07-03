package dev.chocoboy.cascade.client;

import dev.chocoboy.cascade.engine.effect.BeamState;
import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class BeamEffect implements RenderedEffect {

    private final BeamState sim;
    private final int color;
    private final float halfWidth;

    public BeamEffect(BeamState sim, int color, float width) {
        this.sim = sim;
        this.color = color;
        this.halfWidth = width;
    }

    @Override
    public boolean tick() {
        return sim.tick();
    }

    @Override
    public Vec3 position() {
        List<Vec3f> spine = sim.spine();
        if (spine.isEmpty()) {
            return Vec3.ZERO;
        }
        Vec3f p = spine.get(0);
        return new Vec3(p.x(), p.y(), p.z());
    }

    @Override
    public int drawCount() {
        return sim.spine().size();
    }

    @Override
    public void render(VfxFrame frame) {
        Vec3 cam = frame.cameraPos();
        Matrix4f m = frame.pose().last().pose();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        frame.queue().submit(CascadeRenderTypes.additive(), vc -> {
            List<Vec3f> spine = sim.spine();
            for (int i = 0; i < spine.size() - 1; i++) {
                Vec3f p0 = spine.get(i);
                Vec3f p1 = spine.get(i + 1);
                Vec3 a = new Vec3(p0.x() - cam.x, p0.y() - cam.y, p0.z() - cam.z);
                Vec3 c = new Vec3(p1.x() - cam.x, p1.y() - cam.y, p1.z() - cam.z);
                Vec3 dir = c.subtract(a).normalize();
                Vec3 toView = a.scale(-1.0).normalize();
                Vec3 side = dir.cross(toView).normalize().scale(halfWidth);
                vc.addVertex(m, (float) (a.x - side.x), (float) (a.y - side.y), (float) (a.z - side.z)).setColor(r, g, b, 255);
                vc.addVertex(m, (float) (a.x + side.x), (float) (a.y + side.y), (float) (a.z + side.z)).setColor(r, g, b, 255);
                vc.addVertex(m, (float) (c.x + side.x), (float) (c.y + side.y), (float) (c.z + side.z)).setColor(r, g, b, 255);
                vc.addVertex(m, (float) (c.x - side.x), (float) (c.y - side.y), (float) (c.z - side.z)).setColor(r, g, b, 255);
            }
        });
    }
}
