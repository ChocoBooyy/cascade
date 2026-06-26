package dev.chocoboy.cascade.neoforge;

import dev.chocoboy.cascade.CascadeCommon;
import dev.chocoboy.cascade.neoforge.client.CascadeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(CascadeCommon.MOD_ID)
public final class CascadeNeoForge {

    public CascadeNeoForge() {
        CascadeCommon.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CascadeClient.init();
        }
    }
}
