package uk.co.finleyofthewoods.warpspeed.infrastructure.exceptions;

public class TpxRequestFailedException extends RuntimeException {
    public TpxRequestFailedException(String message) {
        super(message);
    }
}

