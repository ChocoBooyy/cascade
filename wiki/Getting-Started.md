# Getting Started

## Add the dependency

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

Declare `cascade` as a required dependency in your `neoforge.mods.toml` or `fabric.mod.json` so it loads
first.

## Play an effect

Effects are played from server-side code and stream to nearby clients. The quickest call plays the built-in
burst:

```java
Vfx.burst(level, pos); // level is a ServerLevel, pos a Vec3
```

Most effects are built fluently and finished with `play`:

```java
Vfx.emitter()
        .shape(ShapeSpec.sphere(0.5f))
        .count(200).lifetime(40).speed(0.2f)
        .gradient(Easings.LINEAR, 0xFFCC33, 0xFF3300)
        .play(level, pos);
```

## Where to go next

- [Emitters](Emitters) for the full emitter builder
- [Forces and Steering](Forces-and-Steering) to add motion
- [Effects](Effects) for beams, domes, and volumes
- [Sequencing](Sequencing) to choreograph a timeline
- [Datapack Effects](Datapack-Effects) and [Custom Components](Custom-Components) to go further
