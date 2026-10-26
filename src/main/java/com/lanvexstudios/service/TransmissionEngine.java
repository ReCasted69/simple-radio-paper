package com.lanvexstudios.service;

import com.lanvexstudios.config.Config;
import com.lanvexstudios.model.AntennaScan;
import com.lanvexstudios.model.Radio;
import com.lanvexstudios.model.RadioKey;
import com.lanvexstudios.model.RadioMode;
import com.lanvexstudios.physics.RadioPhysics;
import com.lanvexstudios.physics.SignalQuality;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.block.data.type.Switch;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class TransmissionEngine {

    private static final int SCAN_EVERY_TICKS = 5;
    private static final double INTERFERENCE_SIGNAL_FACTOR = 0.7;
    private static final Particle.DustOptions RECEIVE_DUST = new Particle.DustOptions(Color.fromRGB(255, 60, 60), 1.0f);

    private final JavaPlugin plugin;
    private final Config config;
    private final RadioRegistry registry;
    private final AntennaScanner scanner;
    private final RadioPhysics physics;
    private final Propagation propagation;

    private final Map<RadioKey, AntennaScan> scanCache = new HashMap<>();
    private final Map<RadioKey, Integer> lastScanTick = new HashMap<>();
    private int tick;

    public TransmissionEngine(JavaPlugin plugin, Config config, RadioRegistry registry,
                              AntennaScanner scanner, RadioPhysics physics,
                              Propagation propagation) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.scanner = scanner;
        this.physics = physics;
        this.propagation = propagation;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 1L, config.tickInterval);
    }

    private void tick() {
        try {
            doTick();
        } catch (Throwable t) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "SimpleRadio transmission tick failed", t);
        }
    }

    private void doTick() {
        tick++;
        Map<Double, List<ActiveTransmitter>> active = new HashMap<>();

        for (Radio radio : registry.getAll()) {
            Block base = blockAt(radio.getKey());
            if (base == null) {
                cleanup(radio);
                continue;
            }
            Material type = base.getType();
            boolean isBase = scanner.isBase(type);
            boolean isCore = type == Material.REDSTONE_BLOCK;
            if (!isBase && !isCore) {
                cleanup(radio);
                continue;
            }
            if (isCore && radio.getMode() != RadioMode.RECEIVER) {
                base.setType(radio.getBaseMaterial());
            }

            AntennaScan scan = getScan(radio, base);
            radio.getAntenna().setRods(scan.getAntenna().getRods());
            radio.getAntenna().setCopper(scan.getAntenna().getCopper());
            radio.getAntenna().setNetherite(scan.getAntenna().getNetherite());
            radio.setMaxRange(physics.maxRange(scan.getAntenna()));
            radio.setAntennaHeight(scan.getHeight());
            radio.setWorld(base.getWorld());
            radio.setNoteLocation(scanner.findNoteBlock(base));
            radio.setVoiceUnit(radio.getNoteLocation() != null);

            if (!scan.hasRod()) {
                continue;
            }

            if (radio.getMode() == RadioMode.TRANSMITTER) {
                int strength = detectInputStrength(base);
                radio.setInputStrength(strength);
                if (strength > 0) {
                    double freq = round(radio.getFrequency());
                    Location top = base.getLocation().add(0.5, scan.getHeight(), 0.5);
                    active.computeIfAbsent(freq, k -> new ArrayList<>())
                            .add(new ActiveTransmitter(radio, radio.getMaxRange(), strength, top));
                }
            }
        }

        for (Radio radio : registry.getAll()) {
            if (radio.getMode() == RadioMode.TRANSMITTER) continue;

            Block base = blockAt(radio.getKey());
            if (base == null) continue;
            Material type = base.getType();
            boolean isBase = scanner.isBase(type);
            boolean isCore = type == Material.REDSTONE_BLOCK;
            if (!isBase && !isCore) continue;

            if (!radio.getAntenna().hasRod()) {
                if (isCore) base.setType(radio.getBaseMaterial());
                radio.setLinked(false);
                radio.setSignalPercent(0);
                radio.setOutputStrength(0);
                radio.setLinkedSourceKey(null);
                radio.setLinkedDistance(0);
                radio.setInterference(false);
                radio.setTransmitterCount(0);
                radio.setQuality(SignalQuality.STATIC);
                continue;
            }

            double freq = round(radio.getFrequency());
            List<ActiveTransmitter> transmitters = active.get(freq);

            ActiveTransmitter best = null;
            double bestSignal = 0;
            double bestDist = 0;
            int reachCount = 0;

            if (transmitters != null) {
                Location rxTop = base.getLocation().add(0.5, radio.getAntennaHeight(), 0.5);
                for (ActiveTransmitter tx : transmitters) {
                    if (tx.radio.getKey().equals(radio.getKey())) continue;
                    if (tx.radio.getWorld() == null || !tx.radio.getWorld().equals(radio.getWorld())) continue;
                    double dist = distance(base, blockAt(tx.radio.getKey()));
                    int obstructions = propagation.countObstructions(rxTop, tx.top);
                    double effRange = physics.effectiveRange(tx.range, radio.getMaxRange(), obstructions);
                    double sp = physics.signalPercent(dist, effRange);
                    if (sp > 0) reachCount++;
                    if (sp > bestSignal) {
                        bestSignal = sp;
                        bestDist = dist;
                        best = tx;
                    }
                }
            }

            boolean interference = reachCount > 1 && config.interferenceEnabled;
            if (interference) {
                bestSignal *= INTERFERENCE_SIGNAL_FACTOR;
            }

            boolean linked = best != null && bestSignal > 0;
            boolean flicker = interference && linked
                    && ThreadLocalRandom.current().nextDouble() < config.interferenceFlickerChance;

            radio.setInterference(interference);
            radio.setTransmitterCount(reachCount);
            radio.setLinked(linked);
            radio.setSignalPercent(bestSignal);
            radio.setQuality(SignalQuality.fromPercent(bestSignal, config));

            if (linked && best != null) {
                radio.setLinkedSourceKey(best.radio.getKey());
                radio.setLinkedDistance(bestDist);
            } else {
                radio.setLinkedSourceKey(null);
                radio.setLinkedDistance(0);
            }

            if (linked && best != null) {
                int out = best.strength;
                if (config.attenuateBySignal) {
                    out = (int) Math.round(out * bestSignal / 100.0);
                }
                if (flicker) {
                    out = 0;
                }
                radio.setOutputStrength(out);

                boolean powerCore = radio.getMode() == RadioMode.RECEIVER && !flicker;
                setReceiverCore(base, powerCore, radio);

                if (powerCore && config.receiveDust) {
                    spawnReceiveDust(base);
                }
            } else {
                radio.setOutputStrength(0);
                setReceiverCore(base, false, radio);
            }
        }
    }

    private void spawnReceiveDust(Block base) {
        World world = base.getWorld();
        if (world == null) return;
        double x = base.getX() + 0.5;
        double y = base.getY() + 1.05;
        double z = base.getZ() + 0.5;
        for (int i = 0; i < 3; i++) {
            world.spawnParticle(Particle.DUST, x, y, z, 1, 0.4, 0.3, 0.4, 0, RECEIVE_DUST);
        }
    }

    private AntennaScan getScan(Radio radio, Block base) {
        RadioKey key = radio.getKey();
        Integer last = lastScanTick.get(key);
        AntennaScan cached = scanCache.get(key);
        if (cached != null && last != null && tick - last < SCAN_EVERY_TICKS) {
            return cached;
        }
        AntennaScan scan = scanner.scan(base);
        scanCache.put(key, scan);
        lastScanTick.put(key, tick);
        return scan;
    }

    private void cleanup(Radio radio) {
        RadioKey key = radio.getKey();
        Block base = blockAt(key);
        if (base != null && base.getType() == Material.REDSTONE_BLOCK) {
            base.setType(radio.getBaseMaterial());
        }
        scanCache.remove(key);
        lastScanTick.remove(key);
        registry.remove(key);
    }

    public void clearOutput(Radio radio) {
        Block base = blockAt(radio.getKey());
        if (base == null) return;
        if (base.getType() == Material.REDSTONE_BLOCK) {
            base.setType(radio.getBaseMaterial());
        }
    }

    private void setReceiverCore(Block base, boolean powered, Radio radio) {
        if (powered) {
            if (base.getType() != Material.REDSTONE_BLOCK) {
                base.setType(Material.REDSTONE_BLOCK);
            }
        } else {
            if (base.getType() == Material.REDSTONE_BLOCK) {
                base.setType(radio.getBaseMaterial());
            }
        }
    }

    private int detectInputStrength(Block base) {
        int max = 0;
        World world = base.getWorld();
        if (world == null) return 0;
        int bx = base.getX(), by = base.getY(), bz = base.getZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Block b = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    int s = strengthOf(b);
                    if (s > max) max = s;
                }
            }
        }
        return max;
    }

    private int strengthOf(Block b) {
        Material t = b.getType();
        if (t == Material.REDSTONE_WIRE) {
            return ((RedstoneWire) b.getBlockData()).getPower();
        }
        if (t == Material.LEVER) {
            return ((Switch) b.getBlockData()).isPowered() ? 15 : 0;
        }
        if (t == Material.REPEATER) {
            return ((Repeater) b.getBlockData()).isPowered() ? 15 : 0;
        }
        if (t == Material.REDSTONE_BLOCK) {
            return 15;
        }
        if (t == Material.REDSTONE_TORCH || t == Material.REDSTONE_WALL_TORCH) {
            return ((Lightable) b.getBlockData()).isLit() ? 15 : 0;
        }
        if (Tag.BUTTONS.isTagged(t) || Tag.PRESSURE_PLATES.isTagged(t)) {
            if (b.getBlockData() instanceof Powerable powerable) {
                return powerable.isPowered() ? 15 : 0;
            }
        }
        if (b.getBlockData() instanceof AnaloguePowerable analogue) {
            return analogue.getPower();
        }
        return 0;
    }

    private Block blockAt(RadioKey key) {
        World world = Bukkit.getWorld(key.getWorld());
        if (world == null) return null;
        return world.getBlockAt(key.getX(), key.getY(), key.getZ());
    }

    private double distance(Block a, Block b) {
        if (a == null || b == null) return Double.MAX_VALUE;
        return a.getLocation().add(0.5, 0.5, 0.5)
                .distance(b.getLocation().add(0.5, 0.5, 0.5));
    }

    private double round(double f) {
        return Math.round(f * 100.0) / 100.0;
    }

    private static class ActiveTransmitter {
        final Radio radio;
        final double range;
        final int strength;
        final Location top;

        ActiveTransmitter(Radio radio, double range, int strength, Location top) {
            this.radio = radio;
            this.range = range;
            this.strength = strength;
            this.top = top;
        }
    }
}
