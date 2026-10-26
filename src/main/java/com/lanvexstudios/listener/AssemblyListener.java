package com.lanvexstudios.listener;

import com.lanvexstudios.config.Messages;
import com.lanvexstudios.gui.RadioGui;
import com.lanvexstudios.model.AntennaScan;
import com.lanvexstudios.model.Radio;
import com.lanvexstudios.model.RadioKey;
import com.lanvexstudios.service.AntennaScanner;
import com.lanvexstudios.service.RadioRegistry;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class AssemblyListener implements Listener {

    private final Messages messages;
    private final AntennaScanner scanner;
    private final RadioRegistry registry;
    private final RadioGui gui;

    public AssemblyListener(Messages messages, AntennaScanner scanner,
                            RadioRegistry registry, RadioGui gui) {
        this.messages = messages;
        this.scanner = scanner;
        this.registry = registry;
        this.gui = gui;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        boolean isBase = scanner.isBase(block.getType());
        boolean isCore = block.getType() == Material.REDSTONE_BLOCK;
        if (!isBase && !isCore) return;

        if (!player.isSneaking() && holdingPlaceableBlock(player)) {
            return;
        }

        RadioKey key = keyOf(block);
        boolean registered = registry.contains(key);

        if (isCore && !registered) return;

        event.setCancelled(true);

        if (registered) {
            if (!player.hasPermission("simpleradio.use")) {
                player.sendMessage(messages.prefix() + " " + messages.get("no-permission"));
                return;
            }
            gui.open(player, registry.get(key));
            return;
        }

        AntennaScan scan = scanner.scan(block);
        if (!scan.hasRod()) {
            player.sendMessage(messages.prefix() + " " + messages.get("inactive-no-rod"));
            return;
        }

        if (!player.hasPermission("simpleradio.assemble")) {
            player.sendMessage(messages.prefix() + " " + messages.get("no-permission"));
            return;
        }

        Radio radio = registry.add(key);
        radio.setBaseMaterial(block.getType());
        radio.getAntenna().setRods(scan.getAntenna().getRods());
        radio.getAntenna().setCopper(scan.getAntenna().getCopper());
        radio.getAntenna().setNetherite(scan.getAntenna().getNetherite());
        registry.saveAll();
        player.playSound(block.getLocation().add(0.5, 0.5, 0.5), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.2f);
        player.sendMessage(messages.prefix() + " " + messages.get("assembled"));
        gui.open(player, radio);
    }

    private boolean holdingPlaceableBlock(Player player) {
        return isPlaceableBlock(player.getInventory().getItemInMainHand())
                || isPlaceableBlock(player.getInventory().getItemInOffHand());
    }

    private boolean isPlaceableBlock(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getType().isBlock();
    }

    private RadioKey keyOf(Block block) {
        return new RadioKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }
}
