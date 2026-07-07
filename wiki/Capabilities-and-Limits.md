# Capabilities and Limits

What Cascade can and cannot do, in one place. Everything not listed here works the same everywhere.

## Loader support

Both loaders run the same core, and every feature behaves identically on NeoForge and Fabric with one
exception:

| Feature | NeoForge | Fabric |
| --- | --- | --- |
| Particles, beams, domes, light, SDF volumes, sequencing, JSON effects | yes | yes |
| Soft particles, lit particles, mesh/block/item debris | yes | yes |
| Bloom, GPU backend, HUD particles | yes | yes |
| Camera shake (`Vfx.shake`, `VfxSequence.shake`) | yes | no |

On Fabric the shake payload is accepted and ignored, so sequences that include a shake still run correctly;
players just feel no kick. Fabric exposes no camera-angles hook and Cascade ships no mixins, so this stays
until Fabric API grows one.

## Per-feature limits

### Billboarded particles

- `soft()` applies to alpha-blended billboards only. Additive billboards, trails, and meshes ignore it.
- `stretch(...)` is billboard-only; mesh particles tumble instead.
- Soft particles fade over a fixed 1.5 blocks; the distance is not per-effect.

### Mesh, block, and item particles

- Cube and shard meshes are opaque: per-particle alpha is ignored, so fade them by size.
- Meshes only visibly rotate with `spin(...)`; without it the tumble is invisible.
- Block debris uses the block's default state, untinted (grass and leaves read gray), drawn cutout
  (translucent blocks like glass look wrong), and always takes scene light.
- Item debris uses the ground-context model with item tints, but no enchantment glint.

### GPU backend

Off by default; `GpuSim.setEnabled(true)` opts a client in. Only specs the GPU can mirror exactly run
there, everything else silently uses the CPU simulation (which caps a system at 4000 particles):

- burst emission, additive blend, plain billboards - no mesh, lit, soft, or stretch
- no trails, sub-emitters, or collision
- at most 8 forces, all of gravity / drag / attractor / vortex, and at most 8 color stops
- the particle density option is ignored on the GPU path

Any GPU error logs once and drops the session back to the CPU path.

### SDF volumes

- 1 to 8 shapes per volume.
- Walking the camera inside a volume's bounding sphere can clip it against the near plane.

### HUD particles (`ScreenVfx`)

- Of the render spec, only blend, sprite, and flipbook animation apply; stretch, lit, soft, and mesh are
  world features and are ignored.
- Sub-emitter spawns are dropped; the HUD keeps one flat list, capped at 64 systems.
- Positions and sizes are gui-scaled units. Screen y grows downward.
- A ring shape samples the xz plane and flattens to a line on the HUD; use a sphere shell for a round burst.

### Bloom

Off by default; `PostFx.setEnabled(true)` opts a client in. Only Cascade's own effects feed the bloom
capture, so the sun, lamps, and other mods' rendering never glow. A broken post chain logs once and
disables itself.

### Sequencing and spawning

- Sub-emitters nest at most 4 deep; deeper specs stop spawning children.
- Effects beyond 96 blocks from the camera do not draw (they still tick).
- A frame budget of 60000 primitives draws nearest-first under heavy load and drops the far tail.
