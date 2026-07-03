# Forces and Steering

Forces are behaviors that run each tick and adjust a particle's velocity. Add as many as you like; they stack
in the order you add them.

```java
Vfx.emitter()
        .gravity(0f, -0.02f, 0f)
        .drag(0.05f)
        .curl(0.04f, 1.5f)
        .play(level, pos);
```

| Method | Effect |
| --- | --- |
| `gravity(x, y, z)` | constant acceleration |
| `drag(amount)` | slows particles toward rest |
| `turbulence(strength, frequency)` | chaotic noise jitter |
| `curl(strength, frequency)` | divergence-free curl noise, a smoother fluid-like flow than turbulence |
| `attractor(x, y, z, strength)` | pull toward a point; a negative strength pushes away |
| `vortex(x, y, z, strength)` | swirl around the vertical axis through a point |
| `flock(radius, separation, alignment, cohesion, maxSpeed)` | boids: neighbors separate, align headings, and cohere |

Attractor and vortex points are in emitter-local space, relative to the play position.

To add a force Cascade does not ship, write a [custom component](Custom-Components).
