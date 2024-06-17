package dev.codedsakura.blossom.warps;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.codedsakura.blossom.lib.data.ListDataController;
import dev.codedsakura.blossom.lib.polyfill.IdentifierPolyfill;
import dev.codedsakura.blossom.lib.teleport.TeleportUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class Warp {
    public String name;
    public UUID owner;
    public double x, y, z;
    public float yaw, pitch;
    public String world;
    public boolean global;

    Warp(String name, PlayerEntity owner, TeleportUtils.TeleportDestination destination) {
        this(name, owner, destination, false);
    }

    Warp(String name, PlayerEntity owner, TeleportUtils.TeleportDestination destination, boolean global) {
        this(
                name, destination.world.getRegistryKey().getValue().toString(), owner.getUuid(),
                destination.x, destination.y, destination.z,
                destination.yaw, destination.pitch,
                global
        );
    }

    Warp(String name, String world, UUID owner, double x, double y, double z, float yaw, float pitch, boolean global) {
        this.name = name;
        this.world = world;
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.global = global;
    }

    @Override
    public String toString() {
        return "Warp{" +
                "name='" + name + '\'' +
                ", world=" + world +
                ", owner=" + owner +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", global=" + global +
                '}';
    }

    TeleportUtils.TeleportDestination toDestination(MinecraftServer server) {
        return new TeleportUtils.TeleportDestination(
                server.getWorld(RegistryKey.of(RegistryKeys.WORLD, IdentifierPolyfill.of(this.world))),
                x, y, z, yaw, pitch
        );
    }
}

class WarpController extends ListDataController<Warp> implements SuggestionProvider<ServerCommandSource> {

    @Override
    public List<Warp> defaultData() {
        return new ArrayList<>();
    }

    @Override
    public String getFilename() {
        return "BlossomWarps";
    }

    @Override
    public Class<Warp[]> getArrayClassType() {
        return Warp[].class;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        String start = builder.getRemaining().toLowerCase();
        data.stream()
                .map(v -> v.name)
                .sorted(String::compareToIgnoreCase)
                .filter(pair -> pair.toLowerCase().startsWith(start))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    Warp findWarp(String name) {
        for (Warp warp : data) {
            if (warp.name.equals(name)) {
                return warp;
            }
        }
        return null;
    }

    boolean addWarp(Warp warp) {
        if (findWarp(warp.name) != null) {
            return false;
        }
        data.add(warp);
        write();
        return true;
    }

    List<Warp> getWarps() {
        return data;
    }

    boolean removeWarp(String name) {
        return data.removeIf(warp -> warp.name.equals(name));
    }
}
