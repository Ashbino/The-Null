package com.thegildedking.thenull.commands;

import com.thegildedking.thenull.Commander;
import com.thegildedking.thenull.ConfigManager;
import com.thegildedking.thenull.NullRecord;
import com.thegildedking.thenull.TheNullPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/** /null ... with permission checks and tab completion. */
public final class NullCommand implements CommandExecutor, TabCompleter {
    private static final List<String> POSITIONS = List.of("sovereign", "executioner", "harbinger", "watcher");

    private record Target(UUID uuid, String name, Player online) {
    }

    private final TheNullPlugin plugin;

    public NullCommand(TheNullPlugin plugin) {
        this.plugin = plugin;
    }

    private ConfigManager cm() {
        return plugin.getConfigManager();
    }

    private boolean can(CommandSender s, String node) {
        return s.hasPermission("thenull.admin") || s.hasPermission(node);
    }

    private void send(CommandSender s, String key, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... r) {
        s.sendMessage(cm().msg(key, r));
    }

    private Target resolve(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return new Target(online.getUniqueId(), online.getName(), online);
        }
        NullRecord byId = plugin.getNullManager().getByIdentity(input);
        if (byId != null) {
            return new Target(byId.getUuid(), byId.getOriginalName(), Bukkit.getPlayer(byId.getUuid()));
        }
        for (NullRecord r : plugin.getNullManager().all()) {
            if (r.getOriginalName().equalsIgnoreCase(input)) {
                return new Target(r.getUuid(), r.getOriginalName(), Bukkit.getPlayer(r.getUuid()));
            }
        }
        OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(input);
        if (off != null) {
            return new Target(off.getUniqueId(), off.getName() != null ? off.getName() : input, off.getPlayer());
        }
        return null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            send(sender, "usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "transform" -> transform(sender, args);
            case "remove" -> remove(sender, args);
            case "commander" -> commander(sender, args);
            case "give" -> give(sender, args);
            case "info" -> info(sender, args);
            case "list" -> list(sender);
            case "setskin" -> setSkin(sender);
            case "reload" -> reload(sender);
            default -> send(sender, "usage");
        }
        return true;
    }

    private void transform(CommandSender sender, String[] args) {
        if (!can(sender, "thenull.transform")) {
            send(sender, "no-permission");
            return;
        }
        if (args.length < 2 || args.length > 3) {
            send(sender, "usage-transform");
            return;
        }
        Commander position = null;
        if (args.length == 3) {
            position = Commander.parse(args[2]);
            if (position == null) {
                send(sender, "invalid-position");
                return;
            }
            if (!can(sender, "thenull.commander")) {
                send(sender, "no-permission");
                return;
            }
        }
        Target t = resolve(args[1]);
        if (t == null) {
            send(sender, "player-not-found", Placeholder.unparsed("name", args[1]));
            return;
        }
        if (plugin.getNullManager().isNull(t.uuid())) {
            send(sender, "already-null", Placeholder.unparsed("player", t.name()));
            return;
        }
        NullRecord r;
        try {
            r = plugin.getNullManager().transform(t.uuid(), t.name(), position);
        } catch (IllegalStateException ex) {
            sender.sendMessage(Component.text(ex.getMessage()));
            return;
        }
        if (r == null) {
            send(sender, "already-null", Placeholder.unparsed("player", t.name()));
            return;
        }
        send(sender, "transform-admin", Placeholder.unparsed("player", t.name()),
                Placeholder.unparsed("identity", r.getIdentity()));
        if (position != null) {
            send(sender, "commander-admin-assigned", Placeholder.unparsed("player", t.name()),
                    Placeholder.unparsed("position", plugin.getCommanderManager().title(position)));
        }
        if (t.online() == null) {
            send(sender, "transform-offline-note", Placeholder.unparsed("player", t.name()));
        }
        if (!plugin.getSkinManager().isConfigured()) {
            send(sender, "skin-not-configured");
        }
    }

    private void remove(CommandSender sender, String[] args) {
        if (!can(sender, "thenull.transform")) {
            send(sender, "no-permission");
            return;
        }
        if (args.length != 2) {
            send(sender, "usage-remove");
            return;
        }
        Target t = resolve(args[1]);
        if (t == null) {
            send(sender, "player-not-found", Placeholder.unparsed("name", args[1]));
            return;
        }
        if (!plugin.getNullManager().isNull(t.uuid())) {
            send(sender, "not-null", Placeholder.unparsed("player", t.name()));
            return;
        }
        NullRecord removed = plugin.getNullManager().remove(t.uuid());
        if (removed == null) {
            send(sender, "not-null", Placeholder.unparsed("player", t.name()));
            return;
        }
        send(sender, "remove-admin", Placeholder.unparsed("player", t.name()),
                Placeholder.unparsed("identity", removed.getIdentity()));
        if (removed.getCommander() != null) {
            send(sender, "commander-freed",
                    Placeholder.unparsed("position", plugin.getCommanderManager().title(removed.getCommander())));
        }
        if (t.online() == null) {
            send(sender, "remove-offline-note", Placeholder.unparsed("player", t.name()));
        }
    }

    private void commander(CommandSender sender, String[] args) {
        if (!can(sender, "thenull.commander")) {
            send(sender, "no-permission");
            return;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
            Commander position = Commander.parse(args[2]);
            if (position == null) {
                send(sender, "invalid-position");
                return;
            }
            NullRecord former = plugin.getCommanderManager().clear(position);
            if (former == null) {
                send(sender, "commander-position-empty",
                        Placeholder.unparsed("position", plugin.getCommanderManager().title(position)));
                return;
            }
            send(sender, "commander-removed", Placeholder.unparsed("position", plugin.getCommanderManager().title(position)),
                    Placeholder.unparsed("player", former.getOriginalName()));
            Player online = Bukkit.getPlayer(former.getUuid());
            if (online != null) {
                online.sendMessage(cm().msg("commander-target-removed",
                        Placeholder.unparsed("position", plugin.getCommanderManager().title(position))));
            }
            return;
        }
        if (args.length != 3) {
            send(sender, "usage-commander");
            return;
        }
        Commander position = Commander.parse(args[2]);
        if (position == null) {
            send(sender, "invalid-position");
            return;
        }
        Target t = resolve(args[1]);
        if (t == null) {
            send(sender, "player-not-found", Placeholder.unparsed("name", args[1]));
            return;
        }
        NullRecord record = plugin.getNullManager().get(t.uuid());
        if (record == null) {
            send(sender, "commander-not-null", Placeholder.unparsed("player", t.name()));
            return;
        }
        NullRecord replaced = plugin.getCommanderManager().assign(record, position);
        String title = plugin.getCommanderManager().title(position);
        send(sender, "commander-admin-assigned", Placeholder.unparsed("player", t.name()),
                Placeholder.unparsed("position", title));
        if (replaced != null) {
            send(sender, "commander-replaced", Placeholder.unparsed("player", replaced.getOriginalName()),
                    Placeholder.unparsed("position", title));
            Player old = Bukkit.getPlayer(replaced.getUuid());
            if (old != null) {
                old.sendMessage(cm().msg("commander-target-removed", Placeholder.unparsed("position", title)));
            }
        }
        if (t.online() != null) {
            t.online().sendMessage(cm().msg("commander-target-assigned", Placeholder.unparsed("position", title)));
        }
    }

    private void give(CommandSender sender, String[] args) {
        if (!can(sender, "thenull.totem")) {
            send(sender, "no-permission");
            return;
        }
        if (args.length != 2) {
            send(sender, "usage-give");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            send(sender, "player-not-found", Placeholder.unparsed("name", args[1]));
            return;
        }
        ItemStack totem = plugin.getTotemManager().createTotem();
        for (ItemStack left : target.getInventory().addItem(totem).values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), left);
        }
        send(sender, "give-admin", Placeholder.unparsed("player", target.getName()));
        target.sendMessage(cm().msg("give-target"));
    }

    private void info(CommandSender sender, String[] args) {
        if (!can(sender, "thenull.transform")) {
            send(sender, "no-permission");
            return;
        }
        if (args.length != 2) {
            send(sender, "usage-info");
            return;
        }
        Target t = resolve(args[1]);
        if (t == null) {
            send(sender, "player-not-found", Placeholder.unparsed("name", args[1]));
            return;
        }
        NullRecord r = plugin.getNullManager().get(t.uuid());
        if (r == null) {
            send(sender, "info-not-null", Placeholder.unparsed("player", t.name()));
            return;
        }
        String position = r.getCommander() == null ? "-" : plugin.getCommanderManager().title(r.getCommander());
        send(sender, "info-null", Placeholder.unparsed("player", r.getOriginalName()),
                Placeholder.unparsed("identity", r.getIdentity()),
                Placeholder.unparsed("position", position),
                Placeholder.unparsed("status", t.online() != null ? "online" : "offline"));
    }

    private void list(CommandSender sender) {
        if (!can(sender, "thenull.transform")) {
            send(sender, "no-permission");
            return;
        }
        send(sender, "list-header", Placeholder.unparsed("count", String.valueOf(plugin.getNullManager().all().size())));
        for (Map.Entry<Commander, NullRecord> e : plugin.getCommanderManager().holders().entrySet()) {
            String title = plugin.getCommanderManager().title(e.getKey());
            NullRecord r = e.getValue();
            if (r == null) {
                send(sender, "list-commander-empty", Placeholder.unparsed("position", title));
            } else {
                send(sender, "list-commander", Placeholder.unparsed("position", title),
                        Placeholder.unparsed("player", r.getOriginalName()),
                        Placeholder.unparsed("identity", r.getIdentity()));
            }
        }
        for (NullRecord r : plugin.getNullManager().all()) {
            if (r.getCommander() == null) {
                send(sender, "list-entry", Placeholder.unparsed("player", r.getOriginalName()),
                        Placeholder.unparsed("identity", r.getIdentity()));
            }
        }
    }

    private void setSkin(CommandSender sender) {
        if (!sender.hasPermission("thenull.admin")) {
            send(sender, "no-permission");
            return;
        }
        if (!(sender instanceof Player p)) {
            send(sender, "players-only");
            return;
        }
        if (plugin.getSkinManager().saveFromPlayer(p)) {
            send(sender, "setskin-done");
        } else {
            send(sender, "setskin-failed");
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("thenull.admin")) {
            send(sender, "no-permission");
            return;
        }
        plugin.reloadAll();
        send(sender, "reload-done");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
                                      @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if (can(sender, "thenull.transform")) {
                out.addAll(List.of("transform", "remove", "info", "list"));
            }
            if (can(sender, "thenull.commander")) {
                out.add("commander");
            }
            if (can(sender, "thenull.totem")) {
                out.add("give");
            }
            if (sender.hasPermission("thenull.admin")) {
                out.addAll(List.of("setskin", "reload"));
            }
            return filter(out, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "transform", "give" -> {
                    if (can(sender, sub.equals("give") ? "thenull.totem" : "thenull.transform")) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            out.add(p.getName());
                        }
                    }
                }
                case "remove", "info" -> {
                    if (can(sender, "thenull.transform")) {
                        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                        for (NullRecord r : plugin.getNullManager().all()) {
                            names.add(r.getOriginalName());
                            names.add(r.getIdentity());
                        }
                        if (sub.equals("info")) {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                names.add(p.getName());
                            }
                        }
                        out.addAll(names);
                    }
                }
                case "commander" -> {
                    if (can(sender, "thenull.commander")) {
                        out.add("remove");
                        for (NullRecord r : plugin.getNullManager().all()) {
                            out.add(r.getOriginalName());
                        }
                    }
                }
                default -> {
                }
            }
            return filter(out, args[1]);
        }
        if (args.length == 3) {
            boolean positions = (sub.equals("transform") && can(sender, "thenull.commander"))
                    || (sub.equals("commander") && can(sender, "thenull.commander"));
            if (positions) {
                out.addAll(POSITIONS);
            }
            return filter(out, args[2]);
        }
        return out;
    }

    private List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> res = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                res.add(o);
            }
        }
        return res;
    }
}
