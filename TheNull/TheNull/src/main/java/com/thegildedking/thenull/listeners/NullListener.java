package com.thegildedking.thenull.listeners;

import com.thegildedking.thenull.TheNullPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

/** Join/quit/death handling for Nulls and the Totem of Null pop. */
public final class NullListener implements Listener {
    private final TheNullPlugin plugin;

    public NullListener(TheNullPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.getRestorationManager().handleJoin(p);
        if (plugin.getNullManager().isNull(p.getUniqueId())) {
            e.joinMessage(Component.translatable("multiplayer.player.joined", NamedTextColor.YELLOW, p.displayName()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.getInvitationManager().cancelFor(p.getUniqueId());
        plugin.getNametagManager().removeLabels(p);
        if (plugin.getNullManager().isNull(p.getUniqueId())) {
            e.quitMessage(Component.translatable("multiplayer.player.left", NamedTextColor.YELLOW, p.displayName()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        plugin.getNametagManager().removeLabels(p);
        if (plugin.getNullManager().isNull(p.getUniqueId())
                && plugin.getConfig().getBoolean("nametag.hide-real-name-in-death-messages", true)) {
            e.deathMessage(Component.translatable("death.attack.generic", p.displayName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        relabel(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        relabel(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        relabel(e.getPlayer());
    }

    private void relabel(Player p) {
        if (plugin.getNullManager().isNull(p.getUniqueId())) {
            plugin.getNametagManager().removeLabels(p);
            plugin.getNametagManager().scheduleRefresh(p);
        }
    }

    /** The Totem of Null popped: Null Erasure. Fires once per pop (one EntityResurrectEvent). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent e) {
        if (!(e.getEntity() instanceof Player holder)) {
            return;
        }
        if (!usedNullTotem(holder, e.getHand())) {
            return;
        }
        // One tick later so vanilla totem effects are finished and the holder (if Null) still dies.
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getTotemManager().erase(holder));
    }

    /** Mirrors vanilla: the main hand is checked first, then the off hand. */
    private boolean usedNullTotem(Player p, EquipmentSlot eventHand) {
        if (eventHand != null) {
            return plugin.getTotemManager().isTotem(p.getInventory().getItem(eventHand));
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HAND, EquipmentSlot.OFF_HAND}) {
            ItemStack item = p.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                return plugin.getTotemManager().isTotem(item);
            }
        }
        return false;
    }
}
