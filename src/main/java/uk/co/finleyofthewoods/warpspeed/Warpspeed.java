package uk.co.finleyofthewoods.warpspeed;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import uk.co.finleyofthewoods.warpspeed.command.HomeCommand;
import uk.co.finleyofthewoods.warpspeed.command.SpawnCommand;
import uk.co.finleyofthewoods.warpspeed.command.WarpCommand;
import uk.co.finleyofthewoods.warpspeed.manager.impl.DatabaseManagerImpl;

@Slf4j
public class Warpspeed implements ModInitializer {
    private static final String MOD_ID = "warpspeed";
    private static final ModContainer container = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
    public static final String MOD_NAME = container.getMetadata().getName();
    private static final String VERSION = container.getMetadata().getVersion().getFriendlyString();

    private static final DatabaseManagerImpl databaseManager = new DatabaseManagerImpl();

    @Override
    @SuppressWarnings("unused")
    public void onInitialize() {
        log.info("Initialising {} v{}", MOD_NAME, VERSION);

        try {
            databaseManager.initialise();
            CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
                log.info("Registering {} commands", MOD_NAME);
                HomeCommand.register(dispatcher);
                SpawnCommand.register(dispatcher);
                WarpCommand.register(dispatcher);
            });
        }
        catch (Exception e) {
            log.error("Failed to start {} v{}", MOD_NAME, VERSION, e);
            log.error("{} v{} not enabled", MOD_NAME, VERSION);
        }
    }
}
