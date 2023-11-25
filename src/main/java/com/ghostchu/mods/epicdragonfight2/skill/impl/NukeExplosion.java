package com.ghostchu.mods.epicdragonfight2.skill.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.SkillEndReason;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class NukeExplosion extends AbstractEpicDragonSkill {
    private final Location nukeLocation = new Location(this.getWorld(), 0.0, 70.0, 0.0);
    private final int duration;
    private final int explodeTime;
    private final float power;

    public NukeExplosion(@NotNull DragonFight fight) {
        super(fight, "nuke");
        this.duration = getSkillConfig().getInt("duration");
        this.explodeTime = getSkillConfig().getInt("explode-time");
        this.power = getSkillConfig().getInt("power");

    }

    @Override
    public int start() {
        this.getPlayerInWorld().forEach(player -> {
            Vector dir = this.nukeLocation.subtract(player.getLocation()).toVector().normalize();
            dir.multiply(2);
            player.setVelocity(dir);
        });
        this.getPlayerInWorld().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0f, this.getRandom().nextFloat()));
        return this.duration;
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
    }

    @Override
    public boolean tick() {
        if (this.getTick() < this.explodeTime) {
            this.getWorld().spawnParticle(Particle.SMOKE_LARGE, this.nukeLocation, 4);
            this.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, this.nukeLocation, 4);
            if (this.getTick() % 5 == 0) {
                this.getPlayerInWorld().forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, this.getRandom().nextFloat()));
            }
        }
        if (this.getTick() == this.explodeTime) {
            this.getWorld().createExplosion(new Location(this.getWorld(), 0.0, 70.0, 0.0), this.power, true, false, this.getDragon());
        }
        return false;
    }

    @Override
    public long skillStartWaitingTicks() {
        return 0;
    }

    @Override
    @NotNull
    public Stage[] getAdaptStages() {
        return Stage.values();
    }
}
