package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.engine.seq.Timeline;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
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
