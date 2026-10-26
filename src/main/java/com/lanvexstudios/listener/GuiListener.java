package com.lanvexstudios.listener;

import com.lanvexstudios.gui.RadioGui;
import com.lanvexstudios.model.Radio;
import com.lanvexstudios.model.RadioMode;
import com.lanvexstudios.service.TransmissionEngine;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class GuiListener implements Listener {

    private final RadioGui gui;
    private final TransmissionEngine engine;

    public GuiListener(RadioGui gui, TransmissionEngine engine) {
        this.gui = gui;
        this.engine = engine;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !RadioGui.TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        Radio radio = gui.getOpenRadio(player);
        if (radio == null) return;

        int slot = event.getRawSlot();
        if (slot == 10) {
            gui.onFrequencyClick(player, radio, event.isLeftClick(), event.isShiftClick());
        } else if (slot == 16) {
            RadioMode previous = radio.getMode();
            gui.onModeClick(player, radio);
            if (previous == RadioMode.RECEIVER && radio.getMode() != RadioMode.RECEIVER) {
                engine.clearOutput(radio);
            }
        } else if (slot == 22) {
            gui.onScanClick(player, radio);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!RadioGui.TITLE.equals(event.getView().getTitle())) return;
        if (event.getPlayer() instanceof Player player) {
            gui.close(player);
        }
    }
}
