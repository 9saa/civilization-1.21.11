package com.example.civ;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class InviteGUI {

    public static final String TITLE = "Invite a Player";
    public static final NamespacedKey INVITE_KEY = new NamespacedKey(
            CivPlugin.getInstance(), "invite_target");

    public static void open(CivPlugin plugin, Player inviter, Civilization civ) {
        CivilizationManager mgr = plugin.getManager();

        // Collect online players not in a civ and not the inviter
        List<Player> candidates = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(inviter)) continue;
            if (mgr.getCivOf(p.getUniqueId()) != null) continue;
            candidates.add(p);
        }

        int size = 54;
        Inventory gui = Bukkit.createInventory(null, size,
                Component.text(TITLE, NamedTextColor.DARK_AQUA));

        int slot = 0;
        for (Player target : candidates) {
            if (slot >= size) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.displayName(Component.text(target.getName(), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));

            // Store target UUID in the item's PDC
            meta.getPersistentDataContainer().set(INVITE_KEY,
                    PersistentDataType.STRING, target.getUniqueId().toString());

            head.setItemMeta(meta);
            gui.setItem(slot++, head);
        }

        // Fill empty slots with a filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < size; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }

        inviter.openInventory(gui);
    }
}
