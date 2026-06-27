package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EmitterSpecTest {

    @Test
    void buildsParticleSystemWithCount() {
        EmitterSpec spec = new EmitterSpec(
                ShapeSpec.sphere(1.5f), 80, 20, 0.1f,
                new CurveSpec(0.3f, 0f, Easings.LINEAR),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                0x66CCFF, 0x0033FF, Easings.LINEAR);
        assertEquals(80, spec.build(new Random(1)).particles().size());
    }

    @Test
    void defaultBurstIsTheOriginalPreset() {
        EmitterSpec d = EmitterSpec.defaultBurst();
        assertEquals(120, d.count());
        assertEquals(30, d.lifetime());
        assertEquals(0xFFCC33, d.colorStart());
        assertEquals(0xFF3300, d.colorEnd());
    }
}
