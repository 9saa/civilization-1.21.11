package com.example.civ;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class CrownUtil {

    /** Key used to tag an item as a civ crown and store its tier. */
    public static NamespacedKey crownKey(Plugin plugin) {
        return new NamespacedKey(plugin, "civ_crown_tier");
    }

    /** Build a crown ItemStack for the given civ tier (1–3). */
    public static ItemStack createCrown(Plugin plugin, int tier) {
        ItemStack crown = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta meta = crown.getItemMeta();

        // Display name & lore
        meta.displayName(Component.text("Crown of Civilization", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Tier " + tier, NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Grants powerful effects to the wearer.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        // Tag it with the crown key and tier
        meta.getPersistentDataContainer().set(crownKey(plugin), PersistentDataType.INTEGER, tier);

        // Unbreakable
        meta.setUnbreakable(true);

        // Enchant glint (fake enchantment)
        meta.setEnchantmentGlintOverride(true);

        // Bonus armor: +3 armor toughness so it feels special
        meta.addAttributeModifier(
                Attribute.ARMOR_TOUGHNESS,
                new AttributeModifier(
                        new NamespacedKey(plugin, "crown_toughness"),
                        3.0,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.HEAD
                )
        );

        crown.setItemMeta(meta);
        return crown;
    }

    /** Returns the crown tier stored on the item, or 0 if it isn't a crown. */
    public static int getCrownTier(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_HELMET) return 0;
        if (!item.hasItemMeta()) return 0;
        Integer tier = item.getItemMeta().getPersistentDataContainer()
                .get(crownKey(plugin), PersistentDataType.INTEGER);
        return tier == null ? 0 : tier;
    }
}
