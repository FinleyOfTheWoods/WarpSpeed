package uk.co.finleyofthewoods.warpspeed.utils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import uk.co.finleyofthewoods.warpspeed.infrastructure.BlocklistOfPlayer;
import uk.co.finleyofthewoods.warpspeed.infrastructure.exceptions.*;
import uk.co.finleyofthewoods.warpspeed.infrastructure.tpa.request.AbstractTpxRequest;
import uk.co.finleyofthewoods.warpspeed.infrastructure.tpa.request.impl.*;


public class TpxRequestManager {

    private static final Set<AbstractTpxRequest> requestSet = ConcurrentHashMap.newKeySet();

    public static int tickCounter = 0;

    public static void tick(){
        if (tickCounter >= 20) { // once every second
            requestSet.forEach(request -> {
                if (!request.isActive()) {
                    request.getSender().sendMessage(Text.literal("§o§6Your request to " + request.getReceivers().getFirst().getName().getString() + " has expired."), false);
                    request.getReceivers().getFirst().sendMessage(Text.literal("§o§6The request from " + request.getSender().getName().getString() + " has expired."), false);
                    requestSet.remove(request);
                }
            });
            tickCounter = 0;
        }
        tickCounter++;
    }


    public static boolean isPlayerIdentical(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        return player1.getUuid().equals(player2.getUuid());
    }

    public static boolean areReceiversIdentical(List<ServerPlayerEntity> receivers1, List<ServerPlayerEntity> receivers2) {
        return receivers1.stream().allMatch( player -> receivers2.stream()
                        .anyMatch( receiver -> receiver.getUuid().equals(player.getUuid())));
    }

    public static List<AbstractTpxRequest> findRequestsFromSender(ServerPlayerEntity sender) {

        return requestSet.stream()
                .filter( request -> isPlayerIdentical(sender, request.getSender())).toList();

    }

    public static List<AbstractTpxRequest> findRequestsToPlayer(ServerPlayerEntity receiver) {

        return requestSet.stream()
                .filter( request -> request.getReceivers().stream().anyMatch( rec -> isPlayerIdentical(receiver, rec))).toList();

    }

    public static Optional<AbstractTpxRequest> findRequestToPlayerFromSender(ServerPlayerEntity receiver, ServerPlayerEntity sender) {

        return requestSet.stream()
                .filter( request -> isPlayerIdentical(request.getSender(), sender)
                        && !request.getReceivers().isEmpty()
                        && isPlayerIdentical(receiver, request.getReceivers().getFirst())).findFirst();

    }

    public static boolean alreadySentTeleportRequest(Class<? extends AbstractTpxRequest> requestClass, ServerPlayerEntity sender, List<ServerPlayerEntity> receivers) {
        return requestSet.stream().anyMatch(tpxRequestable -> tpxRequestable.getClass().equals(requestClass)
                && isPlayerIdentical(tpxRequestable.getSender(), sender)
                && areReceiversIdentical(tpxRequestable.getReceivers(), receivers));
    }

    //maybe: permissions + tphere, tpallhere
    //maybe: walk to cancel ?


    public static boolean makeMultiTargetsToSenderRequest(MultipleTargetsToPrivilegedSenderRequest request) throws TpxNotAllowedException, TpxRequestAlreadyExistsException {
        if (request.getReceivers().isEmpty())
            throw new TpxRequestFailedException("No receivers in request: " + request.getReceivers());

        //todo: directly do the teleport
        requestSet.add(request);
        return true;
    }

    public static boolean makeSenderToSingleTargetRequest(SenderToSingleTargetRequest request, DatabaseManager databaseManager) throws TpxNotAllowedException, TpxRequestAlreadyExistsException, TpxRequestNotFoundException {
        if (request.getReceivers().size() != 1)
            throw new TpxRequestFailedException("No receivers in request or too many receivers: " + request.getReceivers());
        if (isPlayerIdentical(request.getSender(), request.getReceivers().getFirst())) {
            throw new TpxRequestFailedException("Can't tp to yourself.");
        }
        //todo: check permission
        if (alreadySentTeleportRequest(request.getClass(), request.getSender(), request.getReceivers())) {
            throw new TpxRequestAlreadyExistsException("You have already sent  a request to that player!");
        }
        if (isPlayerBlockedByPlayer(request.getSender(), request.getReceivers().getFirst(), databaseManager)){
            throw new TpxNotAllowedException("This player can't be sent teleport requests at this moment.");
        }
        requestSet.add(request);
        return true;
    }

