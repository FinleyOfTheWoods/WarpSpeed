package uk.co.finleyofthewoods.warpspeed;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.command.*;
import uk.co.finleyofthewoods.warpspeed.utils.DatabaseManager;
import uk.co.finleyofthewoods.warpspeed.utils.TpxRequestManager;

public class Warpspeed implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(Warpspeed.class);

    private static DatabaseManager dbManager;

    @Override
    public void onInitialize() {
        LOGGER.info("Initialising Warpspeed mod");

        dbManager = new DatabaseManager();
        dbManager.initialise();

        ///  Close the database when the server stops
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LOGGER.info("Closing database...");
            dbManager.close();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> TpxRequestManager.tick());

        /// Register commands
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            /// Handle /spawn command and teleport the player to the world spawn location.
            SpawnCommand.register(dispatcher);
            /// Handle /home command and teleport the player to the home location.
            HomeCommand.register(dispatcher, dbManager);
            /// Handle /back command and teleport the player to the previous location.
            BackCommand.register(dispatcher);
            /// Handle /warp command and teleport the player to the warp location.
            WarpCommand.register(dispatcher, dbManager);
            /// Handle all teleport-related commands.
            TpxCommand.register(dispatcher, dbManager);
            /// Handle /rtp command and teleport the player to a random location
            RtpCommand.register(dispatcher);
        }));
    }
    public static DatabaseManager getDatabaseManager() {
        if (dbManager == null) {
            LOGGER.warn("Database manager accessed before initialization, creating new instance");
            dbManager = new DatabaseManager();
            dbManager.initialise();
        }
        return dbManager;
    }
}
