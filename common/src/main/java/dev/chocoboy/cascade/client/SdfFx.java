package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// the sdf volume backend: one raymarching pipeline and one shared uniform buffer. on 26.1 the volume
// draws through a raw render pass against the main target's color and depth (or the bloom capture's,
// when its overrides are up), with all per-volume data in a std140 block; the gl backend executes
// passes in order, so one shared buffer rewritten before each draw serves every volume in a frame.
// any failure still disables sdf volumes for the session and live effects remove themselves. public
// because the loader's pipeline registration lives in another package
public final class SdfFx {

    static final int MAX_COLOR_STOPS = 8;
    // mat4 + five vec4-sized fields + four vec4[8] arrays, in std140 layout
    static final int CONFIG_BYTES = 64 + 5 * 16 + 4 * 8 * 16;

    public static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("cascade", "sdf_volume"))
            .withVertexShader(Identifier.fromNamespaceAndPath("cascade", "core/sdf_volume"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("cascade", "core/sdf_volume"))
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("SdfConfig", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");

    private static boolean failed;
    private static GpuBuffer config;

    private SdfFx() {
    }

    // draws one volume from its packed std140 config; throws to the caller's fail-soft on any gpu error
    static void draw(ByteBuffer packed) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        if (config == null) {
            config = RenderSystem.getDevice().createBuffer(() -> "cascade sdf config",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, CONFIG_BYTES);
        }
        encoder.writeToBuffer(config.slice(), packed);
        // honour the bloom capture's overrides like RenderType draws do, so volumes bloom too
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        GpuTextureView color = RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride : main.getColorTextureView();
        GpuTextureView depth = RenderSystem.outputDepthTextureOverride != null
                ? RenderSystem.outputDepthTextureOverride : main.getDepthTextureView();
        try (RenderPass pass = encoder.createRenderPass(
                () -> "cascade sdf volume", color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
            pass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("SdfConfig", config);
            pass.draw(0, 6);
        }
    }

    // permanently drops sdf volumes for the session; the rest of the library is unaffected
    static void fail(String what, RuntimeException e) {
        if (!failed) {
            failed = true;
            LOGGER.error("Cascade sdf rendering failed while {}, disabling sdf volumes", what, e);
        }
    }

    static boolean failed() {
        return failed;
    }
}
