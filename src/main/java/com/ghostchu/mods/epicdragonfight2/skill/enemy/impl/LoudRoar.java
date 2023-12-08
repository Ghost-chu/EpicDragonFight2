package com.ghostchu.mods.epicdragonfight2.skill.enemy.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.EpicSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

@EpicSkill
public class LoudRoar extends AbstractEpicDragonSkill {

    private final int potionDuration;
    private final int potionLevel;

    public LoudRoar(@NotNull DragonFight fight) {
        super(fight, "loud-roar");
        this.potionDuration = getSkillConfig().getInt("potion-duration");
        this.potionLevel = getSkillConfig().getInt("potion-level");
    }

    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_1};
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
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, potionDuration, potionLevel));
            }
        }
        return false;
    }

    @Override
    public int skillStartWaitingTicks() {
        return 1;
    }
}
