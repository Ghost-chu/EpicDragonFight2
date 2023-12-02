package com.ghostchu.mods.epicdragonfight2.skill.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.SkillEndReason;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Ravage extends AbstractEpicDragonSkill {
    private final int duration;
    private final int checkInterval;
    private final int range;

    public Ravage(@NotNull DragonFight fight) {
        super(fight, "ravage");
        duration = getSkillConfig().getInt("duration");
        checkInterval = getSkillConfig().getInt("check-interval");
        range = getSkillConfig().getInt("range");
    }

    @Override
    public int start() {
        this.getPlayerInWorld().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_GHAST_WARN, 1.0f, this.getRandom().nextFloat()));
        return this.duration + this.skillStartWaitingTicks();
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
    }

    @Override
    public boolean tick() {
        if(isWaitingStart()){
            return false;
        }
        if (getCleanTick() % this.checkInterval == 0) {
            this.findAndApplyTarget();
        }
        this.playParticle();
        return false;
    }

    private void findAndApplyTarget() {
        for (Player player : this.getPlayerInWorld()) {
            if (player.getInventory().getHelmet() != null && player.getInventory().getHelmet().getType() == Material.CARVED_PUMPKIN)
                continue;
            this.getWorld().getNearbyEntities(player.getLocation(), this.range, 20.0, this.range).stream().filter(entity -> entity instanceof Enderman).map(entity -> (Enderman) entity).forEach(enderman -> {
                enderman.setTarget(player);
                markEntitySummonedByPlugin(enderman);
                this.getWorld().playSound(enderman.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, this.getRandom().nextFloat());
            });
        }
    }

    private void playParticle() {
        for (Enderman enderman : this.getWorld().getEntitiesByClass(Enderman.class)) {
            Location particlePos = enderman.getLocation().add(0.0, 3.0, 0.0);
            this.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, particlePos, 1);
        }
    }

    @Override
    public int skillStartWaitingTicks() {
        return 15;
    }

    @Override
    @NotNull
    public Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_1,
                Stage.STAGE_2};
    }
}
