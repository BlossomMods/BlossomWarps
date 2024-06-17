package dev.codedsakura.blossom.warps;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.codedsakura.blossom.lib.mod.BlossomMod;
import dev.codedsakura.blossom.lib.permissions.Permissions;
import dev.codedsakura.blossom.lib.teleport.TeleportUtils;
import dev.codedsakura.blossom.lib.text.DimName;
import dev.codedsakura.blossom.lib.text.TextSuperJoiner;
import dev.codedsakura.blossom.lib.text.TextUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BlossomWarps extends BlossomMod<BlossomWarpsConfig> implements ModInitializer {
    static WarpController warpController;

    @Override
    public String getName() {
        return "BlossomWarps";
    }

    public BlossomWarps() {
        super(BlossomWarpsConfig.class);
    }

    @Override
    public void onInitialize() {
        warpController = new WarpController();
        this.register();

        addCommand(literal("warp")
                .requires(Permissions.require("blossom.warps.command.warp", true))
                .then(argument("warp", StringArgumentType.string())
                    .suggests(warpController)
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        return this.warpPlayer(ctx, player);
                    })
                        .then(argument("who", EntityArgumentType.player())
                                .requires(Permissions.require("blossom.warps.command.warp.others", 2))
                                .executes(ctx -> this.warpPlayer(ctx, EntityArgumentType.getPlayer(ctx, "who"))))));


        addCommand(literal("warps")
                .requires(Permissions.require("blossom.warps.command.warps", true))
                .executes(this::listWarpsAll)
                .then(literal("list")
                        .executes(this::listWarpsAll)
                        .then(argument("dimension", DimensionArgumentType.dimension())
                                .executes(this::listWarpsDim)))

                .then(literal("add")
                        .requires(Permissions.require("blossom.warps.command.warps.add", 2))
                        .then(argument("name", StringArgumentType.string())
                                .executes(this::addWarpPlayerPos)
                                .then(argument("position", Vec3ArgumentType.vec3(true))
                                        .then(argument("rotation", RotationArgumentType.rotation())
                                                .executes(this::addWarpPosRot)
                                                .then(argument("dimension", DimensionArgumentType.dimension())
                                                        .executes(this::addWarpDimension))))))
                .then(literal("add-global")
                        .requires(Permissions.require("blossom.warps.command.warps.add-global", 2))
                        .then(argument("name", StringArgumentType.string())
                                .executes(this::addGlobalWarpPlayerPos)
                                .then(argument("position", Vec3ArgumentType.vec3(true))
                                        .then(argument("rotation", RotationArgumentType.rotation())
                                                .executes(this::addGlobalWarpPosRot)
                                                .then(argument("dimension", DimensionArgumentType.dimension())
                                                        .executes(this::addGlobalWarpDimension))))))

                .then(literal("remove")
                        .requires(Permissions.require("blossom.warps.command.warps.remove", 2))
                        .then(argument("warp", StringArgumentType.string())
                                .suggests(warpController)
                                .executes(this::removeWarp))));

        Optional.ofNullable(warpController.getWarps(null))
                .orElse(List.of())
                .stream()
                .filter(v -> v.global)
                .map(v -> v.name)
                .forEach(warpName -> addCommand(literal(warpName)
                        .requires(Permissions.require("blossom.warps.global-warp.%s".formatted(warpName), true))
                        .executes(ctx -> warpToName(ctx, warpName))));
    }


    private int warpPlayerToName(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, String warpName) {
        Warp warp = warpController.findWarp(ctx.getSource().getPlayer(), warpName);
        logger.debug("warp player [{}] to {}", player.getUuid(), warp);

        if (warp == null) {
            TextUtils.sendErr(ctx, "blossom.warps.not-found", warpName);
        } else {
            TeleportUtils.teleport(
                    config.teleportation,
                    BlossomWarps.class,
                    player,
                    () -> warp.toDestination(ctx.getSource().getServer())
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private int warpPlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        String warpName = StringArgumentType.getString(ctx, "warp");
        return warpPlayerToName(ctx, player, warpName);
    }

    private int warpToName(CommandContext<ServerCommandSource> ctx, String warpName) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        return warpPlayerToName(ctx, player, warpName);
    }


    MutableText listWarpsConcatenate(ServerPlayerEntity player, String world) {
        MutableText result = warpController.getWarps(player)
                .stream()
                .filter(warp -> warp.world.equals(world))
                .map(warp -> TextUtils.translation("blossom.warps.list.item", warp.name)
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
                                        )))))
                .collect(TextSuperJoiner.collector(
                        TextUtils.translation("blossom.warps.list.item.before"),
                        TextUtils.translation("blossom.warps.list.item.after"),
                        TextUtils.translation("blossom.warps.list.item.join")
                ));
        MutableText copy = TextUtils.translation("blossom.warps.list.header", DimName.get(world)).copy();
        return copy.append(result);
    }

    private int listWarpsAll(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        List<Warp> warps = warpController.getWarps(player);
        if (warps.size() == 0) {
            TextUtils.sendErr(ctx, "blossom.warps.list.all.empty");
            return Command.SINGLE_SUCCESS;
        }

        MutableText result = warps.stream()
                .map(warp -> warp.world)
                .distinct()
                .map((String world) -> listWarpsConcatenate(player, world))
                .collect(TextSuperJoiner.joiner(TextUtils.translation("blossom.warps.list.all.join")));

        TextUtils.sendRaw(ctx, TextUtils.translation("blossom.warps.list.all.header").append(result));
        return Command.SINGLE_SUCCESS;
    }

    private int listWarpsDim(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String dimension = DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue().toString();
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (warpController.getWarps(player).stream().noneMatch(warp -> warp.world.equals(dimension))) {
            TextUtils.sendErr(ctx, "blossom.warps.list.dimension.empty", dimension);
            return Command.SINGLE_SUCCESS;
        }

        TextUtils.sendRaw(ctx, listWarpsConcatenate(player, dimension));
        return Command.SINGLE_SUCCESS;
    }


    private int addWarp(CommandContext<ServerCommandSource> ctx, Warp warp) {
        logger.info("adding warp {}", warp);
        boolean result = warpController.addWarp(warp);
        if (result) {
            TextUtils.sendSuccessOps(ctx, "blossom.warps.add", warp.name);
        } else {
            TextUtils.sendErr(ctx, "blossom.warps.add.failed", warp.name);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int addWarpPlayerPos(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "name");
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        return addWarp(ctx, new Warp(
            name, player,
            new TeleportUtils.TeleportDestination(player)
        ));
    }

    private int addWarpPosRotDim(CommandContext<ServerCommandSource> ctx, ServerWorld dimension, boolean global) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "name");
        Vec3d position = Vec3ArgumentType.getPosArgument(ctx, "position").toAbsolutePos(ctx.getSource());
        Vec2f rotation = RotationArgumentType.getRotation(ctx, "rotation").toAbsoluteRotation(ctx.getSource());
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        return addWarp(ctx, new Warp(
                name, player,
                new TeleportUtils.TeleportDestination(
                        dimension,
                        position,
                        rotation.x,
                        rotation.y
                ),
                global
        ));
    }

    private int addWarpPosRot(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return addWarpPosRotDim(ctx, ctx.getSource().getWorld(), false);
    }

    private int addWarpDimension(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerWorld dimension = DimensionArgumentType.getDimensionArgument(ctx, "dimension");
        return addWarpPosRotDim(ctx, dimension, false);
    }

    private int addGlobalWarpPlayerPos(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "name");
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        return addWarp(ctx, new Warp(
                name, player,
                new TeleportUtils.TeleportDestination(player),
                true
        ));
    }

    private int addGlobalWarpPosRot(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return addWarpPosRotDim(ctx, ctx.getSource().getWorld(), true);
    }

    private int addGlobalWarpDimension(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerWorld dimension = DimensionArgumentType.getDimensionArgument(ctx, "dimension");
        return addWarpPosRotDim(ctx, dimension, true);
    }


    private int removeWarp(CommandContext<ServerCommandSource> ctx) {
        String warpName = StringArgumentType.getString(ctx, "warp");
        logger.info("removing warp {}", warpController.findWarp(null, warpName));
        boolean result = warpController.removeWarp(warpName);
        if (result) {
            TextUtils.sendWarnOps(ctx, "blossom.warps.remove", warpName);
        } else {
            TextUtils.sendErr(ctx, "blossom.warps.remove.failed", warpName);
        }
        return Command.SINGLE_SUCCESS;
    }
}
