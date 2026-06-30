package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

// a faked cast light: an additive glow laid flat on the ground under a bright effect, fading in then out.
// It deliberately does not touch the world light engine, which would mean per-effect chunk relighting and
// the flicker and cost that come with it. This just reads as a pool of light on the floor.
public final class LightSplat implements RenderedEffect {

    private static final int MAX_DROP = 8;

    private final Vec3 ground;
    private final int color;
    private final float radius;
    private final int duration;
    private int age;

    public LightSplat(Vec3 pos, int color, float radius, int duration) {
        this.color = color;
        this.radius = radius;
        this.duration = duration;
        this.ground = groundUnder(pos);
    }

    private static Vec3 groundUnder(Vec3 pos) {
        Player player = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;
        if (player == null || level == null) {
            return pos;
        }
        Vec3 end = pos.subtract(0.0, MAX_DROP, 0.0);
        BlockHitResult hit = level.clip(
                new ClipContext(pos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? pos : hit.getLocation();
    }

    @Override
    public boolean tick() {
        return ++age >= duration;
    }

    @Override
    public Vec3 position() {
        return ground;
    }

    @Override
    public int drawCount() {
        return 1;
    }

    @Override
    public void render(VfxFrame frame) {
        ParticleAtlas.ensureUploaded();
        float fade = fade();
        if (fade <= 0f) {
            return;
        }
        float[] uv = ParticleAtlas.uv(SpriteId.GLOW);
        Vec3 cam = frame.cameraPos();
        // lift a hair off the floor so the decal does not z-fight the surface it sits on
        float y = (float) (ground.y + 0.05 - cam.y);
        float x0 = (float) (ground.x - radius - cam.x);
        float x1 = (float) (ground.x + radius - cam.x);
        float z0 = (float) (ground.z - radius - cam.z);
        float z1 = (float) (ground.z + radius - cam.z);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (fade * 255f);
        Matrix4f m = frame.pose().last().pose();
        VertexConsumer vc = frame.buffers().getBuffer(VfxRenderTypes.TEXTURED_ADDITIVE);
        vc.addVertex(m, x0, y, z0).setUv(uv[0], uv[1]).setColor(r, g, b, a);
        vc.addVertex(m, x0, y, z1).setUv(uv[0], uv[3]).setColor(r, g, b, a);
        vc.addVertex(m, x1, y, z1).setUv(uv[2], uv[3]).setColor(r, g, b, a);
        vc.addVertex(m, x1, y, z0).setUv(uv[2], uv[1]).setColor(r, g, b, a);
    }

    // ramp up over the first fifth, hold, then ramp down over the last two fifths
    private float fade() {
        float t = (float) age / duration;
        if (t < 0.2f) {
            return t / 0.2f;
        }
        if (t > 0.6f) {
            return Math.max(0f, (1f - t) / 0.4f);
        }
        return 1f;
    }
}
