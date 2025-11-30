package uk.co.finleyofthewoods.warpspeed.config;

import org.slf4j.event.Level;

public class Config {
    private int maxPlayerHomes = 10;
    private int maxPlayerWarps = 254;
    private int tpCooldown = 10;
    private int maxAttempts = 256;
    private boolean enableLavaCheck = true;
    private boolean enableCactusCheck = true;
    private boolean enableFireCheck = true;
    private boolean enableWitherRoseCheck = true;
    private boolean enableMagmaBlockCheck = true;
    private boolean enableSweetBerryCheck = true;
    private boolean enableFallCheck = true;

    public int getPlayerHomeLimit() {
        return maxPlayerHomes;
    }

    public void setPlayerHomeLimit(int limit) {
        maxPlayerHomes = limit;
    }

    public int getPlayerWarpLimit() {
        return maxPlayerWarps;
    }

    public void setPlayerWarpLimit(int limit) {
        maxPlayerWarps = limit;
    }

    public int getTPCooldown() {
        return tpCooldown;
    }

    public void setTPCooldown(int cooldown) {
        tpCooldown = cooldown;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isLavaCheckEnabled() {
        return enableLavaCheck;
    }

    public void setEnableLavaCheck(boolean enableLavaCheck) {
        this.enableLavaCheck = enableLavaCheck;
    }

    public boolean isCactusCheckEnabled() {
        return enableCactusCheck;
    }

    public void setEnableCactusCheck(boolean enableCactusCheck) {
        this.enableCactusCheck = enableCactusCheck;
    }

    public boolean isFireCheckEnabled() {
        return enableFireCheck;
    }

    public void setEnableFireCheck(boolean enableFireCheck) {
        this.enableFireCheck = enableFireCheck;
    }

    public boolean isWitherRoseCheckEnabled() {
        return enableWitherRoseCheck;
    }

    public void setEnableWitherRoseCheck(boolean enableWitherRoseCheck) {
        this.enableWitherRoseCheck = enableWitherRoseCheck;
    }

    public boolean isMagmaBlockCheckEnabled() {
        return enableMagmaBlockCheck;
    }

    public void setEnableMagmaBlockCheck(boolean enableMagmaBlockCheck) {
        this.enableMagmaBlockCheck = enableMagmaBlockCheck;
    }

    public boolean isSweetBerryBushCheckEnabled() {
        return enableSweetBerryCheck;
    }

    public void setEnableSweetBerryCheck(boolean enableSweetBerryCheck) {
        this.enableSweetBerryCheck = enableSweetBerryCheck;
    }

    public boolean isFallCheckEnabled() {
        return enableFallCheck;
    }

    public void setEnableFallCheck(boolean enableFallCheck) {
        this.enableFallCheck = enableFallCheck;
    }

    public String toString() {
        return "Config{enableLavaCheck=" + enableLavaCheck +
                ", enableCactusCheck=" + enableCactusCheck +
                ", enableFireCheck=" + enableFireCheck +
                ", enableWitherRoseCheck=" + enableWitherRoseCheck +
                ", enableMagmaBlockCheck=" + enableMagmaBlockCheck +
                ", enableSweetBerryCheck=" + enableSweetBerryCheck +
                ", enableFallCheck=" + enableFallCheck +
                '}';
    }
}
