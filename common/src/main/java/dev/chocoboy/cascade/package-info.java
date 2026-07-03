/**
 * Cascade's public API: a fluent, server-side way to play visual effects that render on every nearby client.
 *
 * <p>Everything starts at {@link dev.chocoboy.cascade.Vfx}. Build an effect with the fluent builders and play
 * it at a point in a level:
 *
 * <pre>{@code
 * Vfx.emitter()
 *     .shape(ShapeSpec.sphere(0.5f))
 *     .count(200).lifetime(40).speed(0.2f)
 *     .gradient(Easings.LINEAR, 0xFFCC33, 0xFF3300)
 *     .play(level, pos);
 * }</pre>
 *
 * <p>Playing an effect is a fire-and-forget send: the server serializes the spec and streams it to players in
 * range, where the client owns the simulation and rendering. Nothing spawns as an entity and nothing needs
 * saving.
 *
 * <p>Effects can also be authored as datapack JSON under {@code data/<namespace>/cascade/effects/} and played
 * by id with {@link dev.chocoboy.cascade.Vfx#play}. Custom particle behaviors register through
 * {@link dev.chocoboy.cascade.Vfx#registerComponent} and implement
 * {@link dev.chocoboy.cascade.engine.effect.ComponentSpec}.
 */
package dev.chocoboy.cascade;
