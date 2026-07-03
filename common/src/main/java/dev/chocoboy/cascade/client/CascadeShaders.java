package dev.chocoboy.cascade.client;

import net.minecraft.client.renderer.ShaderInstance;

// holds the live soft particle core shaders. construction touches a loader-widened ShaderInstance ctor,
// so each loader builds them from its own shader-registration event and installs them here
public final class CascadeShaders {

    private static ShaderInstance soft;
    private static ShaderInstance softLit;

    private CascadeShaders() {
    }

    public static ShaderInstance soft() {
        return soft;
    }

    public static void setSoft(ShaderInstance instance) {
        soft = instance;
    }

    public static ShaderInstance softLit() {
        return softLit;
    }

    public static void setSoftLit(ShaderInstance instance) {
        softLit = instance;
    }
}
