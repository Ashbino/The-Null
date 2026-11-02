package com.thegildedking.thenull;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** Core Null logic: transform, remove, lookups. All changes are saved immediately. */
public final class NullManager {
    private final TheNullPlugin plugin;

    public NullManager(TheNullPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isNull(UUID uuid) {
        return plugin.getDataStorage().get(uuid) != null;
    }

    public NullRecord get(UUID uuid) {
        return plugin.getDataStorage().get(uuid);
    }

    public List<NullRecord> all() {
        return plugin.getDataStorage().all();
    }

    public boolean isIdentityTaken(String identity) {
        return getByIdentity(identity) != null;
    }

    public NullRecord getByIdentity(String identity) {
        for (NullRecord r : plugin.getDataStorage().all()) {
            if (r.getIdentity().equalsIgnoreCase(identity)) {
                return r;
            }
        }
        return null;
    }

    /** The name other players are allowed to see: the identity for Nulls, the real name otherwise. */
    public String publicName(Player p) {
        NullRecord r = get(p.getUniqueId());
        return r != null ? r.getIdentity() : p.getName();
    }

    /**
     * Turns a player into a Null (online or offline).
     * @return the new record, or null if they were already Null.
     */
    public NullRecord transform(UUID uuid, String name, Commander commander) {
        if (isNull(uuid)) {
            return null;
        }
        String identity = plugin.getIdentityGenerator().generate(this);
        NullRecord record = new NullRecord(uuid, name, identity, System.currentTimeMillis());
        plugin.getDataStorage().removePending(uuid);
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            String[] tex = plugin.getSkinManager().capture(online);
            if (tex != null) {
                record.setOriginalTexture(tex[0], tex[1]);
            }
        }
        NullRecord demoted = null;
        if (commander != null) {
            demoted = plugin.getCommanderManager().holder(commander);
            if (demoted != null) {
                demoted.setCommander(null);
            }
            record.setCommander(commander);
        }
        plugin.getDataStorage().put(record);
        plugin.getDataStorage().save();

        if (demoted != null) {
            plugin.getRestorationManager().refreshNametag(demoted.getUuid());
        }
        if (online != null) {
            plugin.getRestorationManager().applyNull(online, record);
            ConfigManager cm = plugin.getConfigManager();
            online.sendMessage(cm.msg("transform-target", Placeholder.unparsed("identity", identity)));
            if (commander != null) {
                online.sendMessage(cm.msg("commander-target-assigned",
                        Placeholder.unparsed("position", plugin.getCommanderManager().title(commander))));
            }
            cm.playSounds(online, "transformation.sounds");
            cm.spawnParticles(online.getLocation().add(0, 1, 0), "transformation.particles");
        }
        return record;
    }

    /**
     * Removes a player from the Null (online or offline).
     * @return the removed record or null if they were not Null.
     */
    public NullRecord remove(UUID uuid) {
        NullRecord record = plugin.getDataStorage().remove(uuid);
        if (record == null) {
            return null;
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online == null) {
            plugin.getDataStorage().addPending(uuid, record.getOriginalName());
        }
        plugin.getDataStorage().save();
        if (online != null) {
            plugin.getRestorationManager().restore(online, record);
            ConfigManager cm = plugin.getConfigManager();
            online.sendMessage(cm.msg("remove-target"));
            cm.playSounds(online, "removal.sounds");
        }
        return record;
    }
}
