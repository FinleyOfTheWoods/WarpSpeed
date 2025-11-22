package uk.co.finleyofthewoods.warpspeed.infrastructure.exceptions;

public class TpxRequestAlreadyExistsException extends RuntimeException {
    public TpxRequestAlreadyExistsException(String message) {
        super(message);
    }
}
