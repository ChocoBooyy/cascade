package dev.chocoboy.cascade.engine.effect;

import java.util.List;
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

// flock tick cost, dominated by the per-tick spatial hash build and the 3x3x3 neighbor queries
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class FlockBench {

    @Param({"500", "2000"})
    private int boids;

    private ParticleSystem flock;

    @Setup(Level.Iteration)
    public void setUp() {
        flock = SimTickBench.BenchSystems.burst(boids,
                List.of(new FlockSpec(3f, 0.02f, 0.015f, 0.012f, 0.14f).toModifier()));
    }

    @Benchmark
    public void flockTick() {
        flock.tick();
    }
}
