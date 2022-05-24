package dev.codedsakura.blossom.warps;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.codedsakura.blossom.lib.BlossomConfig;
import dev.codedsakura.blossom.lib.BlossomLib;
import dev.codedsakura.blossom.lib.CustomLogger;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.core.Logger;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BlossomWarps implements ModInitializer {
    static BlossomWarpsConfig CONFIG = BlossomConfig.load(BlossomWarpsConfig.class, "BlossomWarps.json");
    public static final Logger LOGGER = CustomLogger.createLogger("BlossomWarps");

    @Override
    public void onInitialize() {
        BlossomLib.addCommand(literal("warp")
                .then(argument("warp", StringArgumentType.string())));
    }
}
