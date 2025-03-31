package dev.codedsakura.blossom.warps;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.codedsakura.blossom.lib.BlossomLib;
import dev.codedsakura.blossom.lib.config.BlossomConfig;
import dev.codedsakura.blossom.lib.permissions.Permissions;
import dev.codedsakura.blossom.lib.teleport.TeleportUtils;
import dev.codedsakura.blossom.lib.text.DimName;
import dev.codedsakura.blossom.lib.text.TextSuperJoiner;
import dev.codedsakura.blossom.lib.text.TextUtils;
import dev.codedsakura.blossom.lib.utils.CustomLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.core.Logger;

import java.io.File;
import java.util.List;
import java.util.Optional;

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
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        return this.warpPlayer(ctx, player);
                    })
                        .then(argument("who", EntityArgumentType.player())
                                .requires(Permissions.require("blossom.warps.warp.others", 2))
                                .executes(ctx -> this.warpPlayer(ctx, EntityArgumentType.getPlayer(ctx, "who"))))));


        BlossomLib.addCommand(literal("warps")
                .requires(Permissions.require("blossom.warps.warps", true))
                .executes(this::listWarpsAll)
                .then(literal("list")
                        .executes(this::listWarpsAll)
                        .then(argument("dimension", DimensionArgumentType.dimension())
                                .executes(this::listWarpsDim)))

                .then(literal("add")
                        .requires(Permissions.require("blossom.warps.add", 2))
                        .then(argument("name", StringArgumentType.string())
                                .executes(this::addWarpPlayerPos)
                                .then(argument("position", Vec3ArgumentType.vec3(true))
                                        .then(argument("rotation", RotationArgumentType.rotation())
                                                .executes(this::addWarpPosRot)
                                                .then(argument("dimension", DimensionArgumentType.dimension())
                                                        .executes(this::addWarpDimension))))))
                .then(literal("add-global")
                        .requires(Permissions.require("blossom.warps.add.global", 2))
                        .then(argument("name", StringArgumentType.string())
                                .executes(this::addGlobalWarpPlayerPos)
                                .then(argument("position", Vec3ArgumentType.vec3(true))
                                        .then(argument("rotation", RotationArgumentType.rotation())
                                                .executes(this::addGlobalWarpPosRot)
                                                .then(argument("dimension", DimensionArgumentType.dimension())
                                                        .executes(this::addGlobalWarpDimension))))))

                .then(literal("remove")
                        .requires(Permissions.require("blossom.warps.remove", 2))
                        .then(argument("warp", StringArgumentType.string())
                                .suggests(warpController)
                                .executes(this::removeWarp)))

                .then(literal("load-legacy")
                        .requires(Permissions.require("blossom.warps.load-legacy", 4))
                        .executes(this::loadLegacyDefault)
                        .then(argument("overwrite", BoolArgumentType.bool())
                                .executes(this::loadLegacyArgument))));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
            LOGGER.debug(warpController.getWarps());
            Optional.ofNullable(warpController.getWarps())
                    .orElse(List.of())
                    .stream()
                    .filter(v -> v.global)
                    .map(v -> v.name)
                    .forEach(warpName -> dispatcher
                            .register(literal(warpName)
                                    .requires(Permissions.require("blossom.warps.global." + warpName, true))
                                    .executes(ctx -> warpToName(ctx, warpName))));
        });
    }


    private int warpPlayerToName(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, String warpName) {
        Warp warp = warpController.findWarp(warpName);
        LOGGER.debug("warp player [{}] to global {}", player.getUuid(), warp);

        if (warp == null) {
            TextUtils.sendErr(ctx, "blossom.warps.not-found", warpName);
        } else {
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

    private int warpPlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        String warpName = StringArgumentType.getString(ctx, "warp");
        return warpPlayerToName(ctx, player, warpName);
    }

    private int warpToName(CommandContext<ServerCommandSource> ctx, String warpName) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        return warpPlayerToName(ctx, player, warpName);
    }


    MutableText listWarpsConcatenate(String world) {
        MutableText result = warpController.getWarps()
                .stream()
                .filter(warp -> warp.world.equals(world))
                .map(warp -> TextUtils.translation("blossom.warps.list.item", warp.name)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent.SuggestCommand("/warp " + warp.name))
                                .withHoverEvent(new HoverEvent.ShowText(
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
        List<Warp> warps = warpController.getWarps();
        if (warps.size() == 0) {
            TextUtils.sendErr(ctx, "blossom.warps.list.all.empty");
            return Command.SINGLE_SUCCESS;
        }

        MutableText result = warps.stream()
                .map(warp -> warp.world)
                .distinct()
                .map(this::listWarpsConcatenate)
                .collect(TextSuperJoiner.joiner(TextUtils.translation("blossom.warps.list.all.join")));

        ctx.getSource().sendFeedback(() -> TextUtils.translation("blossom.warps.list.all.header").append(result), false);
        return Command.SINGLE_SUCCESS;
    }

    private int listWarpsDim(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String dimension = DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue().toString();
        if (warpController.getWarps().stream().noneMatch(warp -> warp.world.equals(dimension))) {
            TextUtils.sendErr(ctx, "blossom.warps.list.dimension.empty", dimension);
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().sendFeedback(() ->
                listWarpsConcatenate(dimension),
                false
        );
        return Command.SINGLE_SUCCESS;
    }


    private int addWarp(CommandContext<ServerCommandSource> ctx, Warp warp) {
        LOGGER.info("adding warp {}", warp);
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
        Vec3d position = Vec3ArgumentType.getPosArgument(ctx, "position").getPos(ctx.getSource());
        Vec2f rotation = RotationArgumentType.getRotation(ctx, "rotation").getRotation(ctx.getSource());
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
        LOGGER.info("removing warp {}", warpController.findWarp(warpName));
        boolean result = warpController.removeWarp(warpName);
        if (result) {
            TextUtils.sendWarnOps(ctx, "blossom.warps.remove", warpName);
        } else {
            TextUtils.sendErr(ctx, "blossom.warps.remove.failed", warpName);
        }
        return Command.SINGLE_SUCCESS;
    }


    private int loadLegacy(CommandContext<ServerCommandSource> ctx, boolean overwrite) {
        TextUtils.sendOps(ctx, "blossom.homes.load-legacy.info");

        if (overwrite) {
            TextUtils.sendOps(ctx, "blossom.homes.load-legacy.overwrite");
        }

        MinecraftServer server = ctx.getSource().getServer();

        File[] playerDataFiles = server.getSavePath(WorldSavePath.LEVEL_DAT).toFile().listFiles();

        int totalHomes = 0, totalPlayers = 0;

//        try {
//            assert playerDataFiles != null;
//            for (File playerDataFile : playerDataFiles) {
//                NbtCompound data = NbtIo.readCompressed(playerDataFile);
//
//                if (!data.contains("cardinal_components")) {
//                    continue;
//                }
//                data = data.getCompound("cardinal_components");
//
//                if (!data.contains("fabrichomes:homes")) {
//                    continue;
//                }
//                var homes = data.getCompound("fabrichomes:homes")
//                        .getList("homes", NbtElement.COMPOUND_TYPE)
//                        .stream()
//                        .map(home -> {
//                            String name = ((NbtCompound) home).getString("name");
//                            String world = ((NbtCompound) home).getString("dim");
//                            double x = ((NbtCompound) home).getFloat("x");
//                            double y = ((NbtCompound) home).getFloat("y");
//                            double z = ((NbtCompound) home).getFloat("z");
//                            float yaw = ((NbtCompound) home).getFloat("yaw");
//                            float pitch = ((NbtCompound) home).getFloat("pitch");
//                            return new Home(name, world, x, y, z, yaw, pitch);
//                        })
//                        .toList();
//
//                UUID uuid = UUID.fromString(FilenameUtils.removeExtension(playerDataFile.getName()));
//
//                totalPlayers++;
//                totalHomes += homes.size();
//
//                homeController.appendHomes(uuid, homes, overwrite);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }

        TextUtils.sendOps(ctx, "blossom.homes.load-legacy.done", totalHomes, totalPlayers);
        return Command.SINGLE_SUCCESS;
    }

    private int loadLegacyArgument(CommandContext<ServerCommandSource> ctx) {
        boolean overwrite = BoolArgumentType.getBool(ctx, "overwrite");

        return loadLegacy(ctx, overwrite);
    }

    private int loadLegacyDefault(CommandContext<ServerCommandSource> ctx) {
        return loadLegacy(ctx, false);
    }
}
