package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.List;
import org.junit.jupiter.api.Test;

class EffectSpecTest {

    private static EmitterSpec emitter() {
        return new EmitterSpec(ShapeSpec.point(), 4, 10, 0f,
                new CurveSpec(1f, 0f, Easings.LINEAR), new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFFFFF, 0x000000, Easings.LINEAR);
    }

    @Test
    void holdsItsEmittersInOrder() {
        EmitterSpec a = emitter();
        EmitterSpec b = emitter();
        EffectSpec effect = EffectSpec.of(a, b);
        assertEquals(List.of(a, b), effect.emitters());
    }

    @Test
    void rejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new EffectSpec(List.of()));
    }
}
