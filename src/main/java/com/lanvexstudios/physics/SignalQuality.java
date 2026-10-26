package com.lanvexstudios.physics;

import com.lanvexstudios.config.Config;

public enum SignalQuality {
    STATIC("Static"),
    CLEAR("Clear"),
    STRONG("Strong");

    private final String display;

    SignalQuality(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    public static SignalQuality fromPercent(double signalPercent, Config config) {
        if (signalPercent >= config.signalStrongThreshold) return STRONG;
        if (signalPercent >= config.signalClearThreshold) return CLEAR;
        return STATIC;
    }
}
