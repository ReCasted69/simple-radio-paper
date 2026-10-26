package com.lanvexstudios.listener;

import com.lanvexstudios.config.Messages;
import com.lanvexstudios.gui.RadioGui;
import com.lanvexstudios.model.AntennaScan;
import com.lanvexstudios.model.Radio;
import com.lanvexstudios.model.RadioKey;
import com.lanvexstudios.service.AntennaScanner;
import com.lanvexstudios.service.RadioRegistry;
import com.lanvexstudios.service.TransmissionEngine;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockListener implements Listener {

    private final JavaPlugin plugin;
    private final Messages messages;
    private final AntennaScanner scanner;
    private final RadioRegistry registry;
    private final TransmissionEngine engine;

    public BlockListener(JavaPlugin plugin, Messages messages, AntennaScanner scanner,
                         RadioRegistry registry, TransmissionEngine engine) {
        this.plugin = plugin;
        this.messages = messages;
        this.scanner = scanner;
        this.registry = registry;
        this.engine = engine;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();

        boolean isBase = scanner.isBase(type);
        boolean isCore = type == Material.REDSTONE_BLOCK;
        boolean isAntenna = scanner.isAntenna(type);
        if (!isBase && !isCore && !isAntenna) return;

        if (isAntenna) {
            handleAntennaBreak(block, event.getPlayer());
            return;
        }

        RadioKey key = keyOf(block);
        if (!registry.contains(key)) return;

        Radio radio = registry.get(key);

        if (isCore) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(messages.prefix() + " " + messages.get("cannot-break-powered"));
            return;
        }

        engine.clearOutput(radio);
        registry.remove(key);
        registry.saveAll();
        RadioGui.closeFor(key);
        event.getPlayer().sendMessage(messages.prefix() + " " + messages.get("radio-removed"));
    }

    private void handleAntennaBreak(Block block, Player player) {
        Block base = findBaseBelow(block);
        if (base == null) return;

        RadioKey key = keyOf(base);
        if (!registry.contains(key)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            AntennaScan scan = scanner.scan(base);
            if (scan.hasRod()) return;
            Radio radio = registry.get(key);
            if (radio == null) return;
            engine.clearOutput(radio);
            registry.remove(key);
            registry.saveAll();
            RadioGui.closeFor(key);
            player.sendMessage(messages.prefix() + " " + messages.get("radio-removed"));
        });
    }

    private Block findBaseBelow(Block block) {
        Block current = block.getRelative(BlockFace.DOWN);
        while (scanner.isAntenna(current.getType())) {
            current = current.getRelative(BlockFace.DOWN);
        }
        if (scanner.isBase(current.getType())) return current;
        if (current.getType() == Material.REDSTONE_BLOCK) return current;
        return null;
    }

    private RadioKey keyOf(Block block) {
        return new RadioKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }
}
