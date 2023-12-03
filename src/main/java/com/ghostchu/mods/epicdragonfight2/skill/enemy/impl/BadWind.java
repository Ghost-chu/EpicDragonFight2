package com.ghostchu.mods.epicdragonfight2.skill.enemy.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class BadWind extends AbstractEpicDragonSkill {
    private final int duration;

    public BadWind(@NotNull DragonFight fight) {
        super(fight, "wind");
        this.duration = getSkillConfig().getInt("duration");
    }

    @Override
    public int start() {
        this.getPlayerInWorld().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, this.getRandom().nextFloat()));
        return this.duration + this.skillStartWaitingTicks();
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
    }

    @Override
    public boolean tick() {
        if (isWaitingStart()) {
            return false;
        }
        for (Player player : this.getPlayerInWorld()) {
            if (Math.abs(player.getLocation().distance(getDragon().getLocation())) > 150) {
                continue;
            }
            player.setGliding(false);
            Vector baseVelocity = fromToVector(player.getLocation(), getDragon().getLocation()).add(new Vector(0.5, 0, 0.5));
            this.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation(), 1);
            player.setVelocity(baseVelocity);
        }
        this.playParticle();
        return false;
    }

    private void playParticle() {
        for (Player player : this.getPlayerInWorld()) {
            this.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 1);
        }
    }

    @Override
    public int skillStartWaitingTicks() {
        return 20 * 5;
    }

    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_1, Stage.STAGE_2};
    }
}
