# Getting started with Cascade

Cascade is a code-first visual effects library. Your mod plays effects from server-side code; Cascade
serializes them and streams them to nearby clients, which own the simulation and rendering. Nothing spawns
as an entity and nothing is saved.

## Adding the dependency

Cascade ships one self-contained artifact per loader (`cascade-neoforge`, `cascade-engine`). The loader
artifact already bundles the engine and shared code, so depend on exactly one of them, never both.

```groovy
repositories {
    maven { url 'https://raw.githubusercontent.com/ChocoBooyy/maven/main' }
}

dependencies {
    // pick the version matching your Minecraft: 1.0.0+1.21.1 or 1.0.0+26.1
    implementation 'dev.chocoboy.cascade:cascade-neoforge:1.0.0+26.1'
}
```

Add `cascade` as a required dependency in your `neoforge.mods.toml` (or `fabric.mod.json`) so it loads first.

## Playing your first effect

Everything starts at `Vfx`. The quickest one-liner plays the built-in burst:

```java
import dev.chocoboy.cascade.Vfx;

Vfx.burst(level, pos); // level is a ServerLevel, pos a Vec3
```

Most effects are built with a fluent builder and finished with `play`:

```java
import dev.chocoboy.cascade.Vfx;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.Easings;

Vfx.emitter()
        .shape(ShapeSpec.sphere(0.5f))
        .count(200).lifetime(40).speed(0.2f)
        .size(0.25f, 0f, Easings.EASE_OUT_QUAD)
        .gradient(Easings.LINEAR, 0xFFCC33, 0xFF3300)
        .play(level, pos);
```

## Shaping an emitter

The builder covers spawn shape, lifetime and appearance, plus force and steering behaviors that layer on top
of each other. A few of the many methods:

```java
Vfx.emitter()
        .shape(ShapeSpec.cone(1.5f, 2f))
        .count(120).lifetime(60).speed(0.15f)
        .gravity(0f, -0.02f, 0f)   // constant pull
        .curl(0.04f, 1.5f)         // fluid-like noise
        .vortex(0f, 1f, 0f, 0.03f) // swirl around a point
        .trail(5)                  // ribbon behind each particle
        .lit()                     // read world light instead of glowing
        .play(level, pos);
```

See the `VfxEmitter` javadoc for the full set: velocity modes (`implode`, `jet`, `orbit`), collision
(`collide`), sub-emitters (`burstOnDeath`, `burstOnCollision`), mesh particles (`cube`, `shard`, `block`,
`item`), soft particles, sprite animation, and more.

Layer several emitters into one effect with `Vfx.effect()`:

```java
Vfx.effect()
        .add(Vfx.emitter().shape(ShapeSpec.sphere(0.4f)).count(300).gradient(Easings.LINEAR, 0xFFE9A8, 0xB84DFF))
        .add(Vfx.emitter().shape(ShapeSpec.sphere(1.7f)).count(160).speed(0.04f).lit())
        .play(level, pos);
```

## Other effects

Beyond particles, `Vfx` plays a handful of standalone effects:

```java
Vfx.beam(level, from, to);                 // lightning bolt between two points
Vfx.dome(level, pos, 2.6f, 0x7A3DFF, 44);  // translucent energy hemisphere
Vfx.shake(level, pos, 0.3f, 14);           // camera shake for nearby players
Vfx.light(level, pos, 0xFFAA33, 4f, 30);   // a soft ground glow

Vfx.sdf()                                  // raymarched signed-distance volume
        .sphere(0f, 0f, 0f, 0.6f)
        .box(0.5f, 0f, 0f, 0.2f, 0.2f, 0.2f)
        .smoothness(0.3f)
        .play(level, pos);
```

`Vfx.beam()` returns a builder if you want to tune color, width, arc or segments before playing.

## Sequencing

`Vfx.at(level)` builds a timeline: each call appends a step, `delay` spaces them out, and `parallel` runs
branches at once. It reads top to bottom.

```java
Vfx.at(level)
        .dome(pos, 2.6f, 0x7A3DFF, 44)
        .delay(8)
        .beam(pos, pos.add(0, 4, 0))
        .shake(pos, 0.3f, 14)
        .play();
```

## Effects in a datapack

An effect can be authored as JSON under `data/<namespace>/cascade/effects/<name>.json` and played by id, so
designers can tune effects without recompiling.

```json
{
  "shape": { "kind": "SPHERE", "radius": 0.2 },
  "count": 120,
  "lifetime": 50,
  "speed": 0.3,
  "size":  { "start": 0.14, "end": 0.06, "ease": "LINEAR" },
  "alpha": { "start": 1.0,  "end": 0.0,  "ease": "LINEAR" },
  "color": { "stops": [16749576, 11812607], "ease": "LINEAR" },
  "render": { "blend": "ADDITIVE", "sprite": "SPARK", "stretch": 1.5 },
  "modifiers": [ { "type": "gravity", "accel": { "x": 0, "y": -0.02, "z": 0 } } ]
}
```

```java
import net.minecraft.resources.ResourceLocation;

Vfx.play(level, pos, ResourceLocation.fromNamespaceAndPath("mymod", "spark"));
```

An unknown id is ignored rather than throwing, so a missing datapack is never fatal.

## Custom behaviors

To add a particle behavior Cascade does not ship, implement `ComponentSpec`. It builds a `ParticleModifier`
(one per-particle update step) and names its type:

```java
public record SwirlSpec(float spin, float pull) implements ComponentSpec {

    @Override
    public ParticleModifier toModifier() {
        return new SwirlModifier(spin, pull);
    }

    @Override
    public String typeId() {
        return "mymod:swirl";
    }
}
```

Register it once at mod init, before any effect is sent or any datapack loads, so it resolves on both ends of
the wire and in effect JSON:

```java
Vfx.registerComponent("mymod:swirl",
        StreamCodec.composite(
                ByteBufCodecs.FLOAT, SwirlSpec::spin,
                ByteBufCodecs.FLOAT, SwirlSpec::pull,
                SwirlSpec::new),
        RecordCodecBuilder.<SwirlSpec>mapCodec(i -> i.group(
                Codec.FLOAT.fieldOf("spin").forGetter(SwirlSpec::spin),
                Codec.FLOAT.fieldOf("pull").forGetter(SwirlSpec::pull)
        ).apply(i, SwirlSpec::new)));
```

The component is now usable from the builder with `.component(new SwirlSpec(0.03f, 0.006f))`, and in effect
JSON as `{ "type": "mymod:swirl", "spin": 0.03, "pull": 0.006 }`.

## Where to look next

The javadoc on `Vfx` and the `Vfx*` builders documents every method. `ComponentSpec`, `ParticleModifier`,
`NeighborAware` and `PostUpdate` cover the extension seam.
