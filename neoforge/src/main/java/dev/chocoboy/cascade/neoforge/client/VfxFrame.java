package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

// the per-frame render inputs an effect needs, so render methods take one argument instead of four. The
// buffer source lets each effect pull the vertex consumer for the render type it actually wants.
public record VfxFrame(PoseStack pose, MultiBufferSource buffers, Quaternionf cameraRotation, Vec3 cameraPos) {
}
