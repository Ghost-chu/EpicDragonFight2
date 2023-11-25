package com.ghostchu.mods.epicdragonfight2.skill.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.SkillEndReason;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class Throw extends AbstractEpicDragonSkill {
    private final int duration;
    private final int throwTime;
    private final int velocityX;
    private final int velocityY;
    private final int velocityZ;

    public Throw(@NotNull DragonFight fight) {
        super(fight, "throws");
        this.duration = getSkillConfig().getInt("duration");
        this.throwTime = getSkillConfig().getInt("time");
        this.velocityX = getSkillConfig().getInt("velocity-x");
        this.velocityY = getSkillConfig().getInt("velocity-y");
        this.velocityZ = getSkillConfig().getInt("velocity-z");
    }

    @Override
    public int start() {
        this.getPlayerInWorld().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0f, this.getRandom().nextFloat()));
        return this.duration;
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
    }

    @Override
    public boolean tick() {
        if (this.getTick() == this.throwTime) {
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
        this.playParticle();
        return false;
    }

    private void playParticle() {
        for (Player player : this.getPlayerInWorld()) {
            this.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 4);
        }
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
