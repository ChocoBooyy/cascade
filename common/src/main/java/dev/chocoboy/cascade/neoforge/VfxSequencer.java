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
    private boolean loggedError;

    private VfxSequencer() {
    }

    public static VfxSequencer get() {
        return INSTANCE;
    }

    public void schedule(Timeline timeline) {
        active.add(timeline);
    }

    public void tick() {
        active.removeIf(timeline -> {
            try {
                return timeline.tick();
            } catch (RuntimeException e) {
                logOnce(e);
                return true;
            }
        });
    }

    private void logOnce(RuntimeException e) {
        if (!loggedError) {
            loggedError = true;
            LOGGER.error("Cascade sequence failed and was dropped, further errors suppressed", e);
        }
    }
}
