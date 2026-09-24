package com.example.civ;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class CrownTask extends BukkitRunnable {

    private final CivPlugin plugin;
    private final CivilizationManager manager;

    public CrownTask(CivPlugin plugin, CivilizationManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack helmet = player.getInventory().getHelmet();
            int itemTier = CrownUtil.getCrownTier(plugin, helmet);
            if (itemTier <= 0) continue;

            Civilization civ = manager.getCivOf(player.getUniqueId());
            if (civ == null) continue;

            // Use the civ's current tier (which changes as members join/leave)
            int tier = civ.getCrownTier();
            if (tier <= 0) continue;

            // Always: Hero of the Village (amplifier = tier - 1, max 4)
            applyInfinite(player, PotionEffectType.HERO_OF_THE_VILLAGE, Math.min(tier - 1, 4));

            if (tier == 1) {
                // 10–19 members
                applyInfinite(player, PotionEffectType.REGENERATION, 0);
                applyInfinite(player, PotionEffectType.RESISTANCE, 0);
                applyInfinite(player, PotionEffectType.HEALTH_BOOST, 0); // +4 HP = 2 hearts
            } else if (tier == 2) {
                // 20–29 members
                applyInfinite(player, PotionEffectType.REGENERATION, 0);
                applyInfinite(player, PotionEffectType.RESISTANCE, 0);
                applyInfinite(player, PotionEffectType.STRENGTH, 0);
                applyInfinite(player, PotionEffectType.SPEED, 0);
                applyInfinite(player, PotionEffectType.HEALTH_BOOST, 1); // +8 HP = 4 hearts
            } else if (tier == 3) {
                // 30+ members
                applyInfinite(player, PotionEffectType.REGENERATION, 1);
                applyInfinite(player, PotionEffectType.RESISTANCE, 1);
                applyInfinite(player, PotionEffectType.STRENGTH, 1);
                applyInfinite(player, PotionEffectType.SPEED, 1);
                applyInfinite(player, PotionEffectType.HEALTH_BOOST, 2); // +12 HP = 6 hearts
            }
        }
    }

    private void applyInfinite(Player player, PotionEffectType type, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);

        // Re-apply only if missing, about to expire, or wrong amplifier.
        // INFINITE_DURATION effects have duration = -1 and should not be re-added.
        if (current != null
                && current.getDuration() > 400
                && current.getAmplifier() == amplifier) {
            return;
        }

        player.addPotionEffect(new PotionEffect(
                type,
                PotionEffect.INFINITE_DURATION,
                amplifier,
                false,  // ambient
                false,  // particles
                true    // icon
        ));
    }
}
