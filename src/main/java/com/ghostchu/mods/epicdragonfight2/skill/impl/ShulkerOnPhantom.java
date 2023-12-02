package com.ghostchu.mods.epicdragonfight2.skill.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.SkillEndReason;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Shulker;
import org.jetbrains.annotations.NotNull;

public class ShulkerOnPhantom extends AbstractEpicDragonSkill {
    private final int amount;

    public ShulkerOnPhantom(@NotNull DragonFight fight) {
        super(fight, "shulker-on-phantom");
        this.amount = getSkillConfig().getInt("amount");
    }

    @Override
    public int start() {
        return skillStartWaitingTicks() + 60;
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {

    }

    @Override
    public boolean tick() {
        if(isWaitingStart())
            return false;
        for (int i = 0; i < amount; i++) {
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
                Location spawnAt = this.getDragon().getLocation().add(getRandom().nextInt(-10, 10), getRandom().nextInt(-10, 10), getRandom().nextInt(-10, 10));
                Phantom phantom = (Phantom) spawnAt.getWorld().spawnEntity(spawnAt, EntityType.PHANTOM, false);
                markEntitySummonedByPlugin(phantom);
                Shulker shulker = (Shulker) spawnAt.getWorld().spawnEntity(spawnAt, EntityType.SHULKER, false);
                markEntitySummonedByPlugin(shulker);
                phantom.addPassenger(shulker);
                spawnAt.getWorld().spawnParticle(Particle.SMOKE_NORMAL, spawnAt, 2);
                spawnAt.getWorld().playSound(phantom, Sound.ENTITY_PHANTOM_FLAP, 1.0f, getRandom().nextFloat());

            }, getRandom().nextInt(40));
        }
        return true;
    }

    @Override
    public int skillStartWaitingTicks() {
        return 20;
    }

    @Override
    @NotNull
    public Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_1};
    }
}
