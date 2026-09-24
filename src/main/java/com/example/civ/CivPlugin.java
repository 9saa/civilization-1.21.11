package com.example.civ;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public class CivPlugin extends JavaPlugin {

    private static CivPlugin instance;
    private CivilizationManager manager;

    @Override
    public void onEnable() {
        instance = this;
        this.manager = new CivilizationManager(this);
        this.manager.load();

        // Register the /civ command via the lifecycle manager.
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            CivCommand.register(commands, this);
        });

        // Register the invite GUI listener.
        getServer().getPluginManager().registerEvents(new InviteListener(this), this);

        // Start the crown effect task (runs every 20 ticks = 1 second).
        new CrownTask(this, manager).runTaskTimer(this, 0L, 20L);

        getLogger().info("CivMod enabled.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.save();
        getLogger().info("CivMod disabled.");
    }

    public static CivPlugin getInstance() { return instance; }

    public CivilizationManager getManager() { return manager; }
}
