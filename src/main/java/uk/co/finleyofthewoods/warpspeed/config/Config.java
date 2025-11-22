package uk.co.finleyofthewoods.warpspeed.config;

public class Config {
    private int maxPlayerHomes = 10;
    private int maxPlayerWarps = 254;
    private int rtpCooldown = 30;

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

    public int getRtpCooldown() {
        return rtpCooldown;
    }

    public void setRtpCooldown(int cooldown) {
        rtpCooldown = cooldown;
    }
}
