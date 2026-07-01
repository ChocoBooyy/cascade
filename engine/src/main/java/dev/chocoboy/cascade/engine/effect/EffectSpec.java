package dev.chocoboy.cascade.engine.effect;

import java.util.List;

// a layered effect: several emitters that share one origin and base seed, played together
public record EffectSpec(List<EmitterSpec> emitters) {

    public EffectSpec {
        if (emitters.isEmpty()) {
            throw new IllegalArgumentException("emitters < 1");
        }
        emitters = List.copyOf(emitters);
    }

    public static EffectSpec of(EmitterSpec... emitters) {
        return new EffectSpec(List.of(emitters));
    }
}
