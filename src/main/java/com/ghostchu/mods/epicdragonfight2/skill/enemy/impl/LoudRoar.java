package com.ghostchu.mods.epicdragonfight2.skill.enemy.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class LoudRoar extends AbstractEpicDragonSkill {

    private final int potionDuration;

    public LoudRoar(@NotNull DragonFight fight) {
        super(fight, "loud-road");
        this.potionDuration = getSkillConfig().getInt("potion-duration");
    }

    @Override
    public int start() {

        return this.skillStartWaitingTicks();
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
    }

    @Override
    public boolean tick() {
        if (isWaitingStart()) {
            return false;
        }
        if (getCleanTick() == 0) {
            for (Player player : this.getPlayerInWorld()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, this.getRandom().nextFloat());
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 1, potionDuration));
            }
        }
        return false;
    }

    @Override
    public int skillStartWaitingTicks() {
        return 1;
    }

    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_1};
    }
}
