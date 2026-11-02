package com.thegildedking.thenull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** The Totem of Null item and its Null Erasure ability. */
public final class TotemManager {
    private final TheNullPlugin plugin;
    private final NamespacedKey totemKey;

    public TotemManager(TheNullPlugin plugin) {
        this.plugin = plugin;
        this.totemKey = new NamespacedKey(plugin, "totem_of_null");
    }

    public ItemStack createTotem() {
        ConfigManager cm = plugin.getConfigManager();
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(cm.parseItem(plugin.getConfig().getString("totem.name", "<dark_purple><bold>The Totem of Null")));
        meta.lore(cm.parseItemList("totem.lore"));
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        if (plugin.getConfig().getBoolean("totem.hide-enchantments", true)) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.getPersistentDataContainer().set(totemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(totemKey, PersistentDataType.BYTE);
    }

    /** Runs Null Erasure. Called one tick after the totem popped (see NullListener). */
    public void erase(Player holder) {
        ConfigManager cm = plugin.getConfigManager();
        String holderName = plugin.getNullManager().publicName(holder);

        if (plugin.getConfig().getBoolean("announcements.enabled", true)) {
            Component chat = cm.rawMsg("totem-announcement", Placeholder.unparsed("player", holderName));
            Bukkit.getServer().sendMessage(chat);
            Component title = cm.rawMsg("totem-title");
            Component subtitle = cm.rawMsg("totem-subtitle", Placeholder.unparsed("player", holderName));
            Bukkit.getServer().showTitle(Title.title(title, subtitle));
        }
        cm.playSounds(Bukkit.getServer(), "totem.sounds");

        List<Player> victims = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getNullManager().isNull(p.getUniqueId()) && !p.isDead()) {
                victims.add(p);
            }
        }
        for (Player victim : victims) {
            cm.spawnParticles(victim.getLocation().add(0, 1, 0), "totem.particles");
            // setHealth(0) kills directly and cannot be stopped by another totem.
            victim.setHealth(0.0);
        }
    }
}
