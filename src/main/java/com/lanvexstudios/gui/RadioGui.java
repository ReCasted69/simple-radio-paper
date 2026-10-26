package com.lanvexstudios.gui;

import com.lanvexstudios.config.Config;
import com.lanvexstudios.config.Messages;
import com.lanvexstudios.model.Radio;
import com.lanvexstudios.model.RadioKey;
import com.lanvexstudios.model.RadioMode;
import com.lanvexstudios.physics.SignalQuality;
import com.lanvexstudios.service.RadioRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RadioGui {

    public static final String TITLE = Messages.color("&1Wireless Configuration");

    private static final int SCAN_HOLD_INTERVALS = 3;

    private static final Map<UUID, RadioKey> OPEN_RADIOS = new ConcurrentHashMap<>();

    private final JavaPlugin plugin;
    private final Config config;
    private final RadioRegistry registry;
    private final Map<RadioKey, Integer> scanHold = new ConcurrentHashMap<>();

    public RadioGui(JavaPlugin plugin, Config config, RadioRegistry registry) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
    }

    public void startRefreshTask() {
        if (config.guiRefreshTicks <= 0) return;
        long interval = Math.max(1, config.guiRefreshTicks);
        new BukkitRunnable() {
            @Override
            public void run() {
                refreshAll();
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    public void startScanTask() {
        long interval = Math.max(1, config.scanIntervalTicks);
        new BukkitRunnable() {
            @Override
            public void run() {
                scanStep();
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void scanStep() {
        for (Map.Entry<UUID, RadioKey> entry : OPEN_RADIOS.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            Radio radio = registry.get(entry.getValue());
            if (radio == null || !radio.isScanning()) continue;
            if (!TITLE.equals(player.getOpenInventory().getTitle())) continue;

            Integer hold = scanHold.get(radio.getKey());
            if (hold != null && hold > 0) {
                scanHold.put(radio.getKey(), hold - 1);
                continue;
            }

            double f = radio.getFrequency() + config.scanStepMhz;
            if (f > config.frequencyMaxMhz) f = config.frequencyMinMhz;
            f = Math.round(f * 10) / 10.0;
            radio.setFrequency(f);

            if (isActiveFrequency(radio)) {
                scanHold.put(radio.getKey(), SCAN_HOLD_INTERVALS);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.4f);
            } else {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 0.8f);
            }
            refresh(player, radio);
        }
    }

    private boolean isActiveFrequency(Radio radio) {
        if (radio.getWorld() == null) return false;
        double freq = round(radio.getFrequency());
        for (Radio other : registry.getAll()) {
            if (other.getKey().equals(radio.getKey())) continue;
            if (other.getMode() != RadioMode.TRANSMITTER) continue;
            if (other.getInputStrength() <= 0) continue;
            if (other.getWorld() == null || !other.getWorld().equals(radio.getWorld())) continue;
            if (round(other.getFrequency()) == freq) return true;
        }
        return false;
    }

    private void refreshAll() {
        for (Map.Entry<UUID, RadioKey> entry : OPEN_RADIOS.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            Radio radio = registry.get(entry.getValue());
            if (radio == null) continue;
            if (!TITLE.equals(player.getOpenInventory().getTitle())) continue;
            refresh(player, radio);
        }
    }

    public void open(Player player, Radio radio) {
        OPEN_RADIOS.put(player.getUniqueId(), radio.getKey());
        player.openInventory(buildInventory(radio));
    }

    public void close(Player player) {
        OPEN_RADIOS.remove(player.getUniqueId());
    }

    public Radio getOpenRadio(Player player) {
        RadioKey key = OPEN_RADIOS.get(player.getUniqueId());
        if (key == null) return null;
        return registry.get(key);
    }

    public static void closeFor(RadioKey key) {
        for (Map.Entry<UUID, RadioKey> e : OPEN_RADIOS.entrySet()) {
            if (e.getValue().equals(key)) {
                Player p = Bukkit.getPlayer(e.getKey());
                if (p != null) p.closeInventory();
                OPEN_RADIOS.remove(e.getKey());
            }
        }
    }

    private Inventory buildInventory(Radio radio) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        ItemStack pane = pane();
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);
        inv.setItem(10, frequencyItem(radio));
        inv.setItem(13, statsItem(radio));
        inv.setItem(16, modeItem(radio));
        inv.setItem(22, scanItem(radio));
        return inv;
    }

    private ItemStack pane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack frequencyItem(Radio radio) {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = radio.isScanning()
                    ? Messages.color("&eFrequency Control &b[SCANNING]")
                    : Messages.color("&eFrequency Control");
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7Current: &b" + String.format("%.1f", radio.getFrequency()) + " MHz"));
            lore.add("");
            lore.add(Messages.color("&aLeft-Click: &7+" + String.format("%.1f", 0.1) + " MHz"));
            lore.add(Messages.color("&cRight-Click: &7-" + String.format("%.1f", 0.1) + " MHz"));
            lore.add(Messages.color("&eShift + Left-Click: &7+" + String.format("%.1f", config.coarseStepMhz) + " MHz"));
            lore.add(Messages.color("&eShift + Right-Click: &7-" + String.format("%.1f", config.coarseStepMhz) + " MHz"));
            lore.add(Messages.color("&7Exact: &f/radio setfreq <mhz>"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack statsItem(Radio radio) {
        ItemStack item = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&eRadio Status"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7Range: &f" + String.format("%.0f", radio.getMaxRange()) + " blocks"));
            lore.add(Messages.color("&7Antenna Height: &f" + radio.getAntennaHeight()));
            lore.add("");
            if (radio.getMode() == RadioMode.TRANSMITTER) {
                lore.add(Messages.color("&7Input: &f" + radio.getInputStrength() + "/15"));
                lore.add(Messages.color("&7Status: " + (radio.getInputStrength() > 0 ? "&aTransmitting" : "&7Idle")));
            } else {
                String signal = String.format("%.0f", radio.getSignalPercent());
                String color = radio.isLinked() ? "&a" : "&c";
                lore.add(Messages.color("&7Signal: " + color + signal + "%"));
                lore.add(Messages.color("&7Quality: " + qualityColor(radio.getQuality()) + radio.getQuality().getDisplay()));
                lore.add(Messages.color("&7Linked: " + (radio.isLinked() ? "&aYes" : "&cNo")));
                if (radio.isInterference()) {
                    lore.add(Messages.color("&cInterference: " + radio.getTransmitterCount() + " transmitters"));
                }
                if (radio.getMode() == RadioMode.RECEIVER) {
                    lore.add(Messages.color("&7Output: &f" + radio.getOutputStrength() + "/15"));
                }
            }
            if (config.voiceEnabled) {
                boolean voiceInstalled = Bukkit.getPluginManager().getPlugin("voicechat") != null;
                lore.add("");
                lore.add(Messages.color("&dVoice: " + (voiceInstalled ? "&aEnabled" : "&cUnavailable")));
                if (voiceInstalled) {
                    lore.add(Messages.color("&7Speaker: &f" + (radio.hasVoiceUnit() ? "Note Block" : "Antenna")));
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String qualityColor(SignalQuality quality) {
        return switch (quality) {
            case STRONG -> "&a";
            case CLEAR -> "&e";
            case STATIC -> "&c";
        };
    }

    private ItemStack modeItem(Radio radio) {
        RadioMode mode = radio.getMode();
        Material material = switch (mode) {
            case TRANSMITTER -> Material.REPEATER;
            case RECEIVER -> Material.REDSTONE_TORCH;
            case SOUND_RECEIVER -> Material.JUKEBOX;
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&eRadio Function Mode"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7Current: &f" + mode.getDisplay()));
            lore.add("");
            for (RadioMode option : RadioMode.values()) {
                String marker = option == mode ? "&a> " : "&8  ";
                lore.add(Messages.color(marker + "&f" + option.getDisplay()));
                lore.add(Messages.color("&8    " + option.getDescription()));
            }
            lore.add("");
            lore.add(Messages.color("&eClick to cycle modes."));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack scanItem(Radio radio) {
        ItemStack item = new ItemStack(radio.isScanning() ? Material.EMERALD : Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(radio.isScanning()
                    ? Messages.color("&aScanning: ON")
                    : Messages.color("&7Scanning: OFF"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7Automatically sweeps the frequency band."));
            lore.add(Messages.color("&7Pauses briefly on active frequencies."));
            lore.add("");
            lore.add(Messages.color("&eClick to toggle."));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void onFrequencyClick(Player player, Radio radio, boolean up, boolean coarse) {
        double f = radio.getFrequency();
        double step = coarse ? config.coarseStepMhz : 0.1;
        f += up ? step : -step;
        f = Math.round(f * 10) / 10.0;
        f = Math.max(config.frequencyMinMhz, Math.min(config.frequencyMaxMhz, f));
        radio.setFrequency(f);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        registry.saveAll();
        refresh(player, radio);
    }

    public void onModeClick(Player player, Radio radio) {
        radio.setMode(radio.getMode().next());
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 0.8f, 1.0f);
        registry.saveAll();
        refresh(player, radio);
    }

    public void onScanClick(Player player, Radio radio) {
        radio.setScanning(!radio.isScanning());
        if (radio.isScanning()) {
            scanHold.put(radio.getKey(), 0);
        } else {
            scanHold.remove(radio.getKey());
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, radio.isScanning() ? 1.5f : 0.8f);
        refresh(player, radio);
    }

    public void refresh(Player player, Radio radio) {
        Inventory top = player.getOpenInventory().getTopInventory();
        top.setItem(10, frequencyItem(radio));
        top.setItem(13, statsItem(radio));
        top.setItem(16, modeItem(radio));
        top.setItem(22, scanItem(radio));
    }

    private double round(double f) {
        return Math.round(f * 100.0) / 100.0;
    }
}
