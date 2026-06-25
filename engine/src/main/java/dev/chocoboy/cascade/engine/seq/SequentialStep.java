package dev.chocoboy.cascade.engine.seq;

import java.util.List;

public final class SequentialStep implements Step {

    private final List<Step> children;
    private int index;

    public SequentialStep(List<Step> children) {
        this.children = List.copyOf(children);
    }

    @Override
    public boolean tick() {
        while (index < children.size()) {
            Step child = children.get(index);
            if (child.isDone()) {
                index++;
                continue;
            }
            boolean finished = child.tick();
            if (!child.consumesTick()) {
                // instantaneous: move on within the same tick
                if (finished) {
                    index++;
                }
                continue;
            }
            if (finished) {
                index++;
                drainInstant();
            }
            return index >= children.size();
        }
        return true;
    }

    // fire any trailing instantaneous steps that should land on the tick a timed step finished
    private void drainInstant() {
        while (index < children.size()) {
            Step child = children.get(index);
            if (child.isDone()) {
                index++;
                continue;
            }
            if (child.consumesTick()) {
                return;
            }
            child.tick();
            index++;
        }
    }

    @Override
    public boolean isDone() {
        return index >= children.size();
    }
}
