# Changelog

Notable changes to Cascade, newest first. Versions follow [semver](https://semver.org); while the library is
pre-1.0 the public API can still change between minor releases.

## 0.1.0 - 2026-07-03

First release. Cascade is a code-first visual effects and sequencing library for Minecraft 1.21.1, running on
both NeoForge and Fabric from one shared core. On its own it adds nothing to the game; other mods drive it.

### Particles

- Fluent emitters with configurable shape, count, lifetime, speed, and size/alpha/color curves, including
  multi-stop color gradients.
- Force and steering behaviors: gravity, drag, turbulence, curl noise, point attractors, vortices, and boids
  flocking. Emitters can also carry custom behaviors registered by other mods.
- Velocity modes: radial, inward, orbital, and directional cones.
- Block collision with bounce and friction, and sub-emitters that fire on a particle's death or first impact.
- Trails, camera-facing billboards with optional velocity stretch and world lighting, sprite animation, and
  soft particles that fade against scene geometry instead of clipping through it.
- Mesh particles drawn as tumbling cubes, shards, or real block and item models.
- Optional GPU compute backend for hundred-thousand particle bursts, off by default with a CPU fallback.

### Other effects

- Lightning-style beams, translucent energy domes, camera shake, a soft ground glow to fake cast light, and
  raymarched signed-distance volumes built from fused primitives.

### Authoring

- A fluent builder API rooted at `Vfx`, plus effect sequencing with delays and parallel branches.
- Effects can be authored as datapack JSON under `data/<namespace>/cascade/effects/`.
- Custom particle components register on both the wire and JSON through `Vfx.registerComponent`.

### Engine

- The simulation core is a plain-Java module with no Minecraft dependency, deterministic given a seed, and
  covered by unit tests and a JMH benchmark suite.
