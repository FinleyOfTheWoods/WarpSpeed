package uk.co.finleyofthewoods.warpspeed.utils.tpa;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.utils.TeleportUtils;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.request.TpxRequestable;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.request.impl.TpaRequest;


public class TpxRequestManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TpxRequestManager.class);

    private static final Set<TpxRequestable> requestSet = ConcurrentHashMap.newKeySet();

    public static Set<TpxRequestable> getRequestSet() {
        return requestSet;
    }

    public static boolean isPlayerIdentical(ServerPlayerEntity player1, ServerPlayerEntity player2) {
        return player1.getUuid().equals(player2.getUuid());
    }

    public static boolean areReceiversIdentical(List<ServerPlayerEntity> receivers1, List<ServerPlayerEntity> receivers2) {
        return receivers1.stream().allMatch( player -> receivers2.stream()
                        .anyMatch( receiver -> receiver.getUuid().equals(player.getUuid())));
    }

    public static void clearRequestSet() {
        requestSet.clear();
    }

    public static List<TpxRequestable> findRequestsToPlayer(ServerPlayerEntity receiver) {

        return requestSet.stream()
                .filter( request -> request.getReceivers().stream().anyMatch( rec -> isPlayerIdentical(receiver, rec))).toList();

    }

    public static Optional<TpxRequestable> findRequestsToPlayerFromSender(ServerPlayerEntity receiver, ServerPlayerEntity sender) {

        return requestSet.stream()
                .filter( request -> isPlayerIdentical(request.getSender(), sender)
                        && !request.getReceivers().isEmpty()
                        && isPlayerIdentical(receiver, request.getReceivers().getFirst())).findFirst();

    }


    public static Optional<TpxRequestable> findRequestsOfPlayer(ServerPlayerEntity sender, ServerPlayerEntity receiver) {

        // Check if a request with the same sender and all the same receivers exists in the requestSet and return it
        return requestSet.stream()
                .filter( request -> isPlayerIdentical(request.getSender(), sender)
                && !request.getReceivers().isEmpty()
                && isPlayerIdentical(receiver, request.getReceivers().getFirst())).findFirst();

    }

    public static Optional<TpxRequestable> findRequestsOfPlayer(ServerPlayerEntity sender) {

        // Check if a request with the same sender and all the same receivers exists in the requestSet and return it
        return requestSet.stream()
                .filter( request -> isPlayerIdentical(request.getSender(), sender)).findFirst();

    }

    public static Optional<TpxRequestable> teleportRequestExists(TpxRequestable requestToFind) {

        // Check if a request with the same sender and all the same receivers exists in the requestSet and return it
        return requestSet.stream()
                .filter( request -> isPlayerIdentical(request.getSender(), requestToFind.getSender())
                && areReceiversIdentical(request.getReceivers(), requestToFind.getReceivers())).findFirst();

    }

    public static boolean alreadySentTeleportRequest(ServerPlayerEntity sender, List<ServerPlayerEntity> receivers) {
        return requestSet.stream().anyMatch(tpxRequestable -> tpxRequestable.getStatus().equals(TpaStatus.PENDING)
                && isPlayerIdentical(tpxRequestable.getSender(), sender)
                && areReceiversIdentical(tpxRequestable.getReceivers(), receivers));
    }

    // todo: methods to implement

    //save previousLocation to PlayerLocationTracker

    //later: blocklist, block, unblock => save to file


    // tpahere -> one single receiver, with approval
    // tpAllHere -> many receivers, no approval
    // tphere -> one receiver, no approval

    // refresh requests one by one and expire old ones, send msg

    // add / delete request

    // makeRequest(sender, receiver) -> make sure sender and receiver are not the same, check permission, blocklist, request doesn't exist yet, then make request and send msg
    // acceptRequest (request, receiver) -> handle and then teleport logic
    // cancelRequest (sender, receiver) -> send feedback to sender and receiver and delete
    // sendMessage

    public static boolean makeTpaRequest(ServerPlayerEntity sender, ServerPlayerEntity receiver) throws TpxNotAllowedException, TpxRequestAlreadyExistsException {
        // makeRequest(sender, receiver) -> make sure sender and receiver are not the same, check permission, blocklist, request doesn't exist yet, then make request and send msg
        if (isPlayerIdentical(sender, receiver)) {
            return false;
        }
        //todo: check permission, blocklist
        if (alreadySentTeleportRequest(sender, List.of(receiver))) {
            sender.sendMessage(Text.literal("You have already sent a request to that player!"), false);
            return false;
        }
        TpxRequestable request = new TpaRequest(sender, receiver);
        requestSet.add(request);
        return true;
    }

    public static boolean denyTpaRequest(ServerPlayerEntity receiver, Optional<ServerPlayerEntity> target){
        try {
            Optional<TpxRequestable> request = Optional.empty();
            if (target.isEmpty()){
                List<TpxRequestable> results = findRequestsToPlayer(receiver);
                if (results.size() > 1){
                    receiver.sendMessage(Text.literal("You have received too many pending requests, please provide a username to deny the request of."), false);
                } else if(results.isEmpty()){
                    receiver.sendMessage(Text.literal("You have currently no open requests."), false);
                } else {
                    request = Optional.of(results.getFirst());
                }
            } else {
                request = findRequestsToPlayerFromSender(receiver, target.get());
            }

            if (request.isPresent()) {
                request.get().setStatus(TpaStatus.DENIED);
                receiver.sendMessage(Text.literal("You have denied the request of " + request.get().getSender().getName().getString()), false);
                request.get().getSender().sendMessage(Text.literal(receiver.getName().getString() + " has denied your request."), false);
                requestSet.remove(request.get());
                return true;
            } else {
                receiver.sendMessage(Text.literal("No request found."), false);
                return false;

            }
        } catch (Exception e) {
            LOGGER.debug("failed to deny tpa request.", e);
            return false;
        }
    }


    public static void teleport(TpxRequestable request) {
        ServerPlayerEntity sender = request.getSender();
        List<ServerPlayerEntity> receivers = request.getReceivers();


        if (request.canTeleport()) {
            //make sure to use TeleportUtils and PlayerLocationTracker
            // play sound for receiver
            //TeleportTarget target = new TeleportTarget(sender.getEntityWorld(), sender.getEntityPos(), sender.getVelocity(), sender.getYaw(), sender.getPitch(), TeleportTarget.NO_OP);
            request.teleport();
        }
    }


}
