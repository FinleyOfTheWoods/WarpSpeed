package uk.co.finleyofthewoods.warpspeed.command.utils;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import uk.co.finleyofthewoods.warpspeed.exception.MissingSourceException;

@Slf4j
public class ContextHelper {
    public static @NonNull CommandSourceStack getSourceFromContext(@NonNull CommandContext<CommandSourceStack> context) throws MissingSourceException {
        CommandSourceStack source = context.getSource();
        if (source == null) {
            throw new MissingSourceException("Missing source");
        }
        return source;
    }

    public static @Nullable String getStringFromContext(@NonNull CommandContext<CommandSourceStack> context, String key) {
        return StringArgumentType.getString(context, key);
    }

    public static boolean getBooleanFromContext(@NonNull CommandContext<CommandSourceStack> context, String key) {
        return BoolArgumentType.getBool(context, key);
    }

    public static @Nullable ServerPlayer getPlayerFromContext(@NonNull CommandSourceStack source) throws CommandSyntaxException {
        if (isPlayer(source)) {
            ServerPlayer player = source.getPlayerOrException();
            log.debug("returning player from context: {}", player.getPlainTextName());
            return player;
        }
        source.sendFailure(Component.literal("Command source is not a player"));
        return null;
    }

    private static boolean isPlayer(CommandSourceStack source) {
        if (source.isPlayer()) {
            log.debug("Command source is a player: {}", source.getTextName());
            return true;
        }
        log.warn("Command source is not a player: {}", source.getTextName());
        return false;
    }
}
