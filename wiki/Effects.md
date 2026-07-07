# Effects

Besides particle emitters, `Vfx` plays several standalone effects. Each sends to players near the target
point.

## Beam

A lightning-style bolt between two points.

```java
Vfx.beam(level, from, to);
```

Tune it with the builder before playing:

```java
Vfx.beam()
        .color(0x66CCFF).width(0.2f)
        .arc(0.5f)      // how far it bows off the straight line
        .segments(12)   // more segments read as a jaggeder arc
        .play(level, from, to);
```

## Dome

A translucent energy hemisphere, for force fields and zones.

```java
Vfx.dome(level, pos, 2.6f, 0x7A3DFF, 44); // radius, color, duration in ticks
```

## Camera shake and light

```java
Vfx.shake(level, pos, 0.3f, 14);          // magnitude in blocks, duration in ticks
Vfx.light(level, pos, 0xFFAA33, 4f, 30);  // a soft ground glow: color, radius, duration
```

Shake is NeoForge-only: Fabric clients accept the payload and ignore it, so mixed-loader servers can send
it freely. See [Capabilities and Limits](Capabilities-and-Limits).

## Signed-distance volumes

A raymarched volume built from primitive shapes fused by a smooth union. Offsets and sizes are in blocks.

```java
Vfx.sdf()
        .sphere(0f, 0f, 0f, 0.6f)
        .box(0.5f, 0f, 0f, 0.2f, 0.2f, 0.2f)
        .torus(-0.5f, 0f, 0f, 0.4f, 0.1f)
        .smoothness(0.3f)                       // larger values fuse the shapes into one mass
        .gradient(Easings.LINEAR, 0x66CCFF, 0xFFFFFF)
        .rotate(0.02f)                          // spin, radians per tick
        .play(level, pos);
```
