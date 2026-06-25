package dev.chocoboy.cascade.engine.seq;

import java.util.List;

public final class ParallelStep implements Step {

    private final List<Step> children;

    public ParallelStep(List<Step> children) {
        this.children = List.copyOf(children);
    }

    @Override
    public boolean tick() {
        boolean allDone = true;
        for (Step child : children) {
            if (!child.isDone()) {
                child.tick();
                if (!child.isDone()) {
                    allDone = false;
                }
            }
        }
        return allDone;
    }

    @Override
    public boolean isDone() {
        for (Step child : children) {
            if (!child.isDone()) {
                return false;
            }
        }
        return true;
    }
}
