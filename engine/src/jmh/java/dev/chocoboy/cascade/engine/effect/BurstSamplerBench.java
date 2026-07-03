package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorSpec;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

// gpu upload prep cost: sampling 100k spawn states, the cpu-side work a GpuBurstEffect does once.
// directional spread is the rng-heaviest launch mode, so this is the worst case
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class BurstSamplerBench {

    private static final int COUNT = 100_000;

    private EmitterSpec spec;

    @Setup
    public void setUp() {
        spec = new EmitterSpec(ShapeSpec.sphere(2f), COUNT, 300, 0.05f,
                new CurveSpec(0.06f, 0.02f, Easings.LINEAR),
                new CurveSpec(1f, 0f, Easings.EASE_IN_QUAD),
                ColorSpec.of(0xFFE9A8, 0xFF9C33, Easings.LINEAR),
                List.of(), EmissionSpec.burst(), RenderSpec.DEFAULT, RotationSpec.spin(0.25f),
                CollisionSpec.NONE, null, TrailSpec.NONE,
                VelocitySpec.directional(new Vec3f(0.3f, 1f, -0.2f), 0.6f));
    }

    @Benchmark
    public void sample100k(Blackhole bh) {
        BurstSampler.sample(spec, new Random(99L), COUNT,
                (px, py, pz, vx, vy, vz, rotation, spin) -> {
                    bh.consume(px + py + pz);
                    bh.consume(vx + vy + vz);
                    bh.consume(rotation + spin);
                });
    }
}