    public static boolean makeSingleTargetToSenderRequest(SingleTargetToSingleSenderRequest request, DatabaseManager databaseManager) throws TpxNotAllowedException, TpxRequestAlreadyExistsException {
        if (request.getReceivers().size() != 1)
            throw new TpxRequestFailedException("No receivers in request or too many receivers: " + request.getReceivers());
        if (isPlayerIdentical(request.getSender(), request.getReceivers().getFirst())) {
            throw new TpxRequestFailedException("Can't tp yourself to to yourself.");
        }
        //todo: check permission
        if (alreadySentTeleportRequest(request.getClass(), request.getSender(), request.getReceivers())) {
            throw new TpxRequestAlreadyExistsException("You have already sent a request to that player!");
        }
        if (isPlayerBlockedByPlayer(request.getSender(), request.getReceivers().getFirst(), databaseManager)){
            throw new TpxNotAllowedException("This player can't be sent teleport requests at this moment.");
        }
        requestSet.add(request);
        return true;
    }

    public static boolean makeSingleTargetToPrivilegedSenderRequest(@NotNull SingleTargetToPrivilegedSenderRequest request) throws TpxNotAllowedException, TpxRequestAlreadyExistsException {
        // makeRequest(sender, receiver) -> make sure sender and receiver are not the same, check permission, blocklist, request doesn't exist yet, then make request and send msg
        if (request.getReceivers().size() != 1)
            throw new TpxRequestFailedException(" receivers in request or too many receivers: " + request.getReceivers());
        if (isPlayerIdentical(request.getSender(), request.getReceivers().getFirst())) {
            throw new TpxRequestFailedException("n't tp yourself to to yourself.");
        }
        //todo: check permission
        //todo: teleport directly
        //requestSet.add(request);
        return true;
    }

    public static boolean denySingleTpaRequest(ServerPlayerEntity receiver, Optional<ServerPlayerEntity> sender){
        Optional<AbstractTpxRequest> request = Optional.empty();
        if (sender.isEmpty()){
            List<AbstractTpxRequest> results = findRequestsToPlayer(receiver);
            if (results.size() > 1){
                receiver.sendMessage(Text.literal("§o§cYou have received too many pending requests, please provide a username to deny the request of."), false);
            } else if(results.isEmpty()){
                receiver.sendMessage(Text.literal("§o§cYou have currently no open requests."), false);
            } else {
                request = Optional.of(results.getFirst());
            }
        } else {
            request = findRequestToPlayerFromSender(receiver, sender.get());
        }

        if (request.isPresent()) {
            receiver.sendMessage(Text.literal("§o§6You have denied the request of " + request.get().getSender().getName().getString()), false);
            request.get().getSender().sendMessage( Text.literal("§c§6" + receiver.getName().getString() + " has denied your request."), false);
            requestSet.remove(request.get());
            return true;
        } else {
            throw new TpxRequestNotFoundException("Request couldn't be found");
        }
    }

    public static boolean acceptSingleTpaRequest(ServerPlayerEntity receiver, Optional<ServerPlayerEntity> sender){

        Optional<AbstractTpxRequest> request = resolveRequestToReceiver(receiver, sender);

        if (request.isPresent()) {
            receiver.sendMessage(Text.literal("§o§6You have accepted the request of " + request.get().getSender().getName().getString() ), false);
            request.get().getSender().sendMessage(Text.literal("§o§6" +receiver.getName().getString() + " has accepted your request."), false);

            requestSet.remove(request.get());
            return request.get().doTeleport();
        } else {
            throw new TpxRequestNotFoundException("Request couldn't be found");
        }
    }

    public static boolean cancelSingleTpaRequest(ServerPlayerEntity sender, Optional<ServerPlayerEntity> receiver){
        Optional<AbstractTpxRequest> request = resolveRequestFromSender(sender, receiver);

        if (request.isPresent()) {
            ServerPlayerEntity receiverPlayer = request.get().getReceivers().getFirst();
            sender.sendMessage(Text.literal("§o§cYou have cancelled your request to " + receiverPlayer.getName().getString()), false);
            receiverPlayer.sendMessage(Text.literal("§o§c" + sender.getName().getString() + " has cancelled their request."), false);
            requestSet.remove(request.get());
            return true;
        } else {
             throw new TpxRequestNotFoundException("Request couldn't be found");
        }
    }

