# Emitters

An emitter is built with `Vfx.emitter()` and finished with `play(level, pos)`. It starts from the default
burst, so you override only what you need. This page walks the main groups of settings; the full list is in
the `VfxEmitter` javadoc.

## Shape and count

The spawn volume comes from `ShapeSpec`: `point`, `line`, `ring`, `sphere`, `cone`, `box`, `disc`, or
`hemisphere`. Sizes are in blocks.

```java
Vfx.emitter()
        .shape(ShapeSpec.cone(1.5f, 2f))
        .count(120)     // particles in a burst
        .lifetime(60)   // ticks each particle lives
        .speed(0.15f)   // initial speed
        .play(level, pos);
```

For a steady stream instead of a one-shot burst, use `rate`:

```java
.rate(4f, 40) // 4 particles per tick for 40 ticks; count is then ignored
```

## Appearance

Size, alpha, and color are curves over a particle's life, eased by an `Easings` value. Color can be a two-stop
fade or a multi-stop gradient.

```java
.size(0.25f, 0f, Easings.EASE_OUT_QUAD)   // start, end, ease
.alpha(1f, 0f, Easings.LINEAR)
.gradient(Easings.LINEAR, 0xFFE9A8, 0xB84DFF, 0x3A2C86)
```

Pick the atlas sprite and blend, and layer on optional looks:

```java
.sprite(SpriteId.SMOKE)   // GLOW, SMOKE, SPARK, RING, STAR, SHARD
.blend(BlendMode.ALPHA)   // ADDITIVE glows, ALPHA occludes
.lit()                    // read world light instead of drawing full bright
.soft()                   // fade against scene geometry (alpha blend only)
.animate()                // play the sprite's frames across the life
.stretch(1.5f)            // streak fast particles along their velocity
.spin(0.2f)               // random roll plus a per-tick spin
.trail(5)                 // ribbon through the last 5 positions
```

## Motion

By default particles fire radially outward. The other velocity modes:

```java
.implode()               // inward toward the shape center
.orbit()                 // tangent to the vertical axis, a swirl
.jet(0f, 1f, 0f, 0.3f)   // a cone along a direction, half-angle in radians
```

Layer on forces (gravity, curl noise, vortices, flocking) as described in
[Forces and Steering](Forces-and-Steering).

## Meshes

Instead of billboards, particles can be solid geometry. Meshes are opaque and fade by shrinking their size
curve rather than by alpha, so pair them with a `spin`.

```java
.cube()                // tumbling solid box
.shard()               // elongated splinter
.block(Blocks.STONE)   // a real block model, for break debris
.item(Items.DIAMOND)   // a real item model
```

## Collision and sub-emitters

Particles can bounce off blocks and spawn child systems on death or impact:

```java
.collide(0.4f, 0.2f)                         // bounce, friction
.burstOnDeath(Vfx.emitter().count(20))       // child fires where a particle dies
.burstOnCollision(Vfx.emitter().count(10))   // child fires on first block contact
```

## Layering

Combine several emitters into one effect played at a single point:

```java
Vfx.effect()
        .add(Vfx.emitter().shape(ShapeSpec.sphere(0.4f)).count(300))
        .add(Vfx.emitter().shape(ShapeSpec.sphere(1.7f)).count(160).lit())
        .play(level, pos);
```
