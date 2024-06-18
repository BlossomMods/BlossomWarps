package dev.codedsakura.blossom.warps;

import dev.codedsakura.blossom.lib.config.BlossomConfig;
import dev.codedsakura.blossom.lib.teleport.TeleportConfig;

public class BlossomWarpsConfig extends BlossomConfig {
    TeleportConfig teleportation = new TeleportConfig.Builder()
            .setStandStill(3)
            .setCooldown(30)
            .build();

    @Deprecated
    int standStill = 3;
    @Deprecated
    int cooldown = 30;

    @Override
    protected int getLatestVersion() {
        return 1;
    }

    @Override
    public boolean update() {
        if (version == null) {
            teleportation = new TeleportConfig();
            teleportation.standStill = standStill;
            teleportation.cooldown = cooldown;
            return true;
        }
        return false;
    }
}
