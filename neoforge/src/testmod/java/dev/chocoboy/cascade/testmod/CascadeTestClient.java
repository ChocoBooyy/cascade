package dev.chocoboy.cascade.testmod;

import com.mojang.brigadier.Command;
import dev.chocoboy.cascade.neoforge.client.BeamEffect;
import dev.chocoboy.cascade.neoforge.client.ParticleBurstEffect;
import dev.chocoboy.cascade.neoforge.client.VfxRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class CascadeTestClient {

    private CascadeTestClient() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.register(CascadeTestClient.class);
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vfxtest")
                .then(Commands.literal("burst").executes(ctx -> {
                    Player player = Minecraft.getInstance().player;
                    if (player != null) {
                        VfxRenderManager.get().spawn(ParticleBurstEffect.burst(
                                player.position().add(0.0, 1.0, 0.0), player.level().getGameTime()));
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("beam").executes(ctx -> {
                    Player player = Minecraft.getInstance().player;
                    if (player != null) {
                        Vec3 from = player.getEyePosition();
                        Vec3 to = from.add(player.getLookAngle().scale(10.0));
                        VfxRenderManager.get().spawn(BeamEffect.bolt(from, to, player.level().getGameTime()));
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("vfxtest: try /vfxtest burst or /vfxtest beam"), false);
                    return Command.SINGLE_SUCCESS;
                }));
    }
}
