package com.thegildedking.thenull.listeners;

import com.thegildedking.thenull.InvitationGui;
import com.thegildedking.thenull.TheNullPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Right-click invitations and the GUI clicks. */
public final class InvitationListener implements Listener {
    private final TheNullPlugin plugin;

    public InvitationListener(TheNullPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player target)) {
            return;
        }
        Player holder = e.getPlayer();
        EquipmentSlot hand = e.getHand();
        if (!plugin.getTotemManager().isTotem(holder.getInventory().getItem(hand))) {
            return;
        }
        e.setCancelled(true);
        if (target.hasMetadata("NPC") || target.getUniqueId().equals(holder.getUniqueId())) {
            return;
        }
        // The same click can reach us twice (interact + interact-at, or both hands): ignore repeats.
        if (plugin.getInvitationManager().isInvitationFrom(target.getUniqueId(), holder.getUniqueId())) {
            return;
        }
        plugin.getInvitationManager().invite(holder, target);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof InvitationGui gui)) {
            return;
        }
        // Nothing in this GUI may ever be moved, taken, swapped or shift-clicked.
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player clicker)) {
            return;
        }
        if (!clicker.getUniqueId().equals(gui.getTarget())) {
            return; // only the targeted player may answer
        }
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }
        ClickType click = e.getClick();
        if (click != ClickType.LEFT && click != ClickType.RIGHT) {
            return;
        }
        int slot = e.getSlot();
        if (gui.isYes(slot)) {
            plugin.getInvitationManager().accept(clicker);
        } else if (gui.isNo(slot)) {
            plugin.getInvitationManager().decline(clicker, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof InvitationGui) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof InvitationGui gui)) {
            return;
        }
        if (!(e.getPlayer() instanceof Player p) || !p.getUniqueId().equals(gui.getTarget())) {
            return;
        }
        // Accept/decline/expire remove the invitation before closing, so this only fires for ESC.
        if (plugin.getInvitationManager().hasInvitation(p.getUniqueId())
                && plugin.getConfig().getBoolean("invitation.closing-counts-as-decline", true)) {
            plugin.getInvitationManager().decline(p, true);
        }
    }
}
