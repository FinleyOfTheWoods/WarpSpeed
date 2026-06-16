package uk.co.finleyofthewoods.warpspeed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import uk.co.finleyofthewoods.warpspeed.command.utils.ContextHelper;
import uk.co.finleyofthewoods.warpspeed.manager.TPXRequestManager;
import uk.co.finleyofthewoods.warpspeed.manager.impl.TPXRequestManagerImpl;
import uk.co.finleyofthewoods.warpspeed.model.TPARequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Slf4j
public class TPXCommand {
    private static final TPXRequestManager tpxRequestManager = new TPXRequestManagerImpl();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        log.debug("Registering TPX command");
        dispatcher.register(literal("tpa")
                .then(argument("player", EntityArgument.player())
                        .suggests(TPXCommand::suggestPlayers)
                        .executes(TPXCommand::executeTPASendRequest)));
        dispatcher.register(literal("tpaccept")
                .then(argument("player", EntityArgument.player())
                        .suggests(TPXCommand::suggestTPARequests)
                        .executes(TPXCommand::executeAcceptTPARequest)));
    }

    private static CompletableFuture<Suggestions> suggestPlayers(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        try {
            ServerPlayer player = ContextHelper.getPlayerFromContext(context.getSource());
            if (player == null) {
                log.error("Failed to get player from context when constructing suggestions");
                return null;
            }
            ServerLevel level = player.level();
            MinecraftServer server = level.getServer();
            String input = builder.getInput();
            server.getPlayerList().getPlayers().stream()
                    .filter(p -> p != player)
                    .filter(p -> !p.isSpectator())
                    .filter(p -> p.getDisplayName().getString().toLowerCase().contains(input.toLowerCase()))
                    .forEach(p -> builder.suggest(p.getDisplayName().getString()));
            return builder.buildFuture();
        } catch (Exception e) {
            log.error("Failed to suggest players", e);
            return null;
        }
    }

    private static CompletableFuture<Suggestions> suggestTPARequests(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        try {
            ServerPlayer player = ContextHelper.getPlayerFromContext(context.getSource());
            if (player == null) {
                log.error("Failed to get player from context when constructing TPA suggestions");
                return null;
            }
            String input = builder.getInput();
            List<TPARequest> requests = tpxRequestManager.getRequests(player);
            if (requests == null || requests.isEmpty()) {
                return null;
            }
            requests.stream()
                .filter(r -> r.getTarget().getDisplayName().getString().toLowerCase().contains(input.toLowerCase()))
                .forEach(r -> builder.suggest(r.getTarget().getDisplayName().getString()));
            return builder.buildFuture();
        } catch (Exception e) {
            log.error("Failed to suggest players", e);
            return null;
        }
    }

    private static int executeTPASendRequest(CommandContext<CommandSourceStack> context) {
       try {
           CommandSourceStack source = ContextHelper.getSourceFromContext(context);
           ServerPlayer player = ContextHelper.getPlayerFromContext(source);
           if (player == null) {
               source.sendFailure(Component.literal("Unable to get player from context")
                       .withStyle(ChatFormatting.RED));
               return 1;
           }
           ServerPlayer target = ContextHelper.getPlayerEntityFromContext(context, "target");

           TPARequest request = new TPARequest(player, target, System.currentTimeMillis());
           tpxRequestManager.createRequest(request);
           return 0;
       } catch (Exception e) {
           log.error("Failed to send TPA request", e);
           return 1;
       }
    }

    private static int executeAcceptTPARequest(CommandContext<CommandSourceStack> context) {
        return 0;
    }
}
