package dev.chocoboy.cascade.engine.seq;

import java.util.Objects;

public final class Timeline {

    private final Step root;
    private boolean done;

    public Timeline(Step root) {
        this.root = Objects.requireNonNull(root, "root");
        this.done = root.isDone();
    }

    public boolean tick() {
        if (done) {
            return true;
        }
        done = root.tick();
        return done;
    }

    public boolean isDone() {
        return done;
    }
}
