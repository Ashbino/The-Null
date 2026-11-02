package com.thegildedking.thenull;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * YAML persistence (plugins/TheNull/data.yml). Every change is written immediately using a
 * temp file + atomic move, and the previous version is kept as data.yml.bak.
 */
public final class DataStorage {
    private final TheNullPlugin plugin;
    private final File file;
    private final File backup;
    private final File tmp;

    private final Map<UUID, NullRecord> records = new LinkedHashMap<>();
    /** Players removed while offline: uuid -> original name. Cleaned up when they next join. */
    private final Map<UUID, String> pendingRestore = new LinkedHashMap<>();

    public DataStorage(TheNullPlugin plugin) {
        this.plugin = plugin;
        File dir = plugin.getDataFolder();
        this.file = new File(dir, "data.yml");
        this.backup = new File(dir, "data.yml.bak");
        this.tmp = new File(dir, "data.yml.tmp");
    }

    public synchronized boolean load() {
        records.clear();
        pendingRestore.clear();
        if (!file.exists()) {
            return true;
        }
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.load(file);
        } catch (Exception ex) {
            plugin.getLogger().severe("data.yml could not be read: " + ex.getMessage());
            if (!backup.exists()) {
                return false;
            }
            y = new YamlConfiguration();
            try {
                y.load(backup);
                plugin.getLogger().warning("Loaded data from data.yml.bak instead.");
            } catch (Exception ex2) {
                plugin.getLogger().severe("data.yml.bak could not be read either: " + ex2.getMessage());
                return false;
            }
        }
        ConfigurationSection nulls = y.getConfigurationSection("nulls");
        if (nulls != null) {
            for (String key : nulls.getKeys(false)) {
                ConfigurationSection s = nulls.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                try {
                    UUID uuid = UUID.fromString(key);
                    String identity = s.getString("identity");
                    if (identity == null || identity.isBlank()) {
                        continue;
                    }
                    NullRecord r = new NullRecord(uuid, s.getString("original-name", "unknown"),
                            identity, s.getLong("since", System.currentTimeMillis()));
                    r.setCommander(Commander.parse(s.getString("commander")));
                    r.setOriginalTexture(s.getString("original-texture-value"),
                            s.getString("original-texture-signature"));
                    records.put(uuid, r);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Skipping invalid entry in data.yml: " + key);
                }
            }
        }
        ConfigurationSection pending = y.getConfigurationSection("pending-restore");
        if (pending != null) {
            for (String key : pending.getKeys(false)) {
                try {
                    pendingRestore.put(UUID.fromString(key), pending.getString(key, "unknown"));
                } catch (IllegalArgumentException ignored) {
                    // skip
                }
            }
        }
        return true;
    }

    public synchronized boolean save() {
        YamlConfiguration y = new YamlConfiguration();
        for (NullRecord r : records.values()) {
            String base = "nulls." + r.getUuid();
            y.set(base + ".original-name", r.getOriginalName());
            y.set(base + ".identity", r.getIdentity());
            y.set(base + ".since", r.getSince());
            if (r.getCommander() != null) {
                y.set(base + ".commander", r.getCommander().name());
            }
            if (r.getOriginalTextureValue() != null) {
                y.set(base + ".original-texture-value", r.getOriginalTextureValue());
                if (r.getOriginalTextureSignature() != null) {
                    y.set(base + ".original-texture-signature", r.getOriginalTextureSignature());
                }
            }
        }
        for (Map.Entry<UUID, String> e : pendingRestore.entrySet()) {
            y.set("pending-restore." + e.getKey(), e.getValue());
        }
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
            Path tmpPath = tmp.toPath();
            Files.writeString(tmpPath, y.saveToString(), StandardCharsets.UTF_8);
            if (file.exists()) {
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(tmpPath, file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmpPath, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException ex) {
            plugin.getLogger().severe("FAILED TO SAVE data.yml: " + ex.getMessage());
            return false;
        }
    }

    public synchronized NullRecord get(UUID uuid) {
        return records.get(uuid);
    }

    public synchronized void put(NullRecord r) {
        records.put(r.getUuid(), r);
    }

    public synchronized NullRecord remove(UUID uuid) {
        return records.remove(uuid);
    }

    public synchronized List<NullRecord> all() {
        return new ArrayList<>(records.values());
    }

    public synchronized boolean addPending(UUID uuid, String name) {
        return pendingRestore.put(uuid, name) == null;
    }

    public synchronized boolean removePending(UUID uuid) {
        return pendingRestore.remove(uuid) != null;
    }

    public synchronized boolean hasPending(UUID uuid) {
        return pendingRestore.containsKey(uuid);
    }

    public synchronized Collection<String> pendingNames() {
        return new ArrayList<>(pendingRestore.values());
    }
}
