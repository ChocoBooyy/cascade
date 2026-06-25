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
        for (int i = 0; i < children.size(); i++) {
            Step child = children.get(i);
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
        for (int i = 0; i < children.size(); i++) {
            if (!children.get(i).isDone()) {
                return false;
            }
        }
        return true;
    }
}
