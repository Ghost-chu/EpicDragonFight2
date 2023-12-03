package com.ghostchu.mods.epicdragonfight2.skill.enemy.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.NotNull;

public class GroupedWolfs extends AbstractEpicDragonSkill {
    private final int duration;
    private final int wolfAmount;

    public GroupedWolfs(@NotNull DragonFight fight) {
        super(fight, "grouped-wolfs");
        duration = getSkillConfig().getInt("duration");
        wolfAmount = getSkillConfig().getInt("wolf-amount");
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDragonBallHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof DragonFireball)) {
            return;
        }
        if (event.getEntity() instanceof DragonFireball && event.getHitEntity() instanceof EnderDragon) {
            event.setCancelled(true);
            return;
        }
        if(!isMarkedSummonedByPlugin(event.getEntity())){
            return;
        }
        for (int i = 0; i < wolfAmount; i++) {
            Wolf wolf = (Wolf) this.getWorld().spawnEntity(event.getEntity().getLocation(), EntityType.WOLF);
            wolf.setAngry(true);
            wolf.setTarget(randomPlayer());
        }
    }

    @Override
    public int skillStartWaitingTicks() {
        return 20 * 3;
    }

    @NotNull
    public static Stage[] getAdaptStages() {
        return  new Stage[]{Stage.STAGE_1, Stage.STAGE_2};
    }
}
