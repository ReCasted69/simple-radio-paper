package com.lanvexstudios.model;

import java.util.Objects;
import java.util.UUID;

public class RadioKey {

    private final UUID world;
    private final int x;
    private final int y;
    private final int z;

    public RadioKey(UUID world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String toStoreKey() {
        return world + ":" + x + ":" + y + ":" + z;
    }

    public static RadioKey fromStoreKey(String key) {
        String[] parts = key.split(":");
        if (parts.length != 4) return null;
        try {
            return new RadioKey(UUID.fromString(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RadioKey key)) return false;
        return x == key.x && y == key.y && z == key.z && Objects.equals(world, key.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }
}
