package com.lanvexstudios.listener;

import com.lanvexstudios.config.Messages;
import com.lanvexstudios.model.AntennaScan;
import com.lanvexstudios.model.Radio;
import com.lanvexstudios.model.RadioKey;
import com.lanvexstudios.service.AntennaScanner;
import com.lanvexstudios.service.RadioRegistry;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class StructureListener implements Listener {

    private final Messages messages;
    private final AntennaScanner scanner;
    private final RadioRegistry registry;

    public StructureListener(Messages messages, AntennaScanner scanner, RadioRegistry registry) {
        this.messages = messages;
        this.scanner = scanner;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        Material type = placed.getType();

        Block base = null;
        if (scanner.isBase(type)) {
            base = placed;
        } else if (scanner.isAntenna(type)) {
            base = findBaseBelow(placed);
        }
        if (base == null) return;

        RadioKey key = keyOf(base);
        if (registry.contains(key)) return;

        AntennaScan scan = scanner.scan(base);
        if (!scan.hasRod()) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("simpleradio.assemble")) {
            player.sendMessage(messages.prefix() + " " + messages.get("no-permission"));
            return;
        }

        Radio radio = registry.add(key);
        radio.setBaseMaterial(base.getType());
        radio.getAntenna().setRods(scan.getAntenna().getRods());
        radio.getAntenna().setCopper(scan.getAntenna().getCopper());
        radio.getAntenna().setNetherite(scan.getAntenna().getNetherite());
        registry.saveAll();

        player.playSound(base.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.2f);
        player.sendMessage(messages.prefix() + " " + messages.get("assembled"));
    }

    private Block findBaseBelow(Block block) {
        Block current = block.getRelative(BlockFace.DOWN);
        while (scanner.isAntenna(current.getType())) {
            current = current.getRelative(BlockFace.DOWN);
        }
        return scanner.isBase(current.getType()) ? current : null;
    }

    private RadioKey keyOf(Block block) {
        return new RadioKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }
}
