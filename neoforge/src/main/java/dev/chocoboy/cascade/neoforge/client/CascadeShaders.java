package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

public final class CascadeShaders {

    private static ShaderInstance soft;

    private CascadeShaders() {
    }

    static ShaderInstance soft() {
        return soft;
    }

    static void setSoft(ShaderInstance instance) {
        soft = instance;
    }

    // build the soft particle core shader and hand it to the loader. neoforge may swap the instance during
    // load, so the live one is captured in the setSoft callback, not the one passed to registerShader
    static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath("cascade", "cascade_soft"),
                            DefaultVertexFormat.POSITION_TEX_COLOR),
                    CascadeShaders::setSoft);
        } catch (IOException e) {
            throw new IllegalStateException("Cascade failed to load the cascade_soft core shader", e);
        }
    }
}
