package uk.co.finleyofthewoods.warpspeed.infrastructure.exceptions;

public class TpxExpiredException extends RuntimeException {
    public TpxExpiredException(String message) {
        super(message);
    }
}
