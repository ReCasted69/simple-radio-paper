package com.lanvexstudios.service;

import com.lanvexstudios.model.Radio;
import com.lanvexstudios.model.RadioKey;
import com.lanvexstudios.model.RadioMode;
import com.lanvexstudios.storage.YamlStore;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RadioRegistry {

    private final YamlStore store;
    private final double defaultFrequency;
    private final Map<RadioKey, Radio> radios = new ConcurrentHashMap<>();

    public RadioRegistry(YamlStore store, double defaultFrequency) {
        this.store = store;
        this.defaultFrequency = defaultFrequency;
    }

    public void loadAll() {
        radios.clear();
        for (Radio radio : store.load()) {
            radios.put(radio.getKey(), radio);
        }
    }

    public void saveAll() {
        store.save(radios.values());
    }

    public Radio get(RadioKey key) {
        return radios.get(key);
    }

    public boolean contains(RadioKey key) {
        return radios.containsKey(key);
    }

    public Radio add(RadioKey key) {
        Radio radio = new Radio(key);
        radio.setFrequency(defaultFrequency);
        radio.setMode(RadioMode.RECEIVER);
        radios.put(key, radio);
        return radio;
    }

    public void remove(RadioKey key) {
        radios.remove(key);
    }

    public Collection<Radio> getAll() {
        return radios.values();
    }

    public int size() {
        return radios.size();
    }
}
