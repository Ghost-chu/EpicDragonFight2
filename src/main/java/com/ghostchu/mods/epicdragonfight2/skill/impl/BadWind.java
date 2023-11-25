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

public class BadWind extends AbstractEpicDragonSkill {
    private final int duration;

    public BadWind(@NotNull DragonFight fight) {
        super(fight, "wind");
        this.duration = getSkillConfig().getInt("duration");
    }

    @Override
    public int start() {
        this.getPlayerInWorld().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, this.getRandom().nextFloat()));
        return this.duration;
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
    }

    @Override
    public boolean tick() {
        for (Player player : this.getPlayerInWorld()) {
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
    public long skillStartWaitingTicks() {
        return 35;
    }

    @Override
    @NotNull
    public Stage[] getAdaptStages() {
        return Stage.values();
    }
}
