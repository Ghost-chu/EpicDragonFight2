package com.ghostchu.mods.epicdragonfight2.skill.enemy.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.EpicSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import org.bukkit.Location;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.NotNull;

@EpicSkill
public class GroupedWolfs extends AbstractEpicDragonSkill {
    private final int duration;
    private final int wolfAmount;

    public GroupedWolfs(@NotNull DragonFight fight) {
        super(fight, "grouped-wolfs");
        duration = getSkillConfig().getInt("duration");
        wolfAmount = getSkillConfig().getInt("amount");
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
            for (Player player : this.getPlayerInWorld()) {
                Location wolfGenerated = getDragon().getLocation();
                if (player.getLocation().getBlockY() > getDragon().getLocation().getBlockY()) {
                    wolfGenerated.add(0, 3, 0);
                } else {
                    wolfGenerated.add(0, -3, 0);
                }
                for (int i = 0; i < wolfAmount; i++) {
                    Wolf wolf = (Wolf) this.getWorld().spawnEntity(wolfGenerated, EntityType.WOLF);
                    wolf.setVelocity(fromToVector(player.getLocation(), getDragon().getLocation()));
                    wolf.setAngry(true);
                    wolf.setTarget(randomPlayer());
                    wolf.setCustomNameVisible(true);
                    wolf.setCustomName("狼群弹药");
                    markEntitySummonedByPlugin(wolf);
                }
            }
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDragonBallHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof DragonFireball)) {
            return;
        }
        if (!isMarkedSummonedByPlugin(event.getEntity())) {
            return;
        }

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWolfDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Wolf)) {
            return;
        }
        if (!isMarkedSummonedByPlugin(event.getEntity())) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWolfDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Wolf)) {
            return;
        }
        if (!isMarkedSummonedByPlugin(event.getEntity())) {
            return;
        }
        if (!isMarkedSummonedByPlugin(event.getDamager())) {
            return;
        }
        event.setCancelled(true);
    }

    @Override
    public int skillStartWaitingTicks() {
        return 20 * 3;
    }

    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_1, Stage.STAGE_2};
    }
}
