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

public class Throw extends AbstractEpicDragonSkill {
    private final int throwTime;
    private final int velocityX;
    private final int velocityY;
    private final int velocityZ;

    public Throw(@NotNull DragonFight fight) {
        super(fight, "throws");
        this.throwTime = getSkillConfig().getInt("time");
        this.velocityX = getSkillConfig().getInt("velocity-x");
        this.velocityY = getSkillConfig().getInt("velocity-y");
        this.velocityZ = getSkillConfig().getInt("velocity-z");
    }

    @Override
    public int start() {
        this.getPlayerInWorld().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0f, this.getRandom().nextFloat()));
        return  this.skillStartWaitingTicks();
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
    }

    @Override
    public boolean tick() {
        this.playParticle();
        if (isWaitingStart()) {
            return false;
        }
        if (getCleanTick() == 0) {
            for (Player player : this.getPlayerInWorld()) {
                Vector baseVelocity = player.getVelocity();
                baseVelocity.setX(baseVelocity.getX() + velocityX);
                baseVelocity.setY(baseVelocity.getY() + velocityY);
                baseVelocity.setZ(baseVelocity.getZ() + velocityZ);
                player.setVelocity(baseVelocity);
                this.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 1);
                this.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, this.getRandom().nextFloat());
            }
        }
        return false;
    }

    private void playParticle() {
        for (Player player : this.getPlayerInWorld()) {
            this.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 4);
        }
    }

    @Override
    public int skillStartWaitingTicks() {
        return throwTime;
    }

    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_1};
    }
}
