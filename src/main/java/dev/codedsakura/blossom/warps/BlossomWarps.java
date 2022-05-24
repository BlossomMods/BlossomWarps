package dev.codedsakura.blossom.warps;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.codedsakura.blossom.lib.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import org.apache.logging.log4j.core.Logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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


        BlossomLib.addCommand(literal("warps")
                .requires(Permissions.require("blossom.warps.warps", true))
                .executes(this::listWarpsAll)
                .then(literal("list")
                        .executes(this::listWarpsAll)
                        .then(argument("dimension", DimensionArgumentType.dimension())
                                .executes(this::listWarpsDim))));
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


    MutableText listWarpsConcatenate(List<Warp> warps, String world) {
        LOGGER.debug("concatenating {} warps (of dim {})", warps.size(), world);
        AtomicBoolean pastFirst = new AtomicBoolean(false);
        MutableText result = TextUtils.translation("blossom.warps.list.dimension.header", world);
        warps.stream()
                .filter(warp -> Objects.equals(warp.world, world))
                .forEach(warp -> {
                    if (pastFirst.getAndSet(true)) {
                        result.append("\n");
                    }
                    result.append(TextUtils.translation("blossom.warps.list.item.before"));
                    result.append(TextUtils.translation("blossom.warps.list.item", warp.name)
                            .styled(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/warp " + warp.name))
                                    .withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            TextUtils.translation(
                                                    "blossom.warps.list.item.description",
                                                    warp.name,
                                                    warp.owner,
                                                    warp.world,
                                                    String.format("%.2f", warp.x),
                                                    String.format("%.2f", warp.y),
                                                    String.format("%.2f", warp.z),
                                                    String.format("%.2f", warp.yaw),
                                                    String.format("%.2f", warp.pitch)
                                            )))));
                    result.append(TextUtils.translation("blossom.warps.list.item.after"));
                });
        return result;
    }

    private int listWarpsAll(CommandContext<ServerCommandSource> ctx) {
        AtomicBoolean pastFirst = new AtomicBoolean(false);
        List<Warp> warps = warpController.getWarps();
        MutableText result = TextUtils.translation("blossom.warps.list.all.header");
        warps.stream()
                .map(warp -> warp.world)
                .distinct()
                .forEach(world -> {
                    if (pastFirst.getAndSet(true)) {
                        result.append("\n");
                    }
                    result.append(listWarpsConcatenate(warps, world));
                });
        ctx.getSource().sendFeedback(result, false);
        return Command.SINGLE_SUCCESS;
    }

    private int listWarpsDim(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ctx.getSource().sendFeedback(
                listWarpsConcatenate(
                        warpController.getWarps(),
                        DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue().toString()
                ),
                false
        );
        return Command.SINGLE_SUCCESS;
    }
}
