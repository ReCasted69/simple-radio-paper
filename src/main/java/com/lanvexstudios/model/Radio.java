package com.lanvexstudios.model;

import com.lanvexstudios.physics.SignalQuality;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class Radio {

    private final RadioKey key;
    private double frequency;
    private RadioMode mode;
    private final Antenna antenna = new Antenna();
    private Material baseMaterial = Material.CHISELED_COPPER;

    private boolean voiceUnit;
    private int inputStrength;
    private double signalPercent;
    private int outputStrength;

    private volatile double maxRange;
    private volatile int antennaHeight;
    private volatile boolean linked;

    private volatile World world;
    private volatile Location noteLocation;

    private volatile RadioKey linkedSourceKey;
    private volatile double linkedDistance;

    private volatile boolean interference;
    private volatile int transmitterCount;
    private volatile SignalQuality quality = SignalQuality.STATIC;
    private volatile boolean scanning;

    public Radio(RadioKey key) {
        this.key = key;
    }

    public RadioKey getKey() {
        return key;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public RadioMode getMode() {
        return mode;
    }

    public void setMode(RadioMode mode) {
        this.mode = mode;
    }

    public Antenna getAntenna() {
        return antenna;
    }

    public Material getBaseMaterial() {
        return baseMaterial;
    }

    public void setBaseMaterial(Material baseMaterial) {
        this.baseMaterial = baseMaterial;
    }

    public boolean hasVoiceUnit() {
        return voiceUnit;
    }

    public void setVoiceUnit(boolean voiceUnit) {
        this.voiceUnit = voiceUnit;
    }

    public int getInputStrength() {
        return inputStrength;
    }

    public void setInputStrength(int inputStrength) {
        this.inputStrength = inputStrength;
    }

    public double getSignalPercent() {
        return signalPercent;
    }

    public void setSignalPercent(double signalPercent) {
        this.signalPercent = signalPercent;
    }

    public int getOutputStrength() {
        return outputStrength;
    }

    public void setOutputStrength(int outputStrength) {
        this.outputStrength = outputStrength;
    }

    public double getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(double maxRange) {
        this.maxRange = maxRange;
    }

    public int getAntennaHeight() {
        return antennaHeight;
    }

    public void setAntennaHeight(int antennaHeight) {
        this.antennaHeight = antennaHeight;
    }

    public boolean isLinked() {
        return linked;
    }

    public void setLinked(boolean linked) {
        this.linked = linked;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public Location getNoteLocation() {
        return noteLocation;
    }

    public void setNoteLocation(Location noteLocation) {
        this.noteLocation = noteLocation;
    }

    public RadioKey getLinkedSourceKey() {
        return linkedSourceKey;
    }

    public void setLinkedSourceKey(RadioKey linkedSourceKey) {
        this.linkedSourceKey = linkedSourceKey;
    }

    public double getLinkedDistance() {
        return linkedDistance;
    }

    public void setLinkedDistance(double linkedDistance) {
        this.linkedDistance = linkedDistance;
    }

    public boolean isInterference() {
        return interference;
    }

    public void setInterference(boolean interference) {
        this.interference = interference;
    }

    public int getTransmitterCount() {
        return transmitterCount;
    }

    public void setTransmitterCount(int transmitterCount) {
        this.transmitterCount = transmitterCount;
    }

    public SignalQuality getQuality() {
        return quality;
    }

    public void setQuality(SignalQuality quality) {
        this.quality = quality;
    }

    public boolean isScanning() {
        return scanning;
    }

    public void setScanning(boolean scanning) {
        this.scanning = scanning;
    }
}
