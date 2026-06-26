package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chocoboy.cascade.engine.effect.BeamState;
import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.List;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class BeamEffect implements RenderedEffect {

    private static final float HALF_WIDTH = 0.12f;

    private final BeamState sim;

    private BeamEffect(BeamState sim) {
        this.sim = sim;
    }

    public static BeamEffect bolt(Vec3 from, Vec3 to, long seed) {
        Vec3f a = new Vec3f((float) from.x, (float) from.y, (float) from.z);
        Vec3f b = new Vec3f((float) to.x, (float) to.y, (float) to.z);
        return new BeamEffect(new BeamState(a, b, 16, 0.35f, 12, new Random(seed)));
    }

    @Override
    public boolean tick() {
        return sim.tick();
    }

    @Override
    public void render(VfxFrame frame) {
        Vec3 cam = frame.cameraPos();
        Matrix4f m = frame.pose().last().pose();
        VertexConsumer vc = frame.vertexConsumer();
        List<Vec3f> spine = sim.spine();
        for (int i = 0; i < spine.size() - 1; i++) {
            Vec3f p0 = spine.get(i);
            Vec3f p1 = spine.get(i + 1);
            Vec3 a = new Vec3(p0.x() - cam.x, p0.y() - cam.y, p0.z() - cam.z);
            Vec3 b = new Vec3(p1.x() - cam.x, p1.y() - cam.y, p1.z() - cam.z);
            Vec3 dir = b.subtract(a).normalize();
            Vec3 toView = a.scale(-1.0).normalize();
            Vec3 side = dir.cross(toView).normalize().scale(HALF_WIDTH);
            vc.addVertex(m, (float) (a.x - side.x), (float) (a.y - side.y), (float) (a.z - side.z)).setColor(120, 200, 255, 255);
            vc.addVertex(m, (float) (a.x + side.x), (float) (a.y + side.y), (float) (a.z + side.z)).setColor(120, 200, 255, 255);
            vc.addVertex(m, (float) (b.x + side.x), (float) (b.y + side.y), (float) (b.z + side.z)).setColor(120, 200, 255, 255);
            vc.addVertex(m, (float) (b.x - side.x), (float) (b.y - side.y), (float) (b.z - side.z)).setColor(120, 200, 255, 255);
        }
    }
}
