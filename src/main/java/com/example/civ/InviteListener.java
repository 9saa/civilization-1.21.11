package com.example.civ;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class InviteListener implements Listener {

    private final CivPlugin plugin;

    public InviteListener(CivPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Only handle our invite GUI
        if (!event.getView().title().equals(
                Component.text(InviteGUI.TITLE, NamedTextColor.DARK_AQUA))) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String uuidStr = clicked.getItemMeta().getPersistentDataContainer()
                .get(InviteGUI.INVITE_KEY, PersistentDataType.STRING);
        if (uuidStr == null) return;

        Player inviter = (Player) event.getWhoClicked();
        Player target = Bukkit.getPlayer(UUID.fromString(uuidStr));

        if (target == null || !target.isOnline()) {
            inviter.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
            return;
        }

        CivilizationManager mgr = plugin.getManager();
        Civilization civ = mgr.getCivOf(inviter.getUniqueId());
        if (civ == null) {
            inviter.sendMessage(Component.text("You are not in a civilization.", NamedTextColor.RED));
            return;
        }
        if (mgr.getCivOf(target.getUniqueId()) != null) {
            inviter.sendMessage(Component.text(target.getName() + " is already in a civilization.", NamedTextColor.RED));
            return;
        }

        // Do the invite
        mgr.join(target.getUniqueId(), civ);
        inviter.sendMessage(Component.text("Invited " + target.getName() + " to " + civ.getName() + "!", NamedTextColor.GREEN));
        target.sendMessage(Component.text("You were invited to " + civ.getName() + "!", NamedTextColor.GOLD));

        // Refresh the GUI so the invited player disappears from the list
        inviter.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> InviteGUI.open(plugin, inviter, civ));
    }
}
