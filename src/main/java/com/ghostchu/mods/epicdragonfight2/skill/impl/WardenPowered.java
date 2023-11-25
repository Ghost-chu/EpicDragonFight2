package com.ghostchu.mods.epicdragonfight2.skill.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.EpicDragonFight2;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.SkillEndReason;
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

public class WardenPowered extends AbstractEpicDragonSkill {
    private boolean summoned;

    public WardenPowered(@NotNull DragonFight fight) {
        super(fight, "warden");
    }

    @Override
    public int start() {
        this.getPlayerInWorld().forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, this.getRandom().nextFloat()));
        return 20;
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
    }

    @Override
    public boolean tick() {
        if (!summoned) {
            getPlayerInWorld().forEach(this::spawnWarden);
            summoned = true;
        }
        return false;
    }

    private void spawnWarden(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 5, 1, false, false, true));
        Location spawnAt = p.getLocation();
        Warden entity = getWorld().spawn(spawnAt, Warden.class, warden -> {
            NBTEntity nbtEntity = new NBTEntity(warden);
            NBTContainer nbtContainer = new NBTContainer("{Brain:{memories:{\"minecraft:dig_cooldown\":{ttl:1200L,value:{}}, \"minecraft:is_emerging\":{ttl:134L,value:{}}}}}");
            nbtEntity.mergeCompound(nbtContainer);
            warden.setInvisible(true);
        });
        applyDifficultRate(entity);
        markEntitySummonedByPlugin(entity);
        Bukkit.getScheduler().runTaskLater(EpicDragonFight2.getInstance(), () -> entity.setInvisible(false), 4);
    }

    @Override
    public long skillStartWaitingTicks() {
        return 35;
    }

    @Override
    @NotNull
    public Stage[] getAdaptStages() {
        return Stage.values();
    }
}
