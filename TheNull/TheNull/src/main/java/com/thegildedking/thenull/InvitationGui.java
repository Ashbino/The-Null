package com.thegildedking.thenull;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** The dark-themed "DO YOU WISH TO BECOME NULL?" chest GUI (5 rows, big YES / NO blocks). */
public final class InvitationGui implements InventoryHolder {
    public static final int ROWS = 5;
    private static final int[] YES_COLUMNS = {1, 2, 3};
    private static final int[] NO_COLUMNS = {5, 6, 7};

    private final UUID inviter;
    private final UUID target;
    private final Set<Integer> yesSlots = new HashSet<>();
    private final Set<Integer> noSlots = new HashSet<>();
    private Inventory inventory;

    private InvitationGui(UUID inviter, UUID target) {
        this.inviter = inviter;
        this.target = target;
    }

    public static InvitationGui create(TheNullPlugin plugin, UUID inviter, UUID target) {
        InvitationGui gui = new InvitationGui(inviter, target);
        ConfigManager cm = plugin.getConfigManager();
        Component title = cm.parse(plugin.getConfig().getString("invitation.gui.title",
                "<dark_purple><bold>DO YOU WISH TO BECOME NULL?"));
        Inventory inv = Bukkit.createInventory(gui, ROWS * 9, title);
        gui.inventory = inv;

        ItemStack filler = button(cm, Material.BLACK_STAINED_GLASS_PANE, "invitation.gui.filler-material", " ", null);
        ItemStack border = button(cm, Material.PURPLE_STAINED_GLASS_PANE, "invitation.gui.border-material", " ", null);
        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9;
            int col = i % 9;
            boolean edge = row == 0 || row == ROWS - 1 || col == 0 || col == 8;
            inv.setItem(i, edge ? border : filler);
        }

        ItemStack yes = button(cm, Material.LIME_CONCRETE, "invitation.gui.accept.material",
                plugin.getConfig().getString("invitation.gui.accept.name", "<green><bold>YES"),
                "invitation.gui.accept.lore");
        ItemStack no = button(cm, Material.RED_CONCRETE, "invitation.gui.decline.material",
                plugin.getConfig().getString("invitation.gui.decline.name", "<red><bold>NO"),
                "invitation.gui.decline.lore");
        for (int row = 1; row <= 3; row++) {
            for (int col : YES_COLUMNS) {
                int slot = row * 9 + col;
                inv.setItem(slot, yes.clone());
                gui.yesSlots.add(slot);
            }
            for (int col : NO_COLUMNS) {
                int slot = row * 9 + col;
                inv.setItem(slot, no.clone());
                gui.noSlots.add(slot);
            }
        }

        ItemStack question = button(cm, Material.ENDER_EYE, "invitation.gui.question.material",
                plugin.getConfig().getString("invitation.gui.question.name", "<dark_purple><bold>THE NULL CALLS TO YOU"),
                "invitation.gui.question.lore");
        inv.setItem(4, question);
        return gui;
    }

    private static ItemStack button(ConfigManager cm, Material def, String materialPath, String name, String lorePath) {
        ItemStack item = new ItemStack(cm.material(materialPath, def));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(cm.parseItem(name));
        if (lorePath != null) {
            meta.lore(cm.parseItemList(lorePath));
        }
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public UUID getInviter() {
        return inviter;
    }

    public UUID getTarget() {
        return target;
    }

    public boolean isYes(int slot) {
        return yesSlots.contains(slot);
    }

    public boolean isNo(int slot) {
        return noSlots.contains(slot);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
