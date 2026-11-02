package com.thegildedking.thenull;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pending Totem invitations. At most one per target; the target alone decides. */
public final class InvitationManager {
    private static final class Invitation {
        final UUID inviter;
        final UUID target;
        BukkitTask expiry;

        Invitation(UUID inviter, UUID target) {
            this.inviter = inviter;
            this.target = target;
        }
    }

    private final TheNullPlugin plugin;
    private final Map<UUID, Invitation> byTarget = new HashMap<>();

    public InvitationManager(TheNullPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasInvitation(UUID target) {
        return byTarget.containsKey(target);
    }

    public boolean isInvitationFrom(UUID target, UUID inviter) {
        Invitation inv = byTarget.get(target);
        return inv != null && inv.inviter.equals(inviter);
    }

    public void invite(Player inviter, Player target) {
        ConfigManager cm = plugin.getConfigManager();
        if (plugin.getNullManager().isNull(target.getUniqueId())) {
            inviter.sendMessage(cm.msg("invite-target-already-null",
                    Placeholder.unparsed("player", plugin.getNullManager().publicName(target))));
            return;
        }
        if (byTarget.containsKey(target.getUniqueId())) {
            inviter.sendMessage(cm.msg("invite-target-pending",
                    Placeholder.unparsed("player", plugin.getNullManager().publicName(target))));
            return;
        }
        Invitation inv = new Invitation(inviter.getUniqueId(), target.getUniqueId());
        int seconds = Math.max(5, plugin.getConfig().getInt("invitation.expire-seconds", 30));
        inv.expiry = Bukkit.getScheduler().runTaskLater(plugin, () -> expire(target.getUniqueId()), seconds * 20L);
        byTarget.put(target.getUniqueId(), inv);

        target.openInventory(InvitationGui.create(plugin, inviter.getUniqueId(), target.getUniqueId()).getInventory());
        target.sendMessage(cm.msg("invite-received",
                Placeholder.unparsed("player", plugin.getNullManager().publicName(inviter)),
                Placeholder.unparsed("seconds", String.valueOf(seconds))));
        cm.playSounds(target, "invitation.sounds.received");
        inviter.sendMessage(cm.msg("invite-sent",
                Placeholder.unparsed("player", plugin.getNullManager().publicName(target)),
                Placeholder.unparsed("seconds", String.valueOf(seconds))));
        cm.playSounds(inviter, "invitation.sounds.sent");
    }

    private Invitation take(UUID target) {
        Invitation inv = byTarget.remove(target);
        if (inv != null && inv.expiry != null) {
            inv.expiry.cancel();
        }
        return inv;
    }

    private void closeGui(Player target) {
        if (target != null && target.getOpenInventory().getTopInventory().getHolder() instanceof InvitationGui) {
            Bukkit.getScheduler().runTask(plugin, target::closeInventory);
        }
    }

    public void accept(Player target) {
        Invitation inv = take(target.getUniqueId());
        if (inv == null) {
            return;
        }
        closeGui(target);
        ConfigManager cm = plugin.getConfigManager();
        Player inviter = Bukkit.getPlayer(inv.inviter);
        if (plugin.getNullManager().isNull(target.getUniqueId())) {
            return;
        }
        NullRecord record = plugin.getNullManager().transform(target.getUniqueId(), target.getName(), null);
        if (record == null) {
            return;
        }
        target.sendMessage(cm.msg("invite-accepted-target"));
        if (inviter != null) {
            inviter.sendMessage(cm.msg("invite-accepted-inviter",
                    Placeholder.unparsed("player", target.getName()),
                    Placeholder.unparsed("identity", record.getIdentity())));
            cm.playSounds(inviter, "invitation.sounds.accepted");
        }
    }

    /** @param silentClose true if the player just closed the GUI without clicking NO */
    public void decline(Player target, boolean silentClose) {
        Invitation inv = take(target.getUniqueId());
        if (inv == null) {
            return;
        }
        if (!silentClose) {
            closeGui(target);
        }
        ConfigManager cm = plugin.getConfigManager();
        target.sendMessage(cm.msg("invite-declined-target"));
        Player inviter = Bukkit.getPlayer(inv.inviter);
        if (inviter != null) {
            inviter.sendMessage(cm.msg("invite-declined-inviter",
                    Placeholder.unparsed("player", target.getName())));
            cm.playSounds(inviter, "invitation.sounds.declined");
        }
    }

    private void expire(UUID targetId) {
        Invitation inv = take(targetId);
        if (inv == null) {
            return;
        }
        ConfigManager cm = plugin.getConfigManager();
        Player target = Bukkit.getPlayer(targetId);
        Player inviter = Bukkit.getPlayer(inv.inviter);
        closeGui(target);
        if (target != null) {
            target.sendMessage(cm.msg("invite-expired-target"));
        }
        if (inviter != null) {
            inviter.sendMessage(cm.msg("invite-expired-inviter",
                    Placeholder.unparsed("player", target != null ? target.getName() : "?")));
        }
    }

    /** Cancels every invitation involving this player (called when they disconnect). */
    public void cancelFor(UUID player) {
        for (Invitation inv : new ArrayList<>(byTarget.values())) {
            if (!inv.target.equals(player) && !inv.inviter.equals(player)) {
                continue;
            }
            take(inv.target);
            ConfigManager cm = plugin.getConfigManager();
            UUID otherId = inv.target.equals(player) ? inv.inviter : inv.target;
            Player other = Bukkit.getPlayer(otherId);
            if (other != null) {
                closeGui(other);
                other.sendMessage(cm.msg("invite-cancelled"));
            }
        }
    }

    public void cancelAll() {
        for (UUID target : new ArrayList<>(byTarget.keySet())) {
            take(target);
            Player p = Bukkit.getPlayer(target);
            if (p != null && p.getOpenInventory().getTopInventory().getHolder() instanceof InvitationGui) {
                p.closeInventory();
            }
        }
    }
}
