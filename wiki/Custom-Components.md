# Custom Components

A component is a particle behavior. Cascade ships gravity, drag, curl noise, and the rest, but you can add
your own without touching the engine. Implement `ComponentSpec`: it builds a `ParticleModifier` (one
per-particle update step, run each tick) and names its type.

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

The modifier is where the motion lives. It adjusts velocity in place:

```java
public final class SwirlModifier implements ParticleModifier {

    private final float spin;
    private final float pull;

    public SwirlModifier(float spin, float pull) {
        this.spin = spin;
        this.pull = pull;
    }

    @Override
    public void apply(Particle p) {
        // tangential push around the y axis plus a gentle inward pull
        float x = p.pos.x();
        float z = p.pos.z();
        p.vel = p.vel.add(new Vec3f(-z * spin - x * pull, 0f, x * spin - z * pull));
    }
}
```

Register it once at mod init, before any effect is sent or datapack loads, so it resolves on both ends of the
wire and in JSON:

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

The `StreamCodec` carries the spec to the client; the `MapCodec` parses it from JSON. The component now works
from the builder:

```java
.component(new SwirlSpec(0.03f, 0.006f))
```

and in a datapack effect's `modifiers` array:

```json
{ "type": "mymod:swirl", "spin": 0.03, "pull": 0.006 }
```

## Extra passes

A modifier may also implement one of two optional interfaces:

- `PostUpdate` runs after the particle has moved, for behavior that reacts to the new position rather than
  just the velocity.
- `NeighborAware` steers by nearby particles against a start-of-tick snapshot of the swarm, the way the
  built-in `flock` works.
