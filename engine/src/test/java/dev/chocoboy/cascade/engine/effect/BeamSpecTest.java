package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BeamSpecTest {

    @Test
    void buildsBeamStateWithSegmentPlusOnePoints() {
        BeamState state = new BeamSpec(0xFFFFFF, 0.2f, 0.5f, 10, 8)
                .build(Vec3f.ZERO, new Vec3f(10f, 0f, 0f), new Random(1));
        assertEquals(9, state.spine().size());
    }

    @Test
    void defaultBoltValues() {
        BeamSpec d = BeamSpec.defaultBolt();
        assertEquals(0x78C8FF, d.color());
        assertEquals(16, d.duration());
        assertEquals(12, d.segments());
    }
}
