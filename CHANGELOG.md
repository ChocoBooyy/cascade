# Changelog

Notable changes to Cascade, newest first. Versions follow [semver](https://semver.org): the public API is
stable across 1.x, and a breaking change would come with a 2.0.

## 1.0.0+26.1 - 2026-07-08

The Minecraft 26.1 port of the 1.0.0 release. The public API is unchanged; every 1.0.0 feature renders on
26.1's rewritten GPU pipeline, verified against a side-by-side 1.21.1 build.

### Changed

- The render layer moved from the removed `RenderStateShard`/`ShaderInstance` composite API onto 26.1's
  `RenderPipeline`/`RenderSetup` model, shared by both loaders from the common module.
- The GPU burst backend no longer uses compute shaders: spawn state uploads once as a vertex buffer and the
  vertex shader re-integrates the forces each frame. The old OpenGL 4.3 requirement is gone; the backend now
  runs wherever Minecraft does.
- Bloom rides 26.1's post-effect format; soft particles, lit meshes, and the sprite alpha cutout ship as
  vendored core shader stages, since vanilla dropped or changed theirs.
- HUD particles draw through the retained GUI as batched blits.

### Requirements

- Minecraft 26.1, Java 25, NeoForge or Fabric with Fabric API. The 1.21.1 line continues as `1.0.0` on the
  `1.21.1` branch.

## 1.0.0 - 2026-07-04

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
