package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.engine.seq.Timeline;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// advances scheduled timelines. loader-agnostic: each loader calls tick once per server tick
public final class VfxSequencer {

    private static final VfxSequencer INSTANCE = new VfxSequencer();
    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");

    private final List<Timeline> active = new ArrayList<>();
    private final List<Timeline> incoming = new ArrayList<>();
    private boolean ticking;
    private boolean loggedError;

    private VfxSequencer() {
    }

    public static VfxSequencer get() {
        return INSTANCE;
    }

    // a run step may start another sequence while tick is mid-iteration; buffering keeps the
    // active list unmodified under removeIf. buffered timelines begin on the next tick
    public void schedule(Timeline timeline) {
        if (ticking) {
            incoming.add(timeline);
        } else {
            active.add(timeline);
        }
    }

    public void tick() {
        ticking = true;
        try {
            active.removeIf(timeline -> {
                try {
                    return timeline.tick();
                } catch (RuntimeException e) {
                    logOnce(e);
                    return true;
                }
            });
        } finally {
            ticking = false;
            active.addAll(incoming);
            incoming.clear();
        }
    }

    private void logOnce(RuntimeException e) {
        if (!loggedError) {
            loggedError = true;
            LOGGER.error("Cascade sequence failed and was dropped, further errors suppressed", e);
        }
    }
}
