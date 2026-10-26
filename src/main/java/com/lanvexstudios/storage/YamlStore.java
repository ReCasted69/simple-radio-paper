package com.lanvexstudios.storage;

import com.lanvexstudios.model.Antenna;
import com.lanvexstudios.model.Radio;
import com.lanvexstudios.model.RadioKey;
import com.lanvexstudios.model.RadioMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class YamlStore {

    private final JavaPlugin plugin;
    private final File file;
    private final double defaultFrequency;

    public YamlStore(JavaPlugin plugin, double defaultFrequency) {
        this.plugin = plugin;
        this.defaultFrequency = defaultFrequency;
        this.file = new File(plugin.getDataFolder(), "radios.yml");
    }

    public List<Radio> load() {
        List<Radio> radios = new ArrayList<>();
        if (!file.exists()) return radios;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("radios");
        if (section == null) return radios;

        for (String key : section.getKeys(false)) {
            RadioKey radioKey = RadioKey.fromStoreKey(key);
            if (radioKey == null) continue;

            ConfigurationSection r = section.getConfigurationSection(key);
            if (r == null) continue;

            Radio radio = new Radio(radioKey);
            radio.setFrequency(r.getDouble("frequency", defaultFrequency));
            try {
                radio.setMode(RadioMode.valueOf(r.getString("mode", "RECEIVER").toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                radio.setMode(RadioMode.RECEIVER);
            }
            try {
                radio.setBaseMaterial(org.bukkit.Material.valueOf(
                        r.getString("base", "CHISELED_COPPER").toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                radio.setBaseMaterial(org.bukkit.Material.CHISELED_COPPER);
            }

            Antenna antenna = radio.getAntenna();
            ConfigurationSection a = r.getConfigurationSection("antenna");
            if (a != null) {
                antenna.setRods(a.getInt("rods", 0));
                antenna.setCopper(a.getInt("copper", 0));
                antenna.setNetherite(a.getInt("netherite", 0));
            }

            radios.add(radio);
        }
        return radios;
    }

    public void save(Iterable<Radio> radios) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("radios");

        for (Radio radio : radios) {
            ConfigurationSection r = section.createSection(radio.getKey().toStoreKey());
            r.set("frequency", radio.getFrequency());
            r.set("mode", radio.getMode().name());
            r.set("base", radio.getBaseMaterial().name());
            Antenna antenna = radio.getAntenna();
            r.set("antenna.rods", antenna.getRods());
            r.set("antenna.copper", antenna.getCopper());
            r.set("antenna.netherite", antenna.getNetherite());
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
            yaml.save(tmp);
            java.nio.file.Files.move(tmp.toPath(), file.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save radios.yml: " + e.getMessage());
        }
    }
}
