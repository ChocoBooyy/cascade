package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

// Selective bloom: cascade's effects are re-rendered into an offscreen capture, blurred, and added back
// over the frame. It rode a PostChain plus a RenderTarget driven through RenderSystem's immediate gl state,
// both reworked in 26.1 (the post pipeline and the GpuTexture-backed targets). The backend is disabled on
// this port: the toggle still holds so the testmod command and loader wiring compile and behave, but the
// capture and blur are no-ops, so the frame is untouched. The full implementation lives on the 1.0.0 branch
// for the re-port. see the porting notes
public final class PostFx {

    private static boolean enabled;

    private PostFx() {
    }

    public static void setEnabled(boolean on) {
        if (on) {
            MeshDebris.warnOnce("bloom");
        }
        enabled = on;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void captureVfx(PoseStack pose, MultiBufferSource.BufferSource buffers, Quaternionf camRot,
            Vec3 camPos) {
    }

    public static void process(float partialTick) {
    }
}
