package dev.chocoboy.cascade.neoforge;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.chocoboy.cascade.engine.effect.EffectSpec;
import dev.chocoboy.cascade.neoforge.net.EffectJson;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// loads datapack-authored effects from data/<namespace>/cascade/effects/*.json into a named registry,
// so Vfx.play can fire an effect by id. Server side, reloaded with the rest of the datapacks. Each loader
// registers an instance with its own reload-listener hook
public class CascadeEffects extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");
    private static final Map<ResourceLocation, EffectSpec> EFFECTS = new HashMap<>();

    public CascadeEffects() {
        super(GSON, "cascade/effects");
    }

    public static EffectSpec get(ResourceLocation id) {
        return EFFECTS.get(id);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        EFFECTS.clear();
        files.forEach((id, json) -> EffectJson.EFFECT.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> LOGGER.error("Cascade effect {} failed to load: {}", id, error))
                .ifPresent(spec -> EFFECTS.put(id, spec)));
        LOGGER.info("Cascade loaded {} effect(s)", EFFECTS.size());
    }
}
