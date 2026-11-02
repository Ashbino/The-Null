package com.thegildedking.thenull;

import java.util.EnumMap;
import java.util.Map;

/** The four commander positions. Holder data lives in the NullRecords so it persists with them. */
public final class CommanderManager {
    private final TheNullPlugin plugin;

    public CommanderManager(TheNullPlugin plugin) {
        this.plugin = plugin;
    }

    public NullRecord holder(Commander position) {
        for (NullRecord r : plugin.getDataStorage().all()) {
            if (r.getCommander() == position) {
                return r;
            }
        }
        return null;
    }

    public Map<Commander, NullRecord> holders() {
        Map<Commander, NullRecord> map = new EnumMap<>(Commander.class);
        for (Commander c : Commander.values()) {
            map.put(c, holder(c));
        }
        return map;
    }

    public String title(Commander position) {
        return plugin.getConfig().getString("commanders." + position.key() + ".title", position.name());
    }

    /**
     * Assigns a position to a Null. If someone else holds it they are demoted to an ordinary Null.
     * If the Null held another position, that position becomes free.
     * @return the replaced holder or null
     */
    public NullRecord assign(NullRecord target, Commander position) {
        NullRecord previous = holder(position);
        if (previous != null && previous != target) {
            previous.setCommander(null);
        } else {
            previous = null;
        }
        target.setCommander(position);
        plugin.getDataStorage().save();
        if (previous != null) {
            plugin.getRestorationManager().refreshNametag(previous.getUuid());
        }
        plugin.getRestorationManager().refreshNametag(target.getUuid());
        return previous;
    }

    /** Frees a position. @return the former holder or null if it was empty. */
    public NullRecord clear(Commander position) {
        NullRecord holder = holder(position);
        if (holder == null) {
            return null;
        }
        holder.setCommander(null);
        plugin.getDataStorage().save();
        plugin.getRestorationManager().refreshNametag(holder.getUuid());
        return holder;
    }
}
