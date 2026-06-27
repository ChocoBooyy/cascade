package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.neoforge.net.ShakePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class ClientShakeHandler {

    private static final double RANGE = 32.0;

    private ClientShakeHandler() {
    }

    public static void handle(ShakePayload payload) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        double dist = player.position().distanceTo(payload.pos());
        float falloff = (float) Math.max(0.0, 1.0 - dist / RANGE);
        if (falloff <= 0f) {
            return;
        }
        ShakeController.get().shake(payload.magnitude() * falloff, payload.duration());
    }
}
