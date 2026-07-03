# Cascade

A code-first visual effects and sequencing library for Minecraft 1.21.1, on both NeoForge and Fabric. It gives
other mods a fluent API for particle effects, beams, and choreographed timelines.

Cascade is a developer library. On its own it adds nothing to the game.

## Example

```java
Vfx.emitter()
        .shape(ShapeSpec.sphere(0.5f))
        .count(200).lifetime(40).speed(0.2f)
        .gradient(Easings.LINEAR, 0xFFCC33, 0xFF3300)
        .play(level, pos);
```

See [docs/getting-started.md](docs/getting-started.md) for the walkthrough.

## Features

- Particle emitters with configurable shapes, size/alpha/color curves, and multi-stop gradients
- Force and steering behaviors: gravity, drag, curl noise, vortices, attractors, boids flocking
- Collision, sub-emitters, trails, soft particles, and mesh, block, and item particles
- Beams, energy domes, camera shake, cast light, and raymarched signed-distance volumes
- Effect sequencing with delays and parallel branches
- Datapack JSON effects and a registration seam for custom particle behaviors
- An optional GPU compute backend for very large bursts, off by default

## Modules

- `engine` - the simulation core: particles, easing and curves, shape sampling, and the sequencing runtime. No Minecraft dependency.
- `common` - the public builder API, effect rendering, and networking, shared by both loaders.
- `neoforge` - the NeoForge entry point and render and network glue.
- `fabric` - the Fabric entry point and render and network glue.

## Requirements

Minecraft 1.21.1, Java 21, and either NeoForge or Fabric with Fabric API.

## Status

0.1.0, the first release. The public API may still change before 1.0. See [CHANGELOG.md](CHANGELOG.md).

## License

MIT. See [LICENSE](LICENSE).
