package com.thegildedking.thenull;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Applies and restores the Null appearance (skin + nametag), including offline restoration on join. */
public final class RestorationManager {
    private final TheNullPlugin plugin;

    public RestorationManager(TheNullPlugin plugin) {
        this.plugin = plugin;
    }

    public void applyNull(Player p, NullRecord r) {
        plugin.getSkinManager().applyBlack(p);
        plugin.getNametagManager().apply(p, r);
    }

    /** Re-applies only the nametag (e.g. after a commander change). */
    public void refreshNametag(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        NullRecord r = plugin.getNullManager().get(uuid);
        if (p != null && r != null) {
            plugin.getNametagManager().apply(p, r);
        }
    }

    /** Restores the original skin and nametag of an online player who was just un-Nulled. */
    public void restore(Player p, NullRecord removed) {
        boolean skinKnown = plugin.getSkinManager().restoreOriginal(p, removed);
        plugin.getNametagManager().clear(p);
        if (!skinKnown) {
            p.sendMessage(plugin.getConfigManager().msg("skin-restore-relog"));
        }
    }

    public void handleJoin(Player p) {
        UUID uuid = p.getUniqueId();
        NullRecord r = plugin.getNullManager().get(uuid);
        if (r != null) {
            boolean changed = false;
            // At join the profile is fresh from Mojang, so it holds the genuine original skin.
            String[] tex = plugin.getSkinManager().capture(p);
            if (tex != null && (!tex[0].equals(r.getOriginalTextureValue()))) {
                r.setOriginalTexture(tex[0], tex[1]);
                changed = true;
            }
            if (!p.getName().equals(r.getOriginalName())) {
                r.setOriginalName(p.getName());
                changed = true;
            }
            if (changed) {
                plugin.getDataStorage().save();
            }
            applyNull(p, r);
            // Apply the skin again shortly after join so every client picks it up reliably.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline() && plugin.getNullManager().isNull(uuid)) {
                    plugin.getSkinManager().applyBlack(p);
                }
            }, 10L);
        } else {
            // Not Null: make sure no leftover nametag state exists.
            plugin.getNametagManager().clearLeftovers(p);
            if (plugin.getDataStorage().removePending(uuid)) {
                plugin.getDataStorage().save();
                p.sendMessage(plugin.getConfigManager().msg("restored-on-join"));
            }
        }
    }
}
