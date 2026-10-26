package com.lanvexstudios.model;

public class AntennaScan {

    private final Antenna antenna;
    private final int height;

    public AntennaScan(Antenna antenna, int height) {
        this.antenna = antenna;
        this.height = height;
    }

    public Antenna getAntenna() {
        return antenna;
    }

    public int getHeight() {
        return height;
    }

    public boolean hasRod() {
        return antenna.hasRod();
    }
}
