package uk.co.finleyofthewoods.warpspeed;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.command.*;
import uk.co.finleyofthewoods.warpspeed.utils.DatabaseManager;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxRequestManager;

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
            /// Handle /tpa command and request a teleport from sender to receiver.
            TpxCommand.register(dispatcher);
            /// Handle /tpahere command and request a teleport from receiver to sender.
            /// Handle /tpaaccept command and accept a teleport from sender to receiver.
            /// Handle /tpadeny command and deny a teleport from sender to receiver.
            /// Handle /tpacancel command and cancel a teleport that sender has made.
            /// Handle /tpblock command and block the target from making requests to sender, with db manager
            /// Handle /tpunblock command and allow target to make requests to sender again, with db manager
            /// Handle /tphere command and teleport the receiver to sender without approval (needs elevated perms).
            /// Handle /tpAllhere command and teleport all players to sender without approval (needs elevated perms).
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
