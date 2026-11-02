package com.thegildedking.thenull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nametags. The real vanilla nametag is hidden with a scoreboard team (NAME_TAG_VISIBILITY NEVER)
 * and replaced by a TextDisplay riding the player that shows the Null identity. The tab list
 * name and display name (chat) are also replaced.
 */
public final class NametagManager {
    private static final String TEAM_NAME = "thenull_hidden";

    private final TheNullPlugin plugin;
    private final NamespacedKey labelKey;
    private final Map<UUID, UUID> registeredLabels = new HashMap<>();
    private Team team;
    private BukkitTask task;

    public NametagManager(TheNullPlugin plugin) {
        this.plugin = plugin;
        this.labelKey = new NamespacedKey(plugin, "null_label");
    }

    public void start() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team old = sb.getTeam(TEAM_NAME);
        if (old != null) {
            old.unregister();
        }
        team = sb.registerNewTeam(TEAM_NAME);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeLabels(p);
        }
        if (team != null) {
            try {
                team.unregister();
            } catch (IllegalStateException ignored) {
                // already unregistered
            }
            team = null;
        }
    }

    /** The public name component of a Null (identity, with commander formatting). */
    public Component nameComponent(NullRecord r) {
        String format;
        String title = "";
        if (r.getCommander() != null) {
            String key = "commanders." + r.getCommander().key();
            title = plugin.getConfig().getString(key + ".title", r.getCommander().name());
            format = plugin.getConfig().getString(key + ".format", "<gold><bold><identity>");
        } else {
            format = plugin.getConfig().getString("nametag.null-format", "<dark_purple><bold><identity>");
        }
        return plugin.getConfigManager().parse(format,
                Placeholder.unparsed("identity", r.getIdentity()),
                Placeholder.unparsed("title", title));
    }

    public void apply(Player p, NullRecord r) {
        Component name = nameComponent(r);
        if (team != null && !team.hasEntry(p.getName())) {
            team.addEntry(p.getName());
        }
        p.playerListName(name);
        p.displayName(name);
        updateLabel(p, name);
    }

    public void clear(Player p) {
        if (team != null && team.hasEntry(p.getName())) {
            team.removeEntry(p.getName());
        }
        p.playerListName(null);
        p.displayName(null);
        removeLabels(p);
    }

    /** For players who are not Null: only undo leftovers (e.g. after a crash), never touch other plugins' names. */
    public void clearLeftovers(Player p) {
        if (team != null && team.hasEntry(p.getName())) {
            clear(p);
        } else {
            removeLabels(p);
        }
    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            NullRecord r = plugin.getNullManager().get(p.getUniqueId());
            if (r == null) {
                continue;
            }
            if (team != null && !team.hasEntry(p.getName())) {
                team.addEntry(p.getName());
            }
            if (!p.isDead()) {
                updateLabel(p, nameComponent(r));
            }
        }
    }

    private TextDisplay findLabel(Player p) {
        for (Entity e : p.getPassengers()) {
            if (e instanceof TextDisplay td && td.getPersistentDataContainer().has(labelKey, PersistentDataType.BYTE)) {
                return td;
            }
        }
        return null;
    }

    private void updateLabel(Player p, Component name) {
        TextDisplay label = findLabel(p);
        if (label != null && label.isValid()) {
            if (!name.equals(label.text())) {
                label.text(name);
            }
            return;
        }
        removeRegistered(p.getUniqueId());
        float height = (float) plugin.getConfig().getDouble("nametag.label-height", 0.35);
        float scale = (float) plugin.getConfig().getDouble("nametag.label-scale", 1.0);
        boolean seeThrough = plugin.getConfig().getBoolean("nametag.label-see-through", false);
        TextDisplay created = p.getWorld().spawn(p.getLocation(), TextDisplay.class, d -> {
            d.text(name);
            d.setBillboard(Display.Billboard.CENTER);
            d.setPersistent(false);
            d.setSeeThrough(seeThrough);
            d.setShadowed(true);
            d.setViewRange(1.0f);
            d.setTransformation(new Transformation(new Vector3f(0f, height, 0f), new AxisAngle4f(),
                    new Vector3f(scale, scale, scale), new AxisAngle4f()));
            d.getPersistentDataContainer().set(labelKey, PersistentDataType.BYTE, (byte) 1);
        });
        p.addPassenger(created);
        p.hideEntity(plugin, created); // the owner does not need to see their own label
        registeredLabels.put(p.getUniqueId(), created.getUniqueId());
    }

    private void removeRegistered(UUID owner) {
        UUID id = registeredLabels.remove(owner);
        if (id != null) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) {
                e.remove();
            }
        }
    }

    public void removeLabels(Player p) {
        for (Entity e : new java.util.ArrayList<>(p.getPassengers())) {
            if (e instanceof TextDisplay td && td.getPersistentDataContainer().has(labelKey, PersistentDataType.BYTE)) {
                td.remove();
            }
        }
        removeRegistered(p.getUniqueId());
    }

    /** Called after teleports/respawns, which dismount passengers. */
    public void scheduleRefresh(Player p) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || p.isDead()) {
                return;
            }
            NullRecord r = plugin.getNullManager().get(p.getUniqueId());
            if (r != null) {
                updateLabel(p, nameComponent(r));
            }
        }, 3L);
    }

    public void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            NullRecord r = plugin.getNullManager().get(p.getUniqueId());
            if (r != null) {
                apply(p, r);
            }
        }
    }
}
