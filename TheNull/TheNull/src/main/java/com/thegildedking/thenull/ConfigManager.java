package com.thegildedking.thenull;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Loads config.yml and offers MiniMessage / sound / particle helpers. */
public final class ConfigManager {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final TheNullPlugin plugin;

    public ConfigManager(TheNullPlugin plugin) {
        this.plugin = plugin;
    }

    /** Re-reads config.yml. Missing keys fall back to the defaults bundled in the jar. */
    public void reload() {
        plugin.reloadConfig();
    }

    public FileConfiguration cfg() {
        return plugin.getConfig();
    }

    public Component parse(String raw, TagResolver... resolvers) {
        return MM.deserialize(raw == null ? "" : raw, resolvers);
    }

    /** Parses text for item names/lore: not italic unless the text asks for it. */
    public Component parseItem(String raw, TagResolver... resolvers) {
        return parse(raw, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public List<Component> parseItemList(String path, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : cfg().getStringList(path)) {
            out.add(parseItem(line, resolvers));
        }
        return out;
    }

    /** A message from the messages section, with the prefix in front. */
    public Component msg(String key, TagResolver... resolvers) {
        String prefix = cfg().getString("prefix", "");
        String text = cfg().getString("messages." + key, "<red>Missing message: " + key);
        return parse(prefix + text, resolvers);
    }

    /** A message from the messages section without the prefix (titles, broadcasts). */
    public Component rawMsg(String key, TagResolver... resolvers) {
        return parse(cfg().getString("messages." + key, "<red>Missing message: " + key), resolvers);
    }

    public Material material(String path, Material def) {
        String name = cfg().getString(path, def.name());
        Material m = Material.matchMaterial(name == null ? "" : name);
        if (m == null || !m.isItem()) {
            plugin.getLogger().warning("Invalid material '" + name + "' at " + path + ", using " + def);
            return def;
        }
        return m;
    }

    /** Sound entries look like "namespace:key;volume;pitch" (namespace optional). */
    public void playSounds(Audience audience, String path) {
        for (String spec : cfg().getStringList(path)) {
            try {
                String[] p = spec.split(";");
                Key key = Key.key(p[0].trim().toLowerCase(Locale.ROOT));
                float volume = p.length > 1 ? Float.parseFloat(p[1].trim()) : 1.0f;
                float pitch = p.length > 2 ? Float.parseFloat(p[2].trim()) : 1.0f;
                audience.playSound(Sound.sound(key, Sound.Source.MASTER, volume, pitch));
            } catch (Exception ex) {
                plugin.getLogger().warning("Invalid sound entry at " + path + ": " + spec);
            }
        }
    }

    /** Particle entries look like "PARTICLE_NAME;count;spread;speed". */
    public void spawnParticles(Location loc, String path) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        for (String spec : cfg().getStringList(path)) {
            try {
                String[] p = spec.split(";");
                Particle particle = Particle.valueOf(p[0].trim().toUpperCase(Locale.ROOT));
                int count = p.length > 1 ? Integer.parseInt(p[1].trim()) : 20;
                double spread = p.length > 2 ? Double.parseDouble(p[2].trim()) : 0.5;
                double speed = p.length > 3 ? Double.parseDouble(p[3].trim()) : 0.02;
                world.spawnParticle(particle, loc, count, spread, spread, spread, speed);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid or unsupported particle entry at " + path + ": " + spec);
            }
        }
    }
}
