package com.lanvexstudios.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Config {

    public double baseRange;
    public double rangePerRod;
    public double rangePerCopper;
    public double rangePerNetherite;
    public double obstructionRangePenalty;

    public double frequencyMinMhz;
    public double frequencyMaxMhz;
    public int tickInterval;

    public boolean interferenceEnabled;
    public double interferenceFlickerChance;
    public double signalClearThreshold;
    public double signalStrongThreshold;

    public int guiRefreshTicks;
    public double coarseStepMhz;
    public double scanStepMhz;
    public int scanIntervalTicks;

    public List<Material> baseBlocks = new ArrayList<>();
    public List<Material> rodBlocks = new ArrayList<>();
    public List<Material> copperBlocks = new ArrayList<>();
    public List<Material> netheriteBlocks = new ArrayList<>();

    public boolean attenuateBySignal;

    public boolean voiceEnabled;
    public int micRadius;
    public int speakerRange;
    public String voiceCategory;
    public double voiceVolume;
    public double voicePickupRadiusScale;
    public int voicePickupRadiusMax;
    public boolean voiceAttenuateBySignal;
    public boolean voiceRequireKeyed;
    public boolean voiceHearOwnTransmission;
    public boolean voiceCrackleBySignal;

    public boolean receiveDust;

    public double lightningStrikeRadius;
    public Material lightningResultBlock;
    public boolean lightningFire;
    public int lightningFireRadius;

    public void load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        FileConfiguration c = plugin.getConfig();

        baseRange = c.getDouble("physics.base-range", 100);
        rangePerRod = c.getDouble("physics.range-per-rod", 250);
        rangePerCopper = c.getDouble("physics.range-per-copper", 500);
        rangePerNetherite = c.getDouble("physics.range-per-netherite", 1000000);
        obstructionRangePenalty = c.getDouble("physics.obstruction-range-penalty", 1.0);

        frequencyMinMhz = c.getDouble("physics.frequency-min-mhz", 0.1);
        frequencyMaxMhz = c.getDouble("physics.frequency-max-mhz", 108.0);
        tickInterval = Math.max(1, c.getInt("physics.tick-interval", 1));

        interferenceEnabled = c.getBoolean("physics.interference-enabled", true);
        interferenceFlickerChance = clamp(c.getDouble("physics.interference-flicker-chance", 0.35), 0.0, 1.0);
        signalClearThreshold = c.getDouble("physics.signal-clear-threshold", 60);
        signalStrongThreshold = c.getDouble("physics.signal-strong-threshold", 90);

        guiRefreshTicks = c.getInt("gui.refresh-ticks", 5);
        coarseStepMhz = Math.max(0.1, c.getDouble("gui.coarse-step-mhz", 1.0));
        scanStepMhz = Math.max(0.1, c.getDouble("gui.scan-step-mhz", 0.2));
        scanIntervalTicks = Math.max(1, c.getInt("gui.scan-interval-ticks", 20));

        baseBlocks = parseMaterials(c.getStringList("base-blocks"), Material.CHISELED_COPPER, Material.WAXED_CHISELED_COPPER);
        rodBlocks = parseMaterials(c.getStringList("antenna.rods"), Material.LIGHTNING_ROD);
        copperBlocks = parseMaterials(c.getStringList("antenna.copper"), Material.COPPER_BLOCK, Material.WAXED_COPPER_BLOCK);
        netheriteBlocks = parseMaterials(c.getStringList("antenna.netherite"), Material.NETHERITE_BLOCK);

        attenuateBySignal = c.getBoolean("redstone.attenuate-by-signal", false);

        voiceEnabled = c.getBoolean("voice.enabled", true);
        micRadius = c.getInt("voice.mic-radius", 8);
        speakerRange = c.getInt("voice.speaker-range", 32);
        voiceCategory = c.getString("voice.category", "radio");
        voiceVolume = c.getDouble("voice.volume", 1.0);
        voicePickupRadiusScale = c.getDouble("voice.pickup-radius-scale", 0.5);
        voicePickupRadiusMax = c.getInt("voice.pickup-radius-max", 16);
        voiceAttenuateBySignal = c.getBoolean("voice.attenuate-by-signal", true);
        voiceRequireKeyed = c.getBoolean("voice.require-keyed", false);
        voiceHearOwnTransmission = c.getBoolean("voice.hear-own-transmission", true);
        voiceCrackleBySignal = c.getBoolean("voice.crackle-by-signal", true);

        receiveDust = c.getBoolean("effects.receive-dust", true);

        lightningStrikeRadius = Math.max(0.5, c.getDouble("lightning.strike-radius", 1.5));
        lightningResultBlock = parseMaterial(c.getString("lightning.result-block", "COAL_BLOCK"), Material.COAL_BLOCK);
        lightningFire = c.getBoolean("lightning.fire", true);
        lightningFireRadius = Math.max(1, c.getInt("lightning.fire-radius", 1));
    }

    private List<Material> parseMaterials(List<String> names, Material... defaults) {
        List<Material> out = new ArrayList<>();
        for (String name : names) {
            try {
                Material m = Material.valueOf(name.trim().toUpperCase(Locale.ROOT));
                out.add(m);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (out.isEmpty()) {
            for (Material d : defaults) out.add(d);
        }
        return out;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
