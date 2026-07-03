package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.net.ShakePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class ClientShakeHandler {

    private ClientShakeHandler() {
    }

    public static void handle(ShakePayload payload) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        double dist = player.position().distanceTo(payload.pos());
        float falloff = (float) Math.max(0.0, 1.0 - dist / ShakePayload.RADIUS);
        if (falloff <= 0f) {
            return;
        }
        ShakeController.get().shake(payload.magnitude() * falloff, payload.duration());
    }
}
