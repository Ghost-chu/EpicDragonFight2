package com.ghostchu.mods.epicdragonfight2.skill.enemy.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.EpicSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EpicSkill
public class FlameBoom extends AbstractEpicDragonSkill {
    private final int duration;
    private final int maxAmount;
    private final int fireTicks;
    private final int darknessTicks;
    private final double damage;

    public FlameBoom(@NotNull DragonFight fight) {
        super(fight, "flame-boom");
        duration = getSkillConfig().getInt("duration");
        maxAmount = getSkillConfig().getInt("max-amount");
        fireTicks = getSkillConfig().getInt("fire-ticks");
        darknessTicks = getSkillConfig().getInt("darkness-ticks");
        damage = getSkillConfig().getDouble("damage");
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
        //noinspection DuplicatedCode
        if (isWaitingStart()) {
            return false;
        }
        if (this.getCleanTick() == 0) {
            for (Player player : randomPlayers()) {
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

        Location loc = event.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, getRandom().nextFloat());
        loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1);
        loc.getWorld().getNearbyEntities(loc, 5, 5, 5).forEach(e -> {
            if (e instanceof Player player) {
                player.damage(damage);
                player.setFireTicks(fireTicks);
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, darknessTicks, 1));
            }
        });
        event.setCancelled(true);
        event.getEntity().remove();
    }

    @Override
    public int skillStartWaitingTicks() {
        return 20 * 3;
    }

    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_2, Stage.STAGE_3};
    }
}
