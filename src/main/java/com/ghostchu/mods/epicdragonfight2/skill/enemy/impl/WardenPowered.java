package com.ghostchu.mods.epicdragonfight2.skill.enemy.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.EpicDragonFight2;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.EpicSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

@EpicSkill
public class WardenPowered extends AbstractEpicDragonSkill {
    private final double wardenHealth;
    private boolean summoned;

    public WardenPowered(@NotNull DragonFight fight) {
        super(fight, "warden");
        this.wardenHealth = getSkillConfig().getDouble("warden-health");
    }

    @Override
    public int start() {
        this.getPlayerInWorld().forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, this.getRandom().nextFloat()));
        return 20 + skillStartWaitingTicks();
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
    }

    @Override
    public boolean tick() {
        if (isWaitingStart()) {
            return false;
        }
        if (!summoned) {
            getPlayerInWorld().forEach(this::spawnWarden);
            summoned = true;
        }
        return false;
    }

    private void spawnWarden(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 5, 1, false, false, true));
        Location spawnAt = p.getLocation();
        getWorld().spawn(spawnAt, Warden.class, (entity) -> {
            entity.setMaxHealth(wardenHealth);
            entity.setHealth(entity.getMaxHealth());
            NBTEntity nbtEntity = new NBTEntity(entity);
            NBTContainer nbtContainer = new NBTContainer("{Brain:{memories:{\"minecraft:dig_cooldown\":{ttl:1200L,value:{}}, \"minecraft:is_emerging\":{ttl:134L,value:{}}}}}");
            nbtEntity.mergeCompound(nbtContainer);
            entity.setInvisible(true);
            markEntitySummonedByPlugin(entity);
            Bukkit.getScheduler().runTaskLater(EpicDragonFight2.getInstance(), () -> entity.setInvisible(false), 4);
        });
    }

    @Override
    public int skillStartWaitingTicks() {
        return 35;
    }

    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_3, Stage.STAGE_4};
    }
}
