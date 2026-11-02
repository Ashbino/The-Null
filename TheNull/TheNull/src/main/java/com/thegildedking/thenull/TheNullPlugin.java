package com.thegildedking.thenull;

import com.thegildedking.thenull.commands.NullCommand;
import com.thegildedking.thenull.listeners.InvitationListener;
import com.thegildedking.thenull.listeners.NullListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TheNullPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private DataStorage dataStorage;
    private IdentityGenerator identityGenerator;
    private NullManager nullManager;
    private CommanderManager commanderManager;
    private TotemManager totemManager;
    private InvitationManager invitationManager;
    private SkinManager skinManager;
    private NametagManager nametagManager;
    private RestorationManager restorationManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.reload();

        dataStorage = new DataStorage(this);
        if (!dataStorage.load()) {
            getLogger().severe("Could not load Null data. Disabling to avoid overwriting it.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        identityGenerator = new IdentityGenerator(this);
        skinManager = new SkinManager(this);
        nametagManager = new NametagManager(this);
        restorationManager = new RestorationManager(this);
        nullManager = new NullManager(this);
        commanderManager = new CommanderManager(this);
        totemManager = new TotemManager(this);
        invitationManager = new InvitationManager(this);

        nametagManager.start();

        Bukkit.getPluginManager().registerEvents(new NullListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InvitationListener(this), this);

        PluginCommand cmd = getCommand("null");
        if (cmd != null) {
            NullCommand handler = new NullCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        // Plugin loaded while players are online (e.g. /reload): apply appearances right away.
        for (Player p : Bukkit.getOnlinePlayers()) {
            restorationManager.handleJoin(p);
        }

        if (!skinManager.isConfigured()) {
            getLogger().warning("No black skin configured yet: Nulls get an identity nametag but KEEP their "
                    + "normal skin. See README 'Black skin setup' (or run /null setskin).");
        }
        getLogger().info("TheNull enabled. " + nullManager.all().size() + " registered Null player(s).");
    }

    @Override
    public void onDisable() {
        if (invitationManager != null) {
            invitationManager.cancelAll();
        }
        if (nametagManager != null) {
            nametagManager.shutdown();
        }
        if (dataStorage != null) {
            dataStorage.save();
        }
    }

    /** /null reload */
    public void reloadAll() {
        configManager.reload();
        skinManager.reload();
        nametagManager.refreshAll();
        for (Player p : Bukkit.getOnlinePlayers()) {
            NullRecord r = nullManager.get(p.getUniqueId());
            if (r != null) {
                skinManager.applyBlack(p);
            }
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    public IdentityGenerator getIdentityGenerator() {
        return identityGenerator;
    }

    public NullManager getNullManager() {
        return nullManager;
    }

    public CommanderManager getCommanderManager() {
        return commanderManager;
    }

    public TotemManager getTotemManager() {
        return totemManager;
    }

    public InvitationManager getInvitationManager() {
        return invitationManager;
    }

    public SkinManager getSkinManager() {
        return skinManager;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public RestorationManager getRestorationManager() {
        return restorationManager;
    }
}
