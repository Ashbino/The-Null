package com.thegildedking.thenull;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

/**
 * Real skin replacement. Uses Paper's Player#setPlayerProfile, which swaps the profile
 * "textures" property and re-registers the player to every client that can see them.
 * Minecraft only accepts textures hosted on Mojang servers, so the black skin must be a
 * Mojang-hosted (and ideally signed) texture: see README, "Black skin setup".
 */
public final class SkinManager {
    private final TheNullPlugin plugin;
    private final File skinFile;
    private String blackValue;
    private String blackSignature;

    public SkinManager(TheNullPlugin plugin) {
        this.plugin = plugin;
        this.skinFile = new File(plugin.getDataFolder(), "skin.yml");
        reload();
    }

    public void reload() {
        String v = plugin.getConfig().getString("skin.texture-value", "");
        String s = plugin.getConfig().getString("skin.texture-signature", "");
        if (v == null || v.isBlank()) {
            if (skinFile.exists()) {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(skinFile);
                v = y.getString("texture-value", "");
                s = y.getString("texture-signature", "");
            }
        }
        blackValue = (v == null || v.isBlank()) ? null : v.trim();
        blackSignature = (s == null || s.isBlank()) ? null : s.trim();
    }

    public boolean isConfigured() {
        return plugin.getConfig().getBoolean("skin.enabled", true) && blackValue != null;
    }

    /** Reads the textures property of the player's current profile: {value, signature}. */
    public String[] capture(Player p) {
        for (ProfileProperty prop : p.getPlayerProfile().getProperties()) {
            if ("textures".equals(prop.getName())) {
                return new String[] {prop.getValue(), prop.getSignature()};
            }
        }
        return null;
    }

    /** Saves the textures of this player as the black skin (stored in skin.yml). */
    public boolean saveFromPlayer(Player p) {
        String[] tex = capture(p);
        if (tex == null) {
            return false;
        }
        YamlConfiguration y = new YamlConfiguration();
        y.set("texture-value", tex[0]);
        y.set("texture-signature", tex[1] == null ? "" : tex[1]);
        try {
            plugin.getDataFolder().mkdirs();
            y.save(skinFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not write skin.yml: " + ex.getMessage());
            return false;
        }
        blackValue = tex[0];
        blackSignature = tex[1] == null || tex[1].isBlank() ? null : tex[1];
        return true;
    }

    /** @return true if a black skin was applied. */
    public boolean applyBlack(Player p) {
        if (!isConfigured()) {
            return false;
        }
        PlayerProfile profile = p.getPlayerProfile();
        profile.setProperty(new ProfileProperty("textures", blackValue, blackSignature));
        p.setPlayerProfile(profile);
        return true;
    }

    /** @return true if the original skin was restored, false if it was not known. */
    public boolean restoreOriginal(Player p, NullRecord record) {
        PlayerProfile profile = p.getPlayerProfile();
        boolean known = record != null && record.getOriginalTextureValue() != null;
        if (known) {
            profile.setProperty(new ProfileProperty("textures", record.getOriginalTextureValue(),
                    record.getOriginalTextureSignature()));
        } else {
            profile.removeProperty("textures");
        }
        p.setPlayerProfile(profile);
        return known;
    }
}
