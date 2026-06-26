package dev.chocoboy.cascade.testmod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("cascadetest")
public final class CascadeTestMod {

    public CascadeTestMod() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CascadeTestClient.init();
        }
    }
}
