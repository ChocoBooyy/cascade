package dev.chocoboy.cascade.testmod;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod("cascadetest")
public final class CascadeTestMod {

    public CascadeTestMod() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vfxtest")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("Cascade harness loaded. Effects land in the next milestone."),
                            false);
                    return Command.SINGLE_SUCCESS;
                }));
    }
}
