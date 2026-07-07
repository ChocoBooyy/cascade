# Cascade

A code-first visual effects and sequencing library for Minecraft 1.21.1 and 26.1, on NeoForge and Fabric. Your mod
plays effects from server-side code; Cascade streams them to nearby clients, which own the simulation and
rendering. Nothing spawns as an entity and nothing is saved.

Cascade is a developer library. On its own it adds nothing to the game.

```java
Vfx.emitter()
        .shape(ShapeSpec.sphere(0.5f))
        .count(200).lifetime(40).speed(0.2f)
        .gradient(Easings.LINEAR, 0xFFCC33, 0xFF3300)
        .play(level, pos);
```

## Pages

- [Getting Started](Getting-Started) - add the dependency and play your first effect
- [Emitters](Emitters) - shape, appearance, and motion of a particle emitter
- [Forces and Steering](Forces-and-Steering) - gravity, noise, vortices, flocking
- [Effects](Effects) - beams, domes, shake, light, and signed-distance volumes
- [Sequencing](Sequencing) - timelines with delays and parallel branches
- [Datapack Effects](Datapack-Effects) - author effects as JSON
- [Custom Components](Custom-Components) - add particle behaviors of your own
- [Capabilities and Limits](Capabilities-and-Limits) - what works where, and each feature's bounds

Every method is documented in the javadoc on `Vfx` and the `Vfx*` builders.