    public static boolean blockPlayerForPlayer(ServerPlayerEntity blockingPlayer, String blockedPlayerName, DatabaseManager databaseManager){
        if (blockingPlayer.getName().getString().equals(blockedPlayerName)){
            throw new TpxBlockingFailedException("You can't block yourself.");
        }
        if (!databaseManager.isPlayerBlockedByPlayer(blockedPlayerName, blockingPlayer.getName().getString())){
            if(!databaseManager.addPlayerToBlockList(blockedPlayerName, blockingPlayer.getName().getString()))
                throw new TpxBlockingFailedException("Blocking has failed");
            blockingPlayer.sendMessage(Text.literal("§o§6You have blocked " + blockedPlayerName + " from sending you teleport requests."), false);
            return true;
        } else {
            throw new TpxBlockingFailedException("Failed to block player. You might have already blocked them.");
        }
    }

    public static boolean unblockPlayerForPlayer(String blockedPlayerName, ServerPlayerEntity blockingPlayer, DatabaseManager databaseManager){
        if (blockedPlayerName.equals(blockingPlayer.getName().getString())){
            throw new TpxUnblockingFailedException("You can't unblock yourself.");
        }
        if (databaseManager.isPlayerBlockedByPlayer(blockedPlayerName, blockingPlayer.getName().getString())){
            blockingPlayer.sendMessage(Text.literal("§o§6You have unblocked " + blockedPlayerName + ". They can send you teleport requests again."), false);
            if (!databaseManager.removePlayerFromBlocklist(blockedPlayerName, blockingPlayer.getName().getString()))
                throw new TpxUnblockingFailedException("Unblocking has failed");
            return true;
        } else {
            throw new TpxUnblockingFailedException("Failed to unblock player from requesting.");
        }
    }

    public static boolean getBlocklistOfPlayer(ServerPlayerEntity blockingPlayer, DatabaseManager databaseManager){
        BlocklistOfPlayer blockedPlayers = databaseManager.getBlocklistForPlayer(blockingPlayer.getName().getString());
        if (blockedPlayers.getBlockedByPlayer().isEmpty()){
            blockingPlayer.sendMessage(Text.literal("§o§6(empty)"), false);
        } else {
            blockingPlayer.sendMessage(Text.literal("§o§6Players blocked from sending you teleport requests:"), false);
            blockingPlayer.sendMessage(Text.literal("§o§6" + blockedPlayers.getBlockedByPlayer()), false);

        }
        return true;
    }

    public static boolean isPlayerBlockedByPlayer(ServerPlayerEntity blockedPlayer, ServerPlayerEntity blockingPlayer, DatabaseManager databaseManager){
        return databaseManager.isPlayerBlockedByPlayer(blockedPlayer.getName().getString(), blockingPlayer.getName().getString());
    }



    public static Optional<AbstractTpxRequest> resolveRequestToReceiver(ServerPlayerEntity receiver, Optional<ServerPlayerEntity> sender){

        if (sender.isEmpty()){
            //search for possible matches and guess
            List<AbstractTpxRequest> results = findRequestsToPlayer(receiver);
            if (results.size() > 1){
                receiver.sendMessage(Text.literal("You have received too many pending requests, please provide a username."), false);
            } else if(results.isEmpty()){
                receiver.sendMessage(Text.literal("You have currently no open requests."), false);
            } else {
                return Optional.of(results.getFirst());
            }
            return Optional.empty();
        } else {
            //just get that one specified request
            return findRequestToPlayerFromSender(receiver, sender.get());
        }
    }



    public static Optional<AbstractTpxRequest> resolveRequestFromSender(ServerPlayerEntity sender, Optional<ServerPlayerEntity> receiver){

        if (receiver.isEmpty()){
            //search for possible matches and guess
            List<AbstractTpxRequest> results = findRequestsFromSender(sender);
            if (results.size() > 1){
                sender.sendMessage(Text.literal("§cYou have too many pending requests to different people, please provide a specific username."), false);
            } else if(results.isEmpty()){
                sender.sendMessage(Text.literal("§cYou have currently no pending requests."), false);
            } else {
                return Optional.of(results.getFirst());
            }
            return Optional.empty();
        } else {
            //just get that one specified request
            return findRequestToPlayerFromSender(receiver.get(), sender);
        }
    }
}
