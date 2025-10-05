package uk.co.finleyofthewoods.warpspeed.exceptions;

public class NoSafeLocationFoundException extends Exception {
    /**
     * Constructor for NoSafeLocationFoundException.
     * Exception thrown when no safe location is found for the player to teleport to within the search radius.
     * @param message A user-friendly message describing the exception.
     */
    public NoSafeLocationFoundException(String message) {
        super(message);
    }
}
