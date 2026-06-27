package dev.chocoboy.cascade.neoforge.client;

import java.util.Random;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class ShakeController {

    private static final ShakeController INSTANCE = new ShakeController();

    private final Random rng = new Random();
    private float magnitude;
    private int remaining;
    private int duration;

    private ShakeController() {
    }

    public static ShakeController get() {
        return INSTANCE;
    }

    public void shake(float magnitude, int durationTicks) {
        this.magnitude = magnitude;
        this.remaining = durationTicks;
        this.duration = durationTicks;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (remaining > 0) {
            remaining--;
        }
    }

    @SubscribeEvent
    public void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (remaining <= 0) {
            return;
        }
        // decay linearly over the shake's life; roll stays subtler so the view does not spin
        float m = magnitude * remaining / duration;
        event.setYaw(event.getYaw() + jitter(m));
        event.setPitch(event.getPitch() + jitter(m));
        event.setRoll(event.getRoll() + jitter(m * 0.5f));
    }

    private float jitter(float m) {
        return (rng.nextFloat() * 2f - 1f) * m;
    }
}
