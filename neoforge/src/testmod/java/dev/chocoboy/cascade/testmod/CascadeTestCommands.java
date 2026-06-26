package dev.chocoboy.cascade.testmod;

import com.mojang.brigadier.Command;
import dev.chocoboy.cascade.neoforge.Vfx;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CascadeTestCommands {

    private CascadeTestCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vfxtest")
                .then(Commands.literal("burst").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Vfx.burst(player.serverLevel(), player.position().add(0.0, 1.0, 0.0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("beam").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Vec3 from = player.getEyePosition();
                    Vec3 to = from.add(player.getLookAngle().scale(10.0));
                    Vfx.beam(player.serverLevel(), from, to);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
