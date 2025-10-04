package uk.co.finleyofthewoods.warpspeed;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.command.SpawnCommand;


import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Warpspeed implements ModInitializer {
    public static final String MOD_ID = "warpspeed";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initialising Warpspeed mod");
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            /// Handle /spawn command and teleport the player to the world spawn location.
            SpawnCommand.register(dispatcher);

            registerCommand(dispatcher);
        }));
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        /// Handle /home command and teleport the player to the home location.
        dispatcher.register(literal("home").requires(source -> source.getPlayer() != null)
            .executes(context -> 0)
            .then(argument("name", StringArgumentType.string())
            .executes(context -> {
                try {
                    executeTeleportToHome(context);
                } catch (Exception e) {
                    handleException(e);
                }
                return 0;
            }))
        );
        /// Handle /addHome [name] command and add the player's current location as a home.
        dispatcher.register(literal("addHome").requires(source -> source.getPlayer() != null)
            .executes(context -> 0)
            .then(argument("name", StringArgumentType.string())
            .executes(context -> {
                try {
                    executeAddHome(context);
                } catch (Exception e) {
                    handleException(e);
                }
                return 0;
            }))
        );
        /// Handle /removeHome [name] command and remove the named home from the player's list of homes.
        dispatcher.register(literal("removeHome").requires(source -> source.getPlayer() != null)
            .executes(context -> 0)
            .then(argument("name", StringArgumentType.string()).requires(source -> source.getPlayer() != null)
            .executes(context -> {
                try {
                    executeRemoveHome(context);
                } catch (Exception e) {
                    handleException(e);
                }
                return 0;
            }))
        );
    }

    private void executeTeleportToSpawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        player.sendMessage(Text.literal("Teleporting to spawn..."), false);
        World world = player.getEntityWorld();
        BlockPos spawnPoint = world.getSpawnPoint().getPos();
        try {
            LOGGER.debug("Teleporting player {} to spawn. Pos: {}, {}, {},", player.getName().getString(), spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ());
            boolean teleported = player.teleport(spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ(), true);
            if (!teleported) {
                // TODO find a safe location to teleport to instead of spawn, check a 7 x 7 x 7 area around the teleport location.
                LOGGER.warn("Failed to teleport {} to spawn. Pos {} {} {}", player.getName().getString(), spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ());
                throw new Exception("Failed to teleport to spawn"); // unable to teleport to spawn
            }
        } catch (Exception e) {
            LOGGER.error("Failed to teleport to spawn", e);
            player.sendMessage(Text.literal("Failed to teleport to spawn"), false);
        }
    }

    private void executeTeleportToHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // TODO implement teleport to home.
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        player.sendMessage(Text.literal("Home not implemented yet"), false);
    }

    private void executeAddHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String homeName = context.getArgument("name", String.class);
        checkHomeName(homeName);
        LOGGER.info("Adding home {} for player {}", homeName, player.getName().getString());
        player.sendMessage(Text.literal("New Home location added: " + homeName), false);
        // TODO add home to player's list of homes and store it in the database.
    }

    private void executeRemoveHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String homeName = context.getArgument("name", String.class);
        checkHomeName(homeName);
        LOGGER.info("Removing home {} for player {}", homeName, player.getName().getString());
        player.sendMessage(Text.literal("Home removed: " + homeName), false);
        // TODO remove home from player's list of homes and remove it in the database.'
    }

    private void handleException(Exception e) {
        if (e instanceof CommandSyntaxException cse) {
            LOGGER.error("Failed to execute command: {}: {}", cse.getType(), cse.getMessage());
        } else {
            LOGGER.error("Unexpected error executing command: {}", e.getMessage());
        }
    }

    private void checkHomeName(String homeName) throws CommandSyntaxException {
        if (homeName.isEmpty()) {
            LOGGER.warn("Home name cannot be empty");
            throw new CommandSyntaxException(null, Text.literal("Home name cannot be empty"));
        }
    }
}
