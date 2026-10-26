package com.lanvexstudios.model;

public class Antenna {

    private int rods;
    private int copper;
    private int netherite;

    public Antenna() {
    }

    public Antenna(int rods, int copper, int netherite) {
        this.rods = rods;
        this.copper = copper;
        this.netherite = netherite;
    }

    public int getRods() {
        return rods;
    }

    public void setRods(int rods) {
        this.rods = rods;
    }

    public int getCopper() {
        return copper;
    }

    public void setCopper(int copper) {
        this.copper = copper;
    }

    public int getNetherite() {
        return netherite;
    }

    public void setNetherite(int netherite) {
        this.netherite = netherite;
    }

    public boolean hasRod() {
        return rods > 0;
    }
}
