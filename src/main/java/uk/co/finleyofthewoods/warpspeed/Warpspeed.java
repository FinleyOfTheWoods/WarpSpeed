package uk.co.finleyofthewoods.warpspeed;

import com.mojang.brigadier.CommandDispatcher;
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


import static net.minecraft.server.command.CommandManager.literal;

public class Warpspeed implements ModInitializer {
    public static final String MOD_ID = "warpspeed";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initialising Warpspeed mod");
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            registerCommand(dispatcher);
        }));
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        /// Handle /spawn command and teleport the player to the world spawn location.
        dispatcher.register(literal("spawn").requires(source -> source.getPlayer() != null).executes(context -> {
            try {
                executeTeleportToSpawn(context);
            } catch (Exception e) {
                handleException(e);
            }
            return 0;
        }));
        /// Handle /home command and teleport the player to the home location.
        dispatcher.register(literal("home").requires(source -> source.getPlayer() != null).executes(context -> {
            try {
                executeTeleportToHome(context);
            } catch (Exception e) {
                handleException(e);
            }
            return 0;
        }));
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

    private void handleException(Exception e) {
        if (e instanceof CommandSyntaxException cse) {
            LOGGER.error("Failed to execute command: {}: {}", cse.getType(), cse.getMessage());
        } else {
            LOGGER.error("Unexpected error executing command: {}", e.getMessage());
        }
    }
}
