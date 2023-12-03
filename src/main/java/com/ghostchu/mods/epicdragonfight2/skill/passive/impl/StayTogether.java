package com.ghostchu.mods.epicdragonfight2.skill.passive.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.skill.passive.AbstactEpicPassiveSkill;
import com.ghostchu.mods.epicdragonfight2.util.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StayTogether extends AbstactEpicPassiveSkill {
    public StayTogether(@NotNull DragonFight fight) {
        super(fight, "stay-together");
    }

    @Override
    public boolean tick() {
        this.getPlayerInWorld().forEach(player -> {
            List<Player> aroundPlayers = player.getNearbyEntities(5.0, 5.0, 5.0).stream()
                    .filter(entity -> entity instanceof Player)
                    .map(entity -> (Player) entity).toList();
            if (aroundPlayers.size() > 3) {
                player.setCooldown(Material.SHIELD, 0);
                player.setCooldown(Material.ENDER_PEARL, 0);
                player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 30, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 30, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 0));
                if (!player.hasMetadata("stay-together")) {
                    player.setMetadata("stay-together", new FixedMetadataValue(getPlugin(), true));
                    // player enter the stay together
                    if (getRandom().nextBoolean()) {
                        // group says
                        Player groupSayer = RandomUtil.randomPick(aroundPlayers);
                        Bukkit.dispatchCommand(groupSayer, "global " +
                                RandomUtil.randomPick(getPlugin().getConfig().getStringList("stay-together.group-says")));
                    } else {
                        // alone says
                        Bukkit.dispatchCommand(player, "global " +
                                RandomUtil.randomPick(getPlugin().getConfig().getStringList("stay-together.alone-says")));
                    }
                }
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 30, 0));
                if (player.hasMetadata("stay-together")) {
                    player.removeMetadata("stay-together", getPlugin());
                    // player exit stay together
                }
            }
        });
        return false;
    }
}
