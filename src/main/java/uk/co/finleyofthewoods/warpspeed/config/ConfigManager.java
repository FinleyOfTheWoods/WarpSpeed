package uk.co.finleyofthewoods.warpspeed.config;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import static uk.co.finleyofthewoods.warpspeed.Warpspeed.MOD_ID;

public class ConfigManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final Gson GSON = new Gson();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("warpspeed/config.json").toFile();
    private static Config INSTANCE;

    public static Config get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    private static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                LOGGER.info("[{}] Loading config from file", MOD_ID);
                INSTANCE = GSON.fromJson(reader, Config.class);
            } catch (Exception e) {
                LOGGER.info("[{}] Failed to load config from file", MOD_ID, e);
                INSTANCE = new Config();
                save();
            }
        } else {
            LOGGER.info("[{}] Config file does not exist, creating new one", MOD_ID);
            INSTANCE = new Config();
            save();
        }
    }

    private static void save() {
        try {
            LOGGER.info("[{}] Saving config to file", MOD_ID);
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[{}] Unexpected error whilst saving config", MOD_ID, e);
        }
    }

    public static void setMaxPlayerHomes(int maxPlayerHomes) {
        try {
            INSTANCE.setPlayerHomeLimit(maxPlayerHomes);
            save();
        } catch (Exception e) {
            LOGGER.error("[{}] Unexpected error whilst saving config", MOD_ID, e);
            throw new RuntimeException("Unexpected error whilst saving config");
        }
    }

    public static void setMaxPlayerWarps(int maxPlayerWarps) {
        try {
            INSTANCE.setPlayerWarpLimit(maxPlayerWarps);
            save();
        } catch (Exception e) {
            LOGGER.error("[{}] Unexpected error whilst saving config", MOD_ID, e);
            throw new RuntimeException("Unexpected error whilst saving config");
        }
    }
}
