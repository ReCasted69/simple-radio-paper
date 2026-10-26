package com.lanvexstudios.service;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Ray-marches between two antenna tops and counts the number of solid
 * (occluding) blocks the radio signal has to travel through. Each obstruction
 * reduces the effective range by a flat per-block penalty.
 */
public class Propagation {

    public int countObstructions(Location a, Location b) {
        if (a == null || b == null) return 0;
        World world = a.getWorld();
        if (world == null || !world.equals(b.getWorld())) return 0;

        double dist = a.distance(b);
        if (dist <= 2.0) return 0;

        int steps = (int) Math.min(128, Math.max(1, Math.round(dist)));
        int obstructions = 0;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            double x = a.getX() + (b.getX() - a.getX()) * t;
            double y = a.getY() + (b.getY() - a.getY()) * t;
            double z = a.getZ() + (b.getZ() - a.getZ()) * t;
            Block block = world.getBlockAt(floor(x), floor(y), floor(z));
            if (isObstructing(block.getType())) obstructions++;
        }
        return obstructions;
    }

    private boolean isObstructing(Material type) {
        return type.isOccluding();
    }

    private int floor(double v) {
        return (int) Math.floor(v);
    }
}
