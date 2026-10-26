package com.lanvexstudios;

import com.lanvexstudios.command.RadioCommand;
import com.lanvexstudios.config.Config;
import com.lanvexstudios.config.Messages;
import com.lanvexstudios.gui.RadioGui;
import com.lanvexstudios.listener.AssemblyListener;
import com.lanvexstudios.listener.BlockListener;
import com.lanvexstudios.listener.GuiListener;
import com.lanvexstudios.listener.LightningListener;
import com.lanvexstudios.listener.StructureListener;
import com.lanvexstudios.physics.RadioPhysics;
import com.lanvexstudios.service.AntennaScanner;
import com.lanvexstudios.service.Propagation;
import com.lanvexstudios.service.RadioRegistry;
import com.lanvexstudios.service.TransmissionEngine;
import com.lanvexstudios.storage.YamlStore;
import com.lanvexstudios.voice.VoiceBridge;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleRadio extends JavaPlugin {

    public static final double DEFAULT_FREQUENCY = 91.3;

    private Config config;
    private Messages messages;
    private RadioRegistry registry;

    @Override
    public void onEnable() {
        config = new Config();
        config.load(this);
        messages = new Messages(this);

        YamlStore store = new YamlStore(this, DEFAULT_FREQUENCY);
        registry = new RadioRegistry(store, DEFAULT_FREQUENCY);
        registry.loadAll();

        AntennaScanner scanner = new AntennaScanner(config);
        RadioPhysics physics = new RadioPhysics(config);
        Propagation propagation = new Propagation();

        TransmissionEngine engine = new TransmissionEngine(this, config, registry, scanner, physics, propagation);
        engine.start();

        RadioGui gui = new RadioGui(this, config, registry);
        gui.startRefreshTask();
        gui.startScanTask();

        getServer().getPluginManager().registerEvents(
                new StructureListener(messages, scanner, registry), this);
        getServer().getPluginManager().registerEvents(
                new AssemblyListener(messages, scanner, registry, gui), this);
        getServer().getPluginManager().registerEvents(new GuiListener(gui, engine), this);
        getServer().getPluginManager().registerEvents(
                new BlockListener(this, messages, scanner, registry, engine), this);
        getServer().getPluginManager().registerEvents(
                new LightningListener(config, scanner, registry, engine), this);

        RadioCommand radioCommand = new RadioCommand(this, config, messages, registry, gui, physics, propagation);
        getCommand("radio").setExecutor(radioCommand);
        getCommand("radio").setTabCompleter(radioCommand);

        hookVoiceChat(physics, propagation);

        getServer().getScheduler().runTaskTimer(this, () -> registry.saveAll(), 6000L, 6000L);

        getLogger().info("SimpleRadio enabled (" + registry.size() + " radios loaded).");
    }

    private void hookVoiceChat(RadioPhysics physics, Propagation propagation) {
        if (!config.voiceEnabled) return;
        if (getServer().getPluginManager().getPlugin("voicechat") == null) {
            getLogger().info("Simple Voice Chat not found - voice feature disabled.");
            return;
        }
        try {
            BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
            if (service != null) {
                service.registerPlugin(new VoiceBridge(this, config, registry, physics, propagation));
                getLogger().info("Simple Voice Chat integration enabled.");
            }
        } catch (Throwable t) {
            getLogger().warning("Failed to hook into Simple Voice Chat: " + t.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (registry != null) {
            registry.saveAll();
        }
        getLogger().info("SimpleRadio disabled.");
    }
}
