package com.lanvexstudios.command;

import com.lanvexstudios.config.Config;
import com.lanvexstudios.config.Messages;
import com.lanvexstudios.gui.RadioGui;
import com.lanvexstudios.model.Radio;
import com.lanvexstudios.model.RadioKey;
import com.lanvexstudios.model.RadioMode;
import com.lanvexstudios.physics.RadioPhysics;
import com.lanvexstudios.service.Propagation;
import com.lanvexstudios.service.RadioRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RadioCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PLAYER_COMMANDS = List.of("help", "setfreq", "mode", "scan", "info");
    private static final List<String> ADMIN_COMMANDS = List.of("list", "debug", "reload");
    private static final List<String> MODE_NAMES = List.of("transmitter", "redstone", "sound");

    private final JavaPlugin plugin;
    private final Config config;
    private final Messages messages;
    private final RadioRegistry registry;
    private final RadioGui gui;
    private final RadioPhysics physics;
    private final Propagation propagation;

    public RadioCommand(JavaPlugin plugin, Config config, Messages messages,
                        RadioRegistry registry, RadioGui gui,
                        RadioPhysics physics,
                        Propagation propagation) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.registry = registry;
        this.gui = gui;
        this.physics = physics;
        this.propagation = propagation;
    }

    private Location micTop(Radio radio) {
        if (radio.getWorld() == null || radio.getKey() == null) return null;
        return radio.getWorld().getBlockAt(radio.getKey().getX(), radio.getKey().getY(), radio.getKey().getZ())
                .getLocation().add(0.5, radio.getAntennaHeight(), 0.5);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> sendHelp(sender);
            case "list" -> {
                if (!requireAdmin(sender)) return true;
                if (registry.size() == 0) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("no-radios"));
                    return true;
                }
                sender.sendMessage(messages.prefix() + " " + messages.get("list-header", "%count%", String.valueOf(registry.size())));
                for (Radio radio : registry.getAll()) {
                    String pos = radio.getKey().getX() + "," + radio.getKey().getY() + "," + radio.getKey().getZ();
                    sender.sendMessage(messages.get("list-entry",
                            "%pos%", pos,
                            "%freq%", String.format("%.1f", radio.getFrequency()),
                            "%mode%", radio.getMode().getDisplay()));
                }
            }
            case "setfreq", "freq", "tune" -> {
                if (!requireUse(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("players-only"));
                    return true;
                }
                Radio radio = gui.getOpenRadio(player);
                if (radio == null) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("unknown-radio"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("invalid-frequency"));
                    return true;
                }
                try {
                    double freq = Double.parseDouble(args[1]);
                    freq = Math.round(freq * 10) / 10.0;
                    freq = Math.max(config.frequencyMinMhz, Math.min(config.frequencyMaxMhz, freq));
                    radio.setFrequency(freq);
                    sender.sendMessage(messages.prefix() + " " + messages.get("frequency-set", "%freq%", String.format("%.1f", freq)));
                    gui.refresh(player, radio);
                } catch (NumberFormatException e) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("invalid-frequency"));
                }
            }
            case "mode" -> {
                if (!requireUse(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("players-only"));
                    return true;
                }
                Radio radio = gui.getOpenRadio(player);
                if (radio == null) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("unknown-radio"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("mode-invalid"));
                    return true;
                }
                RadioMode mode = parseMode(args[1]);
                if (mode == null) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("mode-invalid"));
                    return true;
                }
                radio.setMode(mode);
                sender.sendMessage(messages.prefix() + " " + messages.get("mode-set", "%mode%", mode.getDisplay()));
                gui.refresh(player, radio);
            }
            case "scan" -> {
                if (!requireUse(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("players-only"));
                    return true;
                }
                Radio radio = gui.getOpenRadio(player);
                if (radio == null) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("unknown-radio"));
                    return true;
                }
                gui.onScanClick(player, radio);
                if (radio.isScanning()) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("scan-start", "%freq%", String.format("%.1f", radio.getFrequency())));
                } else {
                    sender.sendMessage(messages.prefix() + " " + messages.get("scan-stop", "%freq%", String.format("%.1f", radio.getFrequency())));
                }
            }
            case "info" -> {
                if (!requireUse(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("players-only"));
                    return true;
                }
                Radio radio = gui.getOpenRadio(player);
                if (radio == null) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("unknown-radio"));
                    return true;
                }
                printInfo(sender, radio);
            }
            case "reload" -> {
                if (!requireAdmin(sender)) return true;
                config.load(plugin);
                messages.reload();
                sender.sendMessage(messages.prefix() + " " + messages.get("config-reloaded"));
            }
            case "debug" -> {
                if (!requireAdmin(sender)) return true;
                Radio radio = resolveDebugRadio(sender, args);
                if (radio == null) {
                    sender.sendMessage(messages.prefix() + " " + messages.get("unknown-radio"));
                    return true;
                }
                printDebug(sender, radio);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("simpleradio.use")) options.addAll(PLAYER_COMMANDS);
            if (sender.hasPermission("simpleradio.admin")) options.addAll(ADMIN_COMMANDS);
            return filter(options, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("mode")) return filter(MODE_NAMES, args[1]);
            if (sub.equals("setfreq") || sub.equals("freq") || sub.equals("tune")) {
                return filter(List.of("91.3", "88.0", "100.0"), args[1]);
            }
        }
        return List.of();
    }

    private boolean requireUse(CommandSender sender) {
        if (sender.hasPermission("simpleradio.use")) return true;
        sender.sendMessage(messages.prefix() + " " + messages.get("no-permission"));
        return false;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("simpleradio.admin")) return true;
        sender.sendMessage(messages.prefix() + " " + messages.get("no-permission"));
        return false;
    }

    private RadioMode parseMode(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "transmitter", "transmit", "tx" -> RadioMode.TRANSMITTER;
            case "receiver", "redstone", "redstone_receiver", "rx" -> RadioMode.RECEIVER;
            case "sound", "voice", "sound_receiver" -> RadioMode.SOUND_RECEIVER;
            default -> null;
        };
    }

    private List<String> filter(List<String> options, String partial) {
        String p = partial.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) out.add(o);
        }
        return out;
    }

    private Radio resolveDebugRadio(CommandSender sender, String[] args) {
        if (args.length >= 4) {
            if (!(sender instanceof Player player)) return null;
            try {
                int x = Integer.parseInt(args[1]);
                int y = Integer.parseInt(args[2]);
                int z = Integer.parseInt(args[3]);
                return registry.get(new RadioKey(player.getWorld().getUID(), x, y, z));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (sender instanceof Player player) {
            return gui.getOpenRadio(player);
        }
        return null;
    }

    private void printInfo(CommandSender sender, Radio radio) {
        sender.sendMessage(messages.prefix() + " " + messages.get("info-header"));
        sender.sendMessage(messages.prefix() + " " + messages.get("info-line",
                "%freq%", String.format("%.1f", radio.getFrequency()),
                "%mode%", radio.getMode().getDisplay(),
                "%range%", String.format("%.0f", radio.getMaxRange())));
        if (radio.getMode() == RadioMode.TRANSMITTER) {
            sender.sendMessage(messages.prefix() + " " + messages.get("info-input",
                    "%input%", String.valueOf(radio.getInputStrength())));
        } else {
            sender.sendMessage(messages.prefix() + " " + messages.get("info-signal",
                    "%signal%", String.format("%.0f", radio.getSignalPercent()),
                    "%linked%", radio.isLinked() ? "yes" : "no",
                    "%quality%", radio.getQuality().getDisplay()));
        }
    }

    private void printDebug(CommandSender sender, Radio radio) {
        RadioKey k = radio.getKey();
        sender.sendMessage(messages.prefix() + " " + messages.get("debug-header"));
        sender.sendMessage(messages.prefix() + " " + messages.get("debug-location",
                "%pos%", k.getX() + "," + k.getY() + "," + k.getZ(), "%world%", String.valueOf(k.getWorld())));
        sender.sendMessage(messages.prefix() + " " + messages.get("debug-base",
                "%base%", radio.getBaseMaterial().name(), "%mode%", radio.getMode().getDisplay()));
        sender.sendMessage(messages.prefix() + " " + messages.get("debug-frequency",
                "%freq%", String.format("%.1f", radio.getFrequency())));
        sender.sendMessage(messages.prefix() + " " + messages.get("debug-antenna",
                "%rods%", String.valueOf(radio.getAntenna().getRods()),
                "%copper%", String.valueOf(radio.getAntenna().getCopper()),
                "%netherite%", String.valueOf(radio.getAntenna().getNetherite()),
                "%height%", String.valueOf(radio.getAntennaHeight())));
        sender.sendMessage(messages.prefix() + " " + messages.get("debug-range",
                "%range%", String.format("%.0f", radio.getMaxRange())));
        sender.sendMessage(messages.prefix() + " " + messages.get("debug-linked",
                "%linked%", radio.isLinked() ? "yes" : "no",
                "%signal%", String.format("%.0f", radio.getSignalPercent()),
                "%interference%", radio.isInterference() ? (radio.getTransmitterCount() + " TX") : "no"));
        if (radio.isLinked() && radio.getLinkedSourceKey() != null) {
            RadioKey src = radio.getLinkedSourceKey();
            sender.sendMessage(messages.prefix() + " " + messages.get("debug-source",
                    "%pos%", src.getX() + "," + src.getY() + "," + src.getZ(),
                    "%dist%", String.format("%.0f", radio.getLinkedDistance())));
        }
        sender.sendMessage(messages.prefix() + " " + messages.get("debug-input",
                "%input%", String.valueOf(radio.getInputStrength()),
                "%output%", String.valueOf(radio.getOutputStrength())));

        if (radio.getMode() == RadioMode.TRANSMITTER) {
            printVoiceTest(sender, radio);
        } else {
            printReceiveTest(sender, radio);
        }
    }

    private void printReceiveTest(CommandSender sender, Radio radio) {
        double freq = round(radio.getFrequency());
        double bestSignal = 0;
        Radio bestTx = null;

        for (Radio other : registry.getAll()) {
            if (other.getKey().equals(radio.getKey())) continue;
            if (other.getMode() != RadioMode.TRANSMITTER) continue;
            if (other.getInputStrength() <= 0) continue;
            if (!other.getAntenna().hasRod()) continue;
            if (round(other.getFrequency()) != freq) continue;
            if (other.getWorld() == null || !other.getWorld().equals(radio.getWorld())) continue;

            double dist = distance(radio, other);
            int obstructions = propagation.countObstructions(micTop(radio), micTop(other));
            double effRange = physics.effectiveRange(other.getMaxRange(), radio.getMaxRange(), obstructions);
            if (dist > effRange) continue;
            double sp = physics.signalPercent(dist, effRange);
            if (sp > bestSignal) {
                bestSignal = sp;
                bestTx = other;
            }
        }

        if (bestTx != null && bestSignal > 0) {
            sender.sendMessage(messages.prefix() + " " + messages.get("debug-receive-pass",
                    "%signal%", String.format("%.0f", bestSignal),
                    "%pos%", bestTx.getKey().getX() + "," + bestTx.getKey().getY() + "," + bestTx.getKey().getZ()));
        } else {
            sender.sendMessage(messages.prefix() + " " + messages.get("debug-receive-fail"));
        }
    }

    private void printVoiceTest(CommandSender sender, Radio radio) {
        boolean vcInstalled = Bukkit.getPluginManager().getPlugin("voicechat") != null;
        StringBuilder reasons = new StringBuilder();
        if (!config.voiceEnabled) reasons.append("voice disabled in config; ");
        if (!vcInstalled) reasons.append("Simple Voice Chat not installed; ");
        if (!radio.getAntenna().hasRod()) reasons.append("no antenna rod; ");

        if (reasons.length() == 0) {
            sender.sendMessage(messages.prefix() + " " + messages.get("debug-voice-pass",
                    "%speaker%", radio.hasVoiceUnit() ? "note block" : "antenna top"));
        } else {
            sender.sendMessage(messages.prefix() + " " + messages.get("debug-voice-fail", "%reasons%", reasons.toString()));
        }
    }

    private double distance(Radio a, Radio b) {
        double dx = a.getKey().getX() - b.getKey().getX();
        double dy = a.getKey().getY() - b.getKey().getY();
        double dz = a.getKey().getZ() - b.getKey().getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double round(double f) {
        return Math.round(f * 100.0) / 100.0;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messages.prefix() + " " + messages.get("help-header"));
        sender.sendMessage(messages.get("help-build"));
        sender.sendMessage(messages.get("help-open"));
        sender.sendMessage(messages.get("help-setfreq"));
        sender.sendMessage(messages.get("help-mode"));
        sender.sendMessage(messages.get("help-scan"));
        sender.sendMessage(messages.get("help-info"));
        sender.sendMessage(messages.get("help-voice"));
        if (sender.hasPermission("simpleradio.admin")) {
            sender.sendMessage(messages.get("help-list"));
            sender.sendMessage(messages.get("help-debug"));
            sender.sendMessage(messages.get("help-reload"));
        }
    }
}
