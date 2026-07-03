package dev.chocoboy.cascade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;

// maps the vanilla Particles video setting onto a spawn-count multiplier, so Cascade thins itself the
// same way vanilla thins its own particles when a player picks Decreased or Minimal
final class ParticleQuality {

    private ParticleQuality() {
    }

    static float density() {
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        return switch (status) {
            case ALL -> 1f;
            case DECREASED -> 0.5f;
            case MINIMAL -> 0.2f;
        };
    }
}
