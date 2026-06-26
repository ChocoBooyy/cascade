package dev.chocoboy.cascade.engine.effect;

public interface EffectSim {

    // advance one tick; return true once the effect has finished
    boolean tick();

    boolean isDone();
}
