package uk.co.finleyofthewoods.warpspeed.infrastructure.exceptions;

public class TpxBlockingFailedException extends RuntimeException {
    public TpxBlockingFailedException(String message) {
        super(message);
    }
}

