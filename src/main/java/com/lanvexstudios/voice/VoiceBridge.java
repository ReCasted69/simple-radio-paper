package com.lanvexstudios.voice;

import com.lanvexstudios.config.Config;
import com.lanvexstudios.model.Radio;
import com.lanvexstudios.model.RadioMode;
import com.lanvexstudios.physics.RadioPhysics;
import com.lanvexstudios.physics.SignalQuality;
import com.lanvexstudios.service.Propagation;
import com.lanvexstudios.service.RadioRegistry;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class VoiceBridge implements VoicechatPlugin {

    private final JavaPlugin plugin;
    private final Config config;
    private final RadioRegistry registry;
    private final RadioPhysics physics;
    private final Propagation propagation;

    private volatile VoicechatServerApi api;

    public VoiceBridge(JavaPlugin plugin, Config config, RadioRegistry registry,
                       RadioPhysics physics, Propagation propagation) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.physics = physics;
        this.propagation = propagation;
    }

    @Override
    public String getPluginId() {
        return "simple_radio";
    }

    @Override
    public void initialize(VoicechatApi api) {
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        this.api = api;
        registerCategory(api);
    }

    private void registerCategory(VoicechatServerApi api) {
        VolumeCategory category = api.volumeCategoryBuilder()
                .setId(config.voiceCategory)
                .setName("Radio")
                .setDescription("Wireless radio voice transmission")
                .build();
        api.registerVolumeCategory(category);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (!config.voiceEnabled) return;
        VoicechatServerApi api = event.getVoicechat();
        if (api == null) api = this.api;
        if (api == null) return;

        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) return;
        ServerPlayer senderPlayer = sender.getPlayer();
        if (senderPlayer == null) return;
        UUID senderUuid = senderPlayer.getUuid();

        Player player = Bukkit.getPlayer(senderUuid);
        if (player == null) return;
        Location playerLoc = player.getLocation();
        World playerWorld = playerLoc.getWorld();
        if (playerWorld == null) return;

        MicrophonePacket packet = event.getPacket();

        for (Radio radio : registry.getAll()) {
            if (radio.getMode() != RadioMode.TRANSMITTER) continue;
            if (!radio.getAntenna().hasRod()) continue;
            if (config.voiceRequireKeyed && radio.getInputStrength() <= 0) continue;
            World rw = radio.getWorld();
            if (rw == null || !rw.equals(playerWorld)) continue;
            double micRadius = Math.min(config.micRadius + radio.getAntennaHeight() * config.voicePickupRadiusScale, config.voicePickupRadiusMax);
            if (distanceSq(playerLoc, radio) > micRadius * micRadius) continue;

            relayFrom(radio, senderUuid, packet, api);
        }
    }

    private void relayFrom(Radio mic, UUID senderUuid, MicrophonePacket packet, VoicechatServerApi api) {
        double freq = round(mic.getFrequency());
        double txRange = mic.getMaxRange();

        for (Radio speaker : registry.getAll()) {
            if (speaker.getKey().equals(mic.getKey())) continue;
            if (speaker.getMode() != RadioMode.SOUND_RECEIVER) continue;
            if (!speaker.getAntenna().hasRod()) continue;

            World sw = speaker.getWorld();
            if (sw == null || !sw.equals(mic.getWorld())) continue;

            double sfreq = round(speaker.getFrequency());
            if (Math.abs(sfreq - freq) > 0.005) continue;

            double dist = distance(mic, speaker);
            int obstructions = propagation.countObstructions(micTop(mic), micTop(speaker));
            double effRange = physics.effectiveRange(txRange, speaker.getMaxRange(), obstructions);
            if (dist > effRange) continue;

            double signalPercent = physics.signalPercent(dist, effRange);
            SignalQuality quality = SignalQuality.fromPercent(signalPercent, config);

            double range = config.speakerRange;
            if (config.voiceAttenuateBySignal) {
                range *= Math.max(0.1, signalPercent / 100.0);
            }
            if (config.voiceCrackleBySignal && quality == SignalQuality.STATIC) {
                range *= 0.5;
                if (ThreadLocalRandom.current().nextDouble() < 0.3) continue;
            }
            range *= Math.max(0.0, config.voiceVolume);
            if (range <= 0) continue;

            Location speakerLocation = speaker.getNoteLocation();
            if (speakerLocation == null) speakerLocation = micTop(speaker);
            if (speakerLocation == null) continue;

            Position pos = api.createPosition(speakerLocation.getX(), speakerLocation.getY(), speakerLocation.getZ());
            ServerLevel level = api.fromServerLevel(sw);

            var listeners = api.getPlayersInRange(level, pos, range);
            if (listeners.isEmpty()) continue;

            UUID channelId = UUID.nameUUIDFromBytes(
                    (speaker.getKey().toStoreKey() + ":" + senderUuid).getBytes(StandardCharsets.UTF_8));

            LocationalSoundPacket.Builder<?> builder = packet.locationalSoundPacketBuilder();
            builder.position(pos);
            builder.distance((float) range);
            builder.category(config.voiceCategory);
            builder.channelId(channelId);
            LocationalSoundPacket relayed = builder.build();

            for (ServerPlayer sp : listeners) {
                if (!config.voiceHearOwnTransmission && sp.getUuid().equals(senderUuid)) continue;
                VoicechatConnection connection = api.getConnectionOf(sp);
                if (connection != null) {
                    api.sendLocationalSoundPacketTo(connection, relayed);
                }
            }
        }
    }

    private double distanceSq(Location loc, Radio radio) {
        double dx = loc.getX() - (radio.getKey().getX() + 0.5);
        double dy = loc.getY() - (radio.getKey().getY() + 0.5);
        double dz = loc.getZ() - (radio.getKey().getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz;
    }

    private Location micTop(Radio radio) {
        World world = radio.getWorld();
        if (world == null) return null;
        return new Location(world, radio.getKey().getX() + 0.5,
                radio.getKey().getY() + radio.getAntennaHeight(),
                radio.getKey().getZ() + 0.5);
    }

    private double distance(Radio a, Radio b) {
        double dx = (a.getKey().getX() - b.getKey().getX());
        double dy = (a.getKey().getY() - b.getKey().getY());
        double dz = (a.getKey().getZ() - b.getKey().getZ());
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double round(double f) {
        return Math.round(f * 100.0) / 100.0;
    }
}
