package com.lanvexstudios.model;

public enum RadioMode {
    TRANSMITTER("Transmitter", "Sends redstone input and nearby voice."),
    RECEIVER("Redstone Receiver", "Outputs redstone while it receives."),
    SOUND_RECEIVER("Sound Receiver", "Plays received voice through voice chat.");

    private final String display;
    private final String description;

    RadioMode(String display, String description) {
        this.display = display;
        this.description = description;
    }

    public String getDisplay() {
        return display;
    }

    public String getDescription() {
        return description;
    }

    public RadioMode next() {
        return switch (this) {
            case TRANSMITTER -> RECEIVER;
            case RECEIVER -> SOUND_RECEIVER;
            case SOUND_RECEIVER -> TRANSMITTER;
        };
    }
}
