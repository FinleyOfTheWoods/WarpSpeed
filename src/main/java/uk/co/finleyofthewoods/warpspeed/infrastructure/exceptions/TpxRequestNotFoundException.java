package uk.co.finleyofthewoods.warpspeed.infrastructure.exceptions;

public class TpxRequestNotFoundException extends RuntimeException {
    public TpxRequestNotFoundException(String message) {
        super(message);
    }
}
