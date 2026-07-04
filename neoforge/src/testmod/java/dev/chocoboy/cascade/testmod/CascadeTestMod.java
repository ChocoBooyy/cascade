package dev.chocoboy.cascade.testmod;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod("cascadetest")
public final class CascadeTestMod {

    public CascadeTestMod() {
        CascadeDemos.registerContainComponent();
        NeoForge.EVENT_BUS.register(CascadeTestCommands.class);
    }
}
