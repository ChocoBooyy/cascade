package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.CollisionProbe;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.MeshId;
import dev.chocoboy.cascade.engine.effect.Particle;
import dev.chocoboy.cascade.engine.effect.ParticleSystem;
import dev.chocoboy.cascade.engine.effect.RenderSpec;
import dev.chocoboy.cascade.engine.effect.SubEmitterSpec;
import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ParticleBurstEffect implements RenderedEffect {

    // hard ceiling on sub-emitter nesting so a self-referencing spec cannot recurse without bound
    private static final int MAX_DEPTH = 4;

    private final Vec3 origin;
    private final ParticleSystem sim;
    private final RenderSpec render;
    private final SubEmitterSpec subEmitter;
    private final int depth;
    private final float[] uv;

    public ParticleBurstEffect(Vec3 origin, ParticleSystem sim, RenderSpec render, SubEmitterSpec subEmitter, int depth) {
        this.origin = origin;
        this.sim = sim;
        this.render = render;
        this.subEmitter = subEmitter;
        this.depth = depth;
        this.uv = ParticleAtlas.uv(render.sprite());
    }

    @Override
    public boolean tick() {
        boolean done = sim.tick();
        if (subEmitter != null && depth < MAX_DEPTH) {
            List<Vec3f> deaths = sim.drainSpawnRequests();
            for (int i = 0; i < deaths.size(); i++) {
                spawnChild(deaths.get(i));
            }
        }
        return done;
    }

    // build the child system where a parent particle died and hand it to the manager as its own effect
    private void spawnChild(Vec3f local) {
        EmitterSpec child = subEmitter.child();
        Vec3 childOrigin = origin.add(local.x(), local.y(), local.z());
        CollisionProbe probe = child.collision().enabled()
                ? new LevelCollisionProbe(Minecraft.getInstance().level, childOrigin)
                : null;
        ParticleSystem childSim = child.build(new Random(), probe, ParticleQuality.density());
        VfxRenderManager.get().spawn(
                new ParticleBurstEffect(childOrigin, childSim, child.render(), child.subEmitter(), depth + 1));
    }

    @Override
    public Vec3 position() {
        return origin;
    }

    @Override
    public int drawCount() {
        int n = sim.particles().size();
        if (render.mesh() == MeshId.ITEM) {
            return n * 4;
        }
        if (render.mesh() == MeshId.BLOCK) {
            return n * Math.max(1, BlockMeshCache.quadsFor(render.meshModel()).size());
        }
        return render.mesh() != MeshId.NONE ? n * 6 : n;
    }

    @Override
    public boolean soft() {
        return render.soft();
    }

    @Override
    public void render(VfxFrame frame) {
        switch (render.mesh()) {
            case ITEM -> renderItemMesh(frame);
            case BLOCK -> renderBlockMesh(frame);
            case CUBE, SHARD -> renderMesh(frame);
            case NONE -> renderBillboards(frame);
        }
    }

    private void renderBillboards(VfxFrame frame) {
        ParticleAtlas.ensureUploaded();
        boolean lit = render.lit();
        RenderType type = billboardType();
        RenderType unlit = unlitType();
        Level level = lit ? Minecraft.getInstance().level : null;
        Vec3 cam = frame.cameraPos();
        Quaternionf camRot = frame.cameraRotation();
        float stretch = render.stretch();
        boolean animate = render.animate();
        // the billboard plane axes in world space, so velocity can be projected onto the quad when streaking
        Vector3f right = camRot.transform(new Vector3f(1f, 0f, 0f));
        Vector3f up = camRot.transform(new Vector3f(0f, 1f, 0f));

        frame.queue().submit(type, vc -> {
            for (Particle p : sim.particles()) {
                int color = sim.colorOf(p);
                int alpha = (int) (sim.alphaOf(p) * 255f);
                float size = sim.sizeOf(p);
                float hx = size;
                float hy = size;
                float roll = p.rotation;
                float[] cell = animate ? ParticleAtlas.uv(render.sprite(), frameOf(p)) : uv;
                if (stretch > 0f) {
                    float speed = p.vel.length();
                    if (speed > 1e-5f) {
                        float vx = p.vel.x() * right.x + p.vel.y() * right.y + p.vel.z() * right.z;
                        float vy = p.vel.x() * up.x + p.vel.y() * up.y + p.vel.z() * up.z;
                        roll = (float) Math.atan2(vy, vx);
                        hx = size * (1f + speed * stretch);
                    }
                }
                float wx = (float) (origin.x + p.pos.x() - cam.x);
                float wy = (float) (origin.y + p.pos.y() - cam.y);
                float wz = (float) (origin.z + p.pos.z() - cam.z);
                int cr = (color >> 16) & 0xFF;
                int cg = (color >> 8) & 0xFF;
                int cb = color & 0xFF;
                if (lit) {
                    int light = lightAt(level, p);
                    Billboards.litQuad(frame.pose(), vc, camRot, wx, wy, wz, hx, hy, roll, cell, cr, cg, cb, alpha, light);
                } else {
                    Billboards.quad(frame.pose(), vc, camRot, wx, wy, wz, hx, hy, roll, cell, cr, cg, cb, alpha);
                }
            }
        });

        if (hasTrails()) {
            Matrix4f m = frame.pose().last().pose();
            frame.queue().submit(unlit, vc -> {
                for (Particle p : sim.particles()) {
                    if (p.trail != null && p.trailCount >= 2) {
                        int color = sim.colorOf(p);
                        Billboards.ribbon(m, vc, p, origin, cam, uv,
                                (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, sim.alphaOf(p), sim.sizeOf(p));
                    }
                }
            });
        }
    }

    private void renderBlockMesh(VfxFrame frame) {
        List<BakedQuad> quads = BlockMeshCache.quadsFor(render.meshModel());
        if (quads.isEmpty()) {
            return;
        }
        Level level = Minecraft.getInstance().level;
        Vec3 cam = frame.cameraPos();
        PoseStack pose = frame.pose();
        frame.queue().submit(RenderType.cutout(), vc -> {
            Quaternionf rot = new Quaternionf();
            for (Particle p : sim.particles()) {
                float size = sim.sizeOf(p);
                float s = size * 2f;   // block models span a unit cube, size is a half extent, so double it
                float wx = (float) (origin.x + p.pos.x() - cam.x);
                float wy = (float) (origin.y + p.pos.y() - cam.y);
                float wz = (float) (origin.z + p.pos.z() - cam.z);
                // block debris always reads scene light, there is no full bright variant like cube and shard have
                int light = level != null ? lightAt(level, p) : 0xF000F0;
                // this path pushes the shared frame pose, so the pop must run even if a quad throws, or the rest
                // of the frame draws on a corrupted stack
                pose.pushPose();
                try {
                    pose.translate(wx, wy, wz);
                    pose.mulPose(rot.rotationYXZ(p.yaw, p.pitch, p.rotation));
                    pose.scale(s, s, s);
                    pose.translate(-0.5f, -0.5f, -0.5f);   // center the 0..1 block model on the particle
                    PoseStack.Pose last = pose.last();
                    for (int i = 0; i < quads.size(); i++) {
                        vc.putBulkData(last, quads.get(i), 1f, 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
                    }
                } finally {
                    pose.popPose();
                }
            }
        });
    }

    private void renderItemMesh(VfxFrame frame) {
        ItemStack stack = ItemMeshCache.stackFor(render.meshModel());
        if (stack.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Vec3 cam = frame.cameraPos();
        PoseStack pose = frame.pose();
        ItemRenderer items = mc.getItemRenderer();
        // renderStatic picks its render types internally, so this path cannot group by type
        frame.queue().submitDirect(buffers -> {
            Quaternionf rot = new Quaternionf();
            for (Particle p : sim.particles()) {
                float s = sim.sizeOf(p) * 3f;   // GROUND transform already shrinks the model, so scale up to match
                float wx = (float) (origin.x + p.pos.x() - cam.x);
                float wy = (float) (origin.y + p.pos.y() - cam.y);
                float wz = (float) (origin.z + p.pos.z() - cam.z);
                int light = level != null ? lightAt(level, p) : 0xF000F0;
                pose.pushPose();
                try {
                    pose.translate(wx, wy, wz);
                    pose.mulPose(rot.rotationYXZ(p.yaw, p.pitch, p.rotation));
                    pose.scale(s, s, s);
                    items.renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                            pose, buffers, level, 0);
                } finally {
                    pose.popPose();
                }
            }
        });
    }

    private void renderMesh(VfxFrame frame) {
        boolean lit = render.lit();
        RenderType type = lit ? CascadeRenderTypes.solidLit() : CascadeRenderTypes.solid();
        Level level = lit ? Minecraft.getInstance().level : null;
        Vec3 cam = frame.cameraPos();
        Matrix4f pose = frame.pose().last().pose();
        Vector3f scale = MeshGeometry.scaleFor(render.mesh());
        frame.queue().submit(type, vc -> {
            Vector3f v = new Vector3f();
            Quaternionf rot = new Quaternionf();
            for (Particle p : sim.particles()) {
                int color = sim.colorOf(p);
                int a = (int) (sim.alphaOf(p) * 255f);
                float size = sim.sizeOf(p);
                int cr = (color >> 16) & 0xFF;
                int cg = (color >> 8) & 0xFF;
                int cb = color & 0xFF;
                double wx = origin.x + p.pos.x() - cam.x;
                double wy = origin.y + p.pos.y() - cam.y;
                double wz = origin.z + p.pos.z() - cam.z;
                rot.rotationYXZ(p.yaw, p.pitch, p.rotation);
                int light = lit ? lightAt(level, p) : 0;
                for (float[] face : MeshGeometry.CUBE_FACES) {
                    for (int i = 0; i < 4; i++) {
                        v.set(face[i * 3] * scale.x, face[i * 3 + 1] * scale.y, face[i * 3 + 2] * scale.z);
                        v.mul(size);
                        rot.transform(v);
                        float fx = (float) (wx + v.x);
                        float fy = (float) (wy + v.y);
                        float fz = (float) (wz + v.z);
                        if (lit) {
                            vc.addVertex(pose, fx, fy, fz).setColor(cr, cg, cb, a).setLight(light);
                        } else {
                            vc.addVertex(pose, fx, fy, fz).setColor(cr, cg, cb, a);
                        }
                    }
                }
            }
        });
    }

    // the unlit textured type. trails always draw with this, and it is the fallback for an unlit billboard pass
    private RenderType unlitType() {
        if (render.blend() != BlendMode.ALPHA) {
            return CascadeRenderTypes.texturedAdditive();
        }
        return render.soft() ? CascadeRenderTypes.texturedAlphaSoft() : CascadeRenderTypes.texturedAlpha();
    }

    // the textured type for the billboard pass, lit or unlit
    private RenderType billboardType() {
        if (!render.lit()) {
            return unlitType();
        }
        if (render.blend() != BlendMode.ALPHA) {
            return CascadeRenderTypes.texturedAdditiveLit();
        }
        return render.soft() ? CascadeRenderTypes.texturedAlphaLitSoft() : CascadeRenderTypes.texturedAlphaLit();
    }

    // scene light at the particle's world cell
    private int lightAt(Level level, Particle p) {
        return LevelRenderer.getLightColor(level, BlockPos.containing(
                origin.x + p.pos.x(), origin.y + p.pos.y(), origin.z + p.pos.z()));
    }

    // trails are allocated for every particle of a trail enabled system, so the first answers for all
    private boolean hasTrails() {
        return !sim.particles().isEmpty() && sim.particles().get(0).trail != null;
    }

    // map a particle's life fraction onto an atlas frame, clamped to the last frame at end of life
    private static int frameOf(Particle p) {
        int frame = (int) (p.life() * ParticleAtlas.FRAMES);
        return frame >= ParticleAtlas.FRAMES ? ParticleAtlas.FRAMES - 1 : frame;
    }
}
