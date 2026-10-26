package com.lanvexstudios.physics;

import com.lanvexstudios.config.Config;
import com.lanvexstudios.model.Antenna;

public class RadioPhysics {

    private final Config config;

    public RadioPhysics(Config config) {
        this.config = config;
    }

    public double maxRange(Antenna antenna) {
        return config.baseRange
                + antenna.getRods() * config.rangePerRod
                + antenna.getCopper() * config.rangePerCopper
                + antenna.getNetherite() * config.rangePerNetherite;
    }

    public double effectiveRange(double txRange, double rxRange, int obstructions) {
        return Math.min(txRange, rxRange) - obstructions * config.obstructionRangePenalty;
    }

    public double signalPercent(double distance, double effectiveRange) {
        if (effectiveRange <= 0) return 0;
        double pct = 100.0 * (1.0 - distance / effectiveRange);
        return Math.max(0.0, Math.min(100.0, pct));
    }
}
