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
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

@EpicSkill
public class DragonEffectCloud extends AbstractEpicDragonSkill {
    private final int duration;
    private final int cloudDuration;
    private final int checkInterval;
    private final float radius;

    public DragonEffectCloud(@NotNull DragonFight fight) {
        super(fight, "dragon-effect-cloud");
        duration = getSkillConfig().getInt("duration");
        cloudDuration = getSkillConfig().getInt("cloud-duration");
        checkInterval = getSkillConfig().getInt("check-interval");
        radius = getSkillConfig().getInt("radius");
    }

    @Override
    public int start() {
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
        if (this.getCleanTick() % this.checkInterval == 0) {
            for (Player player : this.getPlayerInWorld()) {
                Location ballGeneratePos = getDragon().getLocation();
                if (player.getLocation().getBlockY() > getDragon().getLocation().getBlockY()) {
                    ballGeneratePos.add(0, 3, 0);
                } else {
                    ballGeneratePos.add(0, -3, 0);
                }
                DragonFireball dragonFireball = (DragonFireball) getDragon().getWorld().spawnEntity(getDragon().getLocation(), EntityType.DRAGON_FIREBALL, false);
                dragonFireball.setVelocity(fromToVector(player.getLocation(), getDragon().getLocation()));
                dragonFireball.setPersistent(false);
                markEntitySummonedByPlugin(dragonFireball);
            }
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDragonBallHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof DragonFireball)) {
            return;
        }
        if (event.getEntity() instanceof DragonFireball && event.getHitEntity() instanceof EnderDragon) {
            event.setCancelled(true);
            return;
        }
        if (!isMarkedSummonedByPlugin(event.getEntity())) {
            return;
        }
        Entity entity = this.getWorld().spawnEntity(event.getEntity().getLocation(), EntityType.AREA_EFFECT_CLOUD);
        AreaEffectCloud effectCloud = (AreaEffectCloud) entity;
        effectCloud.setBasePotionType(PotionType.INSTANT_DAMAGE);
        effectCloud.setSource(this.getDragon());
        effectCloud.setDuration(this.cloudDuration);
        effectCloud.setParticle(Particle.DRAGON_BREATH);
        effectCloud.setRadius(this.radius);
        effectCloud.setReapplicationDelay(8);
        markEntitySummonedByPlugin(effectCloud);
        this.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, this.getRandom().nextFloat());
        event.setCancelled(true);
        event.getEntity().remove();
    }

    @Override
    public int skillStartWaitingTicks() {
        return 20 * 3;
    }


    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_1};
    }
}
