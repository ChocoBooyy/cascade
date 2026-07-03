# Datapack Effects

An effect can be authored as JSON in a datapack, so designers can tune it without recompiling. Place it at
`data/<namespace>/cascade/effects/<name>.json` and play it by id.

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
  "modifiers": [
    { "type": "gravity", "accel": { "x": 0, "y": -0.02, "z": 0 } }
  ]
}
```

```java
Vfx.play(level, pos, ResourceLocation.fromNamespaceAndPath("mymod", "spark"));
```

Colors are packed RGB integers. The `modifiers` array carries the same forces as the builder, keyed by
`type`: `gravity`, `drag`, `turbulence`, `attractor`, `vortex`, `curl`, `flock`, and any custom component you
register. A `sub_emitter` wraps a nested effect with a trigger of `death` or `collision`:

```json
"sub_emitter": {
  "child": { "shape": { "kind": "SPHERE", "radius": 0.1 }, "count": 60, "lifetime": 30, "speed": 0.25 },
  "trigger": "death"
}
```

An unknown id is ignored rather than throwing, so a missing datapack is never fatal.
