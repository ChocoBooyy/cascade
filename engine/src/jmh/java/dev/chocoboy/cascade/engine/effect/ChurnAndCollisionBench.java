package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

// the two pathological tick shapes: rate-mode spawn/reap churn and per-tick collision contact
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ChurnAndCollisionBench {

    private ParticleSystem churn;
    private ParticleSystem colliding;

    @Setup(Level.Iteration)
    public void setUp() {
        // short lives against an endless rate: the measurement is dominated by spawn + swap-remove reap
        churn = SimTickBench.BenchSystems.system(
                new RateSpawner(200f, SimTickBench.BenchSystems.IMMORTAL), 5,
                List.of(), CollisionSpec.NONE, null);
        // gravity presses everything into the floor so contact happens every tick
        colliding = SimTickBench.BenchSystems.system(
                new BurstSpawner(4000), SimTickBench.BenchSystems.IMMORTAL,
                List.of(new GravitySpec(new Vec3f(0f, -0.05f, 0f)).toModifier()),
                CollisionSpec.bouncy(0.5f, 0.1f), (x, y, z) -> y < 0f);
    }

    @Benchmark
    public void rateChurnTick() {
        churn.tick();
    }

    @Benchmark
    public void collisionTick() {
        colliding.tick();
    }
}
