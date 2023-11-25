package com.ghostchu.mods.epicdragonfight2.skill.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.SkillEndReason;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class RocketRain extends AbstractEpicDragonSkill {
    private final int duration;
    private final int checkInterval;
    private final int creeperExplosionRadius;
    private final boolean creeperPowered;
    private final int creeperFuseTicks;
    private final double creeperHealth;
    private final boolean creeperAutoIgnite;
    private final int creeperSpeedDuration;
    private final int creeperSpeedLevel;

    public RocketRain(@NotNull DragonFight fight) {
        super(fight, "rocket-rain");
        duration = getSkillConfig().getInt("duration");
        checkInterval = getSkillConfig().getInt("check-interval");
        creeperExplosionRadius = getSkillConfig().getInt("creeper-explode-radius");
        creeperPowered = getSkillConfig().getBoolean("creeper-powered");
        creeperFuseTicks = getSkillConfig().getInt("creeper-fuse-ticks");
        creeperHealth = getSkillConfig().getDouble("creeper-health");
        creeperAutoIgnite = getSkillConfig().getBoolean("creeper-auto-ignite");
        creeperSpeedDuration = getSkillConfig().getInt("creeper-speed-duration");
        creeperSpeedLevel = getSkillConfig().getInt("creeper-speed-level");
    }


    @Override
    public int start() {
        return this.duration + this.skillStartWaitingTicks();
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
        getFight().getWorld().getEntitiesByClass(TNTPrimed.class)
                .stream()
                .filter(this::isMarkedSummonedByPlugin)
                .forEach(Entity::remove);
    }

    @Override
    public boolean tick() {
        if (getCleanTick() % this.checkInterval == 0) {
            ArrayList<Player> playersRecent = new ArrayList<Player>();
            for (Player player : this.getPlayerInWorld()) {
                if (!(player.getLocation().distance(this.getDragon().getLocation()) < 300.0)) continue;
                playersRecent.add(player);
            }
            for (Player player : playersRecent) {
                Vector dir = player.getLocation().add(this.getRandom().nextInt(20) - 10, this.getRandom().nextInt(6) - 1, this.getRandom().nextInt(20) - 10).subtract(this.getDragon().getLocation().add(0.0, 0.0, 0.0)).toVector().normalize();
                dir.multiply(2);
                Creeper creeper = (Creeper) this.getWorld().spawnEntity(this.getDragon().getLocation(), EntityType.CREEPER);
                creeper.setExplosionRadius(this.creeperExplosionRadius);
                creeper.setPowered(this.creeperPowered);
                creeper.setMaxFuseTicks(this.creeperFuseTicks);
                if (this.creeperAutoIgnite) {
                    creeper.ignite();
                }
                creeper.setMaxHealth(this.creeperHealth);
                creeper.setHealth(creeper.getMaxHealth());
                applyDifficultRate(creeper);
                TNTPrimed tntEntity = (TNTPrimed) this.getWorld().spawnEntity(this.getDragon().getLocation(), EntityType.PRIMED_TNT);
                tntEntity.setSource(this.getDragon());
                tntEntity.addPassenger(creeper);
                tntEntity.setVelocity(dir);
                tntEntity.setGlowing(true);
                markEntitySummonedByPlugin(tntEntity);
                creeper.setTarget(player);
                creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, this.creeperSpeedDuration, this.creeperSpeedLevel));
                markEntitySummonedByPlugin(creeper);
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, this.getRandom().nextFloat());
            }
        }
        this.playParticle();
        return false;
    }

    private void playParticle() {
        for (TNTPrimed tnt : this.getWorld().getEntitiesByClass(TNTPrimed.class)) {
            this.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, tnt.getLocation(), 2);
        }
    }

    @Override
    public int skillStartWaitingTicks() {
        return 10;
    }

    @Override
    @NotNull
    public Stage[] getAdaptStages() {
        return Stage.values();
    }
}
