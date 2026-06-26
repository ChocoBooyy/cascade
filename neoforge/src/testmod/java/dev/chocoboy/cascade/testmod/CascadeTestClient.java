package dev.chocoboy.cascade.testmod;

import com.mojang.brigadier.Command;
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
                .then(Commands.literal("quad").executes(ctx -> {
                    Player player = Minecraft.getInstance().player;
                    if (player != null) {
                        Vec3 pos = player.position().add(0.0, 1.0, 0.0);
                        VfxRenderManager.get().addQuad(pos, 0x66CCFF, 0.5f, 60);
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("vfxtest: try /vfxtest quad"), false);
                    return Command.SINGLE_SUCCESS;
                }));
    }
}
