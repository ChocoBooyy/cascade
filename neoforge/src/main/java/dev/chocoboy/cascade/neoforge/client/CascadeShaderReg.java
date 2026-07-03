package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.chocoboy.cascade.client.CascadeShaders;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

public final class CascadeShaderReg {

    private CascadeShaderReg() {
    }

    // neoforge may swap the instance during load, so the live one is captured in the set callback
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(newShader(event, "cascade_soft", DefaultVertexFormat.POSITION_TEX_COLOR),
                    CascadeShaders::setSoft);
            event.registerShader(newShader(event, "cascade_soft_lit", DefaultVertexFormat.PARTICLE),
                    CascadeShaders::setSoftLit);
        } catch (IOException e) {
            throw new IllegalStateException("Cascade failed to load a soft particle core shader", e);
        }
    }

    private static ShaderInstance newShader(RegisterShadersEvent event, String name, com.mojang.blaze3d.vertex.VertexFormat format)
            throws IOException {
        return new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath("cascade", name), format);
    }
}
