package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

// resolves an item id to its ground-context model quads, cached per id. Unknown ids yield no layers, so a
// bad id draws nothing. The model and atlas are client state, so this stays out of the engine.
//
// 26.1 replaced ItemRenderer.renderStatic with a retained submit pipeline: a resolver fills an
// ItemStackRenderState, and submitting it hands each model layer's quads to a SubmitNodeCollector for
// vanilla's frame passes, which cascade's own render pass cannot join. so the cache submits ONCE into a
// capture collector against an identity pose and keeps what comes out: per layer, the ground-context
// transform the submit applied, the baked quads, and the tint colors. debris then replays those layers
// through cascade's own buffers every frame. glint foil layers are not replayed, debris has no shimmer
final class ItemMeshCache {

    // one captured model layer: the pose submit built for it (relative to the identity root), its quads,
    // and the resolved tint colors indexed by the quads' tint indices
    record Layer(Matrix4f pose, List<BakedQuad> quads, int[] tints) {
    }

    private static final Map<String, List<Layer>> CACHE = new HashMap<>();

    private ItemMeshCache() {
    }

    static List<Layer> layersFor(String itemId) {
        return CACHE.computeIfAbsent(itemId, ItemMeshCache::capture);
    }

    private static List<Layer> capture(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return List.of();
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
        Minecraft mc = Minecraft.getInstance();
        ItemStackRenderState state = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GROUND, mc.level, null, 0);
        CaptureCollector collector = new CaptureCollector();
        state.submit(new PoseStack(), collector, 0xF000F0, 0, 0);
        return List.copyOf(collector.layers);
    }

    // a collector that keeps item layers and drops everything else. light and overlay are ignored here,
    // the debris pass re-stamps them per particle
    private static final class CaptureCollector implements SubmitNodeCollector {

        final List<Layer> layers = new ArrayList<>();

        @Override
        public OrderedSubmitNodeCollector order(int order) {
            return this;
        }

        @Override
        public void submitItem(PoseStack pose, ItemDisplayContext context, int light, int overlay,
                int outlineColor, int[] tints, List<BakedQuad> quads,
                ItemStackRenderState.FoilType foil) {
            layers.add(new Layer(new Matrix4f(pose.last().pose()), List.copyOf(quads), tints.clone()));
        }

        @Override
        public void submitShadow(PoseStack pose, float strength, List<EntityRenderState.ShadowPiece> pieces) {
        }

        @Override
        public void submitNameTag(PoseStack pose, Vec3 offset, int light, Component name, boolean discrete,
                int y, double distance, CameraRenderState camera) {
        }

        @Override
        public void submitText(PoseStack pose, float x, float y, FormattedCharSequence text, boolean dropShadow,
                Font.DisplayMode mode, int light, int color, int backgroundColor, int outlineColor) {
        }

        @Override
        public void submitFlame(PoseStack pose, EntityRenderState state, Quaternionf rotation) {
        }

        @Override
        public void submitLeash(PoseStack pose, EntityRenderState.LeashState leash) {
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack pose, RenderType type,
                int light, int overlay, int color, TextureAtlasSprite sprite, int outlineColor,
                ModelFeatureRenderer.CrumblingOverlay crumbling) {
        }

        @Override
        public void submitModelPart(ModelPart part, PoseStack pose, RenderType type, int light, int overlay,
                TextureAtlasSprite sprite, boolean fullBright, boolean noRender, int color,
                ModelFeatureRenderer.CrumblingOverlay crumbling, int outlineColor) {
        }

        @Override
        public void submitMovingBlock(PoseStack pose, MovingBlockRenderState state) {
        }

        @Override
        public void submitBlockModel(PoseStack pose, RenderType type, List<BlockStateModelPart> parts,
                int[] tints, int light, int overlay, int outlineColor) {
        }

        @Override
        public void submitBreakingBlockModel(PoseStack pose, BlockStateModel model, long seed, int progress) {
        }

        @Override
        public void submitCustomGeometry(PoseStack pose, RenderType type,
                SubmitNodeCollector.CustomGeometryRenderer renderer) {
        }

        @Override
        public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
        }
    }
}
