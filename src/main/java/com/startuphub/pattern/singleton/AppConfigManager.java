package com.startuphub.pattern.singleton;

public final class AppConfigManager {
    private static volatile AppConfigManager instance;

    private double defaultBurnRateWarningThreshold = 0.8;
    private int startingSimulatedMonth = 1;

    private AppConfigManager() {}

    public static AppConfigManager getInstance() {
        if (instance == null) {
            synchronized (AppConfigManager.class) {
                if (instance == null) {
                    instance = new AppConfigManager();
                }
            }
        }
        return instance;
    }

    public double getDefaultBurnRateWarningThreshold() { return defaultBurnRateWarningThreshold; }
    public int getStartingSimulatedMonth() { return startingSimulatedMonth; }
}
