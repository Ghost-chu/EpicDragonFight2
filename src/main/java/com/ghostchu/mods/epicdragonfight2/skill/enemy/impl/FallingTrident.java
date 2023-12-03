package com.ghostchu.mods.epicdragonfight2.skill.enemy.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.EpicSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@EpicSkill
public class FallingTrident extends AbstractEpicDragonSkill {
    private final int duration;
    private final int checkInterval;
    private final int height;

    public FallingTrident(@NotNull DragonFight fight) {
        super(fight, "trident");
        this.duration = getSkillConfig().getInt("duration");
        this.checkInterval = getSkillConfig().getInt("check-interval");
        this.height = getSkillConfig().getInt("height");
    }

    @Override
    public int skillStartWaitingTicks() {
        return 20 * 2;
    }


    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_2};
    }

    @Override
    public boolean tick() {
        if (isWaitingStart()) {
            return false;
        }
        if (getCleanTick() % this.checkInterval == 0) {
            this.summonTrident();
        }
        if (getTick() % 2 == 0) {
            this.playParticle();
        }
        return false;
    }

    private void playParticle() {
        for (Trident trident : this.getWorld().getEntitiesByClass(Trident.class)) {
            if (!isMarkedSummonedByPlugin(trident)) continue;
            this.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, trident.getLocation(), 2);
            Location location = getFalloutPosition(trident);
            summonCircle(location, 2);
        }
    }

    @Override
    public int start() {
        this.getPlayerInWorld().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, this.getRandom().nextFloat()));
        return this.duration;
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
        this.getWorld().getEntitiesByClass(Trident.class)
                .stream()
                .filter(this::isMarkedSummonedByPlugin)
                .forEach(Entity::remove);
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!Objects.equals(event.getEntity().getLocation().getWorld(), this.getWorld())) {
            return;
        }
        if (!(event.getEntity() instanceof Trident)) {
            return;
        }
        if (!isMarkedSummonedByPlugin(event.getEntity()))
            return;
        this.getWorld().strikeLightning(event.getEntity().getLocation());
        this.getWorld().createExplosion(event.getEntity().getLocation(), 2.5f, false);
        event.getEntity().remove();
    }

    private void summonTrident() {
        for (Player player : this.getPlayerInWorld()) {
            Trident trident = (Trident) this.getWorld().spawnEntity(player.getLocation().add(getRandom().nextInt(-5, 5), this.height, getRandom().nextInt(-5, 5)), EntityType.TRIDENT);
            trident.setCritical(true);
            trident.setGlowing(true);
            trident.setShooter(this.getDragon());
            trident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            trident.setPersistent(false);
            markEntitySummonedByPlugin(trident);
            Vector dir = player.getLocation().subtract(trident.getLocation()).toVector().normalize();
            dir.multiply(2);
            trident.setVelocity(dir);
        }
    }
}
