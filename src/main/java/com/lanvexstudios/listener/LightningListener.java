package com.lanvexstudios.listener;

import com.lanvexstudios.config.Config;
import com.lanvexstudios.gui.RadioGui;
import com.lanvexstudios.model.Radio;
import com.lanvexstudios.model.RadioKey;
import com.lanvexstudios.service.AntennaScanner;
import com.lanvexstudios.service.RadioRegistry;
import com.lanvexstudios.service.TransmissionEngine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.LightningStrikeEvent;

public class LightningListener implements Listener {

    private final Config config;
    private final AntennaScanner scanner;
    private final RadioRegistry registry;
    private final TransmissionEngine engine;

    public LightningListener(Config config, AntennaScanner scanner,
                             RadioRegistry registry, TransmissionEngine engine) {
        this.config = config;
        this.scanner = scanner;
        this.registry = registry;
        this.engine = engine;
    }

    @EventHandler
    public void onLightning(LightningStrikeEvent event) {
        Location strike = event.getLightning().getLocation();

        for (Radio radio : registry.getAll()) {
            World world = radio.getWorld();
            if (world == null || !world.equals(strike.getWorld())) continue;
            Block base = world.getBlockAt(radio.getKey().getX(), radio.getKey().getY(), radio.getKey().getZ());
            if (!scanner.isBase(base.getType())) continue;

            Location top = base.getLocation().add(0.5, radio.getAntennaHeight(), 0.5);
            if (strike.distance(top) > config.lightningStrikeRadius) continue;

            destroy(radio, base);
        }
    }

    private void destroy(Radio radio, Block base) {
        World world = base.getWorld();
        Location center = base.getLocation().add(0.5, 0.5, 0.5);

        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
        world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);

        engine.clearOutput(radio);
        base.setType(config.lightningResultBlock);

        if (config.lightningFire) {
            int radius = config.lightningFireRadius;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Block b = base.getRelative(dx, 0, dz);
                    if (b.getType() == Material.AIR) {
                        b.setType(Material.FIRE);
                    }
                }
            }
        }

        Block current = base.getRelative(BlockFace.UP);
        while (scanner.isAntenna(current.getType())) {
            current.setType(Material.AIR);
            current = current.getRelative(BlockFace.UP);
        }

        registry.remove(radio.getKey());
        registry.saveAll();
        RadioGui.closeFor(radio.getKey());
    }
}
