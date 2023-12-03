package com.ghostchu.mods.epicdragonfight2.skill.enemy.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import fr.skytasul.guardianbeam.Laser;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AttackRadar extends AbstractEpicDragonSkill {
    private final int duration;
    private final int laserDuration;
    private final Map<Player, Laser.GuardianLaser> laserMap = new HashMap<>();

    public AttackRadar(@NotNull DragonFight fight) {
        super(fight, "attack-radar");
        this.duration = getSkillConfig().getInt("duration") ;
        this.laserDuration = getSkillConfig().getInt("laser-charge-duration-in-seconds");
    }

    @Override
    public int start() {
        getPlayerInWorld().forEach(p->{
            try {
                laserMap.put(p, new Laser.GuardianLaser(getDragon().getLocation(), p,laserDuration, 256));
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        });
        return this.duration + this.skillStartWaitingTicks();
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {
        laserMap.values().forEach(Laser::stop);
    }

    @Override
    public boolean tick() {
        updateLaserDragonPos();
        if (isWaitingStart()) {
            return false;
        }
        laserMap.forEach((p,laser)->{
            RayTraceResult result = getWorld().rayTrace(getDragon().getLocation(), fromToVector(getDragon().getLocation(), p.getLocation()),
                    256, FluidCollisionMode.NEVER,
                    true,
                    1.0,
                    entity -> entity == p);
            if(result == null) return;
            if(Objects.equals(p, result.getHitEntity())){
                p.damage(p.getMaxHealth()+1, getDragon());
            }
            if(result.getHitBlock() != null){
                getWorld().createExplosion(result.getHitBlock().getLocation(),7.0f,false, true,getDragon());
            }
            laser.stop();
        });
        laserMap.clear();
        return false;
    }

    private void updateLaserDragonPos() {
        laserMap.values().forEach(laser->{
            try {
                laser.moveStart(getDragon().getLocation());
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public int skillStartWaitingTicks() {
        return 20 * 5;
    }

    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_1,Stage.STAGE_2};
    }
}
