package com.ghostchu.mods.epicdragonfight2.skill.enemy.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.EpicSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import org.apache.commons.lang3.Validate;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@EpicSkill
public class BreathLockLink extends AbstractEpicDragonSkill {
    private final int duration;
    private final List<Player> lockedPlayers = new ArrayList<>();
    private Location location;

    public BreathLockLink(@NotNull DragonFight fight) {
        super(fight, "breath-lock-link");
        duration = getSkillConfig().getInt("duration");
    }

    @Override
    public int start() {
        return this.duration + this.skillStartWaitingTicks();
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
        if (location != null) {
            lockedPlayers.forEach(p -> p.teleport(location));
        }
    }

    @Override
    public boolean tick() {
        //noinspection DuplicatedCode
        if (isWaitingStart()) {
            return false;
        }
        playParticles();
        if (this.getCleanTick() == 0) {
            Player player = randomPlayer();
            if (player == null) return true;
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
        return false;
    }

    private void playParticles() {
        lockedPlayers.forEach(p -> drawLine(location, p.getLocation(), 0.2));
    }

    public void drawLine(Location point1, Location point2, double space) {
        World world = point1.getWorld();
        Validate.isTrue(point2.getWorld().equals(world), "Lines cannot be in different worlds!");
        double distance = point1.distance(point2);
        Vector p1 = point1.toVector();
        Vector p2 = point2.toVector();
        Vector vector = p2.clone().subtract(p1).normalize().multiply(space);
        double length = 0;
        for (; length < distance; p1.add(vector)) {
            world.spawnParticle(Particle.PORTAL, p1.getX(), p1.getY(), p1.getZ(), 1);
            length += space;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
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
        effectCloud.setDuration(duration);
        effectCloud.setParticle(Particle.DRAGON_BREATH);
        effectCloud.setRadius(5);
        effectCloud.setReapplicationDelay(4);
        markEntitySummonedByPlugin(effectCloud);
        this.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, this.getRandom().nextFloat());
        event.setCancelled(true);
        event.getEntity().remove();
        location = effectCloud.getLocation();
        Location loc = event.getEntity().getLocation();
        loc.getWorld().getNearbyEntities(loc, 5, 5, 5).forEach(e -> {
            if (e instanceof Player player) {
                lockedPlayers.add(player);
                player.damage(8);
            }
        });
    }

    @Override
    public int skillStartWaitingTicks() {
        return 20 * 3;
    }

    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_3};
    }
}
