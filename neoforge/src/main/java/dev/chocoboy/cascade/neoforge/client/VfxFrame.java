package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

// the per-frame render inputs an effect needs, so render methods take one argument instead of four
public record VfxFrame(PoseStack pose, VertexConsumer vertexConsumer, Quaternionf cameraRotation, Vec3 cameraPos) {
}
