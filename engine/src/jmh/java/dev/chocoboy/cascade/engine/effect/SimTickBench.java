package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.emitter.Shapes;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.Curve;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

// cost of one ParticleSystem.tick at the 4000 cap under the built-in modifier stacks
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class SimTickBench {

    @Param({"bare", "gravity_drag", "turbulence_curl", "attractor_vortex"})
    private String stack;

    private ParticleSystem burst;

    @Setup(Level.Iteration)
    public void setUp() {
        burst = BenchSystems.burst(4000, modifiers(stack));
    }

    private static List<ParticleModifier> modifiers(String stack) {
        return switch (stack) {
            case "gravity_drag" -> List.of(
                    new GravitySpec(new Vec3f(0f, -0.02f, 0f)).toModifier(),
                    new DragSpec(0.05f).toModifier());
            case "turbulence_curl" -> List.of(
                    new TurbulenceSpec(0.02f, 0.5f).toModifier(),
                    new CurlSpec(0.03f, 0.4f).toModifier());
            case "attractor_vortex" -> List.of(
                    new AttractorSpec(Vec3f.ZERO, 0.03f).toModifier(),
                    new VortexSpec(Vec3f.ZERO, 0.02f).toModifier());
            default -> List.of();
        };
    }

    @Benchmark
    public void burstTick() {
        burst.tick();
    }

    // shared construction for the sim benchmarks, mirroring the engine test helpers
    static final class BenchSystems {

        // effectively immortal so the population stays constant for the whole measurement; a system
        // that reaps mid-run would measure a shrinking workload
        static final int IMMORTAL = 1_000_000_000;

        private BenchSystems() {
        }

        static ParticleSystem burst(int count, List<ParticleModifier> modifiers) {
            return system(new BurstSpawner(count), IMMORTAL, modifiers, CollisionSpec.NONE, null);
        }

        static ParticleSystem system(Spawner spawner, int lifetime, List<ParticleModifier> modifiers,
                CollisionSpec collision, CollisionProbe probe) {
            return new ParticleSystem(
                    Shapes.sphere(2f), spawner, lifetime, 0.05f, VelocitySpec.RADIAL,
                    Curve.of(0.3f, 0f, Easings.LINEAR),
                    Curve.of(1f, 0f, Easings.LINEAR),
                    ColorCurve.of(0xFFCC33, 0xFF3300, Easings.LINEAR),
                    modifiers, RotationSpec.spin(0.2f), collision, probe,
                    false, false, false, TrailSpec.NONE, new Random(31L));
        }
    }
}
