package dev.chocoboy.cascade;

import dev.chocoboy.cascade.engine.effect.EffectSpec;
import dev.chocoboy.cascade.net.EffectJson;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// loads datapack-authored effects from data/<namespace>/cascade/effects/*.json into a named registry,
// so Vfx.play can fire an effect by id. Server side, reloaded with the rest of the datapacks. Each loader
// registers an instance with its own reload-listener hook
public class CascadeEffects extends SimpleJsonResourceReloadListener<EffectSpec> {

    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");
    private static final Map<Identifier, EffectSpec> EFFECTS = new HashMap<>();

    public CascadeEffects() {
        // 26.1 decodes json with a Codec at prepare time, so apply receives already-parsed specs
        super(EffectJson.EFFECT, FileToIdConverter.json("cascade/effects"));
    }

    public static EffectSpec get(Identifier id) {
        return EFFECTS.get(id);
    }

    @Override
    protected void apply(Map<Identifier, EffectSpec> effects, ResourceManager manager, ProfilerFiller profiler) {
        EFFECTS.clear();
        EFFECTS.putAll(effects);
        LOGGER.info("Cascade loaded {} effect(s)", EFFECTS.size());
    }
}
