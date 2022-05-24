package dev.codedsakura.blossom.warps;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.codedsakura.blossom.lib.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.core.Logger;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BlossomWarps implements ModInitializer {
    static BlossomWarpsConfig CONFIG = BlossomConfig.load(BlossomWarpsConfig.class, "BlossomWarps.json");
    public static final Logger LOGGER = CustomLogger.createLogger("BlossomWarps");
    static WarpController warpController;

    @Override
    public void onInitialize() {
        warpController = new WarpController();

        BlossomLib.addCommand(literal("warp")
                .requires(Permissions.require("blossom.warps.warp", true))
                .then(argument("warp", StringArgumentType.string())
                        .suggests(warpController)
                        .executes(ctx -> this.warpPlayer(ctx, ctx.getSource().getPlayer()))

                        .then(argument("who", EntityArgumentType.player())
                                .requires(Permissions.require("blossom.warps.warp.others", 2))
                                .executes(ctx -> this.warpPlayer(ctx, EntityArgumentType.getPlayer(ctx, "who"))))));
    }


    private int warpPlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        String warpName = StringArgumentType.getString(ctx, "warp");
        Warp warp = warpController.findWarp(warpName);
        LOGGER.info("warp player [{}] to {}", player.getUuid(), warp);
        if (warp != null) {
            TeleportUtils.teleport(
                    CONFIG.teleportation,
                    CONFIG.standStill,
                    CONFIG.cooldown,
                    BlossomWarps.class,
                    player,
                    () -> warp.toDestination(ctx.getSource().getServer())
            );
        }
        return Command.SINGLE_SUCCESS;
    }
}
