# Sequencing

`Vfx.at(level)` builds a timeline of effects. Each call appends a step, `delay` spaces steps out, and
`parallel` runs branches at once. It reads top to bottom and begins on the next tick when you call `play`.

```java
Vfx.at(level)
        .dome(pos, 2.6f, 0x7A3DFF, 44)
        .delay(8)
        .beam(pos, pos.add(0, 4, 0))
        .shake(pos, 0.3f, 14)
        .play();
```

The timeline offers the same one-liners as `Vfx` (`burst`, `beam`, `dome`, `shake`, `light`), plus `emit` and
`effect` for the builders:

```java
Vfx.at(level)
        .emit(pos, Vfx.emitter().shape(ShapeSpec.sphere(0.5f)).count(200))
        .delay(20)
        .effect(pos, someEffect)
        .play();
```

Run branches at the same time with `parallel`; the timeline continues after the longest branch finishes:

```java
Vfx.at(level)
        .parallel(
                branch -> branch.beam(from, to),
                branch -> branch.dome(pos, 2f, 0x33CCFF, 30))
        .play();
```

For anything the builders do not cover, `run(Runnable)` drops an arbitrary action into the timeline.
