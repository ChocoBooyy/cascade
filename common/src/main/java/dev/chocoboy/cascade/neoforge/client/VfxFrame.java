package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

// the per-frame render inputs an effect needs, so render methods take one argument instead of five.
// effects submit their draw work to the queue, which groups it by render type before anything is drawn;
// the raw buffer source is still here for a draw that must bypass the queue, at the cost of batching
public record VfxFrame(PoseStack pose, MultiBufferSource buffers, Quaternionf cameraRotation, Vec3 cameraPos,
        VfxRenderQueue queue) {
}
