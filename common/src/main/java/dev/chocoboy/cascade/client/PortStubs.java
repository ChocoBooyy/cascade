package dev.chocoboy.cascade.client;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// some features ride vanilla apis that 26.1 rebuilt (post chains, gpu state, the gui renderer) and are not
// ported yet, so an effect that asks for one still fires but skips that piece. this warns once per feature
// so a spec author sees why, without spamming the log every frame. remove when the last stub is restored
final class PortStubs {

    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");
    private static final Set<String> WARNED = new HashSet<>();

    private PortStubs() {
    }

    static void warnOnce(String feature) {
        if (WARNED.add(feature)) {
            LOGGER.warn("Cascade {} is not yet ported to 26.1; the effect will skip it", feature);
        }
    }
}
