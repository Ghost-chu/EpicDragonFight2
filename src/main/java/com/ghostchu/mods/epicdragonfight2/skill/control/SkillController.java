package com.ghostchu.mods.epicdragonfight2.skill.control;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.EpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import com.ghostchu.mods.epicdragonfight2.skill.passive.EpicPassiveSkill;
import com.ghostchu.mods.epicdragonfight2.skill.team.EpicTeamSkill;
import com.ghostchu.mods.epicdragonfight2.util.ClazzScanner;
import com.ghostchu.mods.epicdragonfight2.util.RandomUtil;
import org.bukkit.Sound;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SkillController {
    private final Logger logger;
    private final DragonFight fight;
    private Stage currentStage = null;
    private int stagePos = 0;
    private List<EpicTeamSkill> currentTeamSkills = new ArrayList<>();
    private List<EpicDragonSkill> currentDragonSkills = new ArrayList<>();
    private List<EpicPassiveSkill> currentPassiveSkills = new ArrayList<>();
    private Map<Stage, List<Class<? extends EpicDragonSkill>>> stageAvailableDragonSkills = new HashMap<>();
    private List<Class<? extends EpicDragonSkill>> availableTeamSkills = new ArrayList<>();
    private List<Class<? extends EpicPassiveSkill>> availablePassiveSkills = new ArrayList<>();
    private int emptyWindowForTeamSkills;
    private int emptyWindowForDragonSkills;


    public SkillController(Logger logger, DragonFight fight) {
        this.logger = logger;
        this.fight = fight;
        scanSkills();
    }

    public void tick() {
        tickStage();
        tickPassiveSkills();
        tickTeamSkills();
        tickDragonSkills();
        assignNewPassiveSkill();
        assignNewTeamSkill();
        assignNewDragonSkill();
    }

    private void assignNewDragonSkill() {
        if(!currentDragonSkills.isEmpty())return;
        emptyWindowForDragonSkills++;
        if(emptyWindowForDragonSkills >= fight.getPlugin().getConfig().getInt("skill-pick-latency")){
            emptyWindowForDragonSkills = 0;
            EpicDragonSkill dragonSkill = spawnNewInstance(RandomUtil.randomPick(stageAvailableDragonSkills.getOrDefault(currentStage, Collections.emptyList())));
            if(dragonSkill != null) {
                currentDragonSkills.add(dragonSkill);
            }
        }
    }

    private void assignNewTeamSkill() {
        if(!currentTeamSkills.isEmpty())return;
        emptyWindowForTeamSkills ++;
        if(emptyWindowForTeamSkills >= fight.getPlugin().getConfig().getInt("skill-pick-latency")){
            emptyWindowForTeamSkills = 0;
            EpicDragonSkill teamSkill = spawnNewInstance(RandomUtil.randomPick(availableTeamSkills));
            if(teamSkill != null) {
                currentDragonSkills.add(teamSkill);
            }
        }
    }

    private <T> T spawnNewInstance(Class<T> clazz){
        if(clazz == null) return null;
        try {
            return clazz.getDeclaredConstructor(DragonFight.class).newInstance(fight);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void assignNewPassiveSkill() {
    }

    private void tickStage() {
        if (currentStage == null) {
            currentStage = Stage.values()[stagePos];
            return;
        }
        double percent = fight.getDragon().getHealth() / fight.getDragon().getMaxHealth();
        if (percent <= currentStage.getPercentage()) {
            stagePos++;
        }
        if (stagePos < Stage.values().length) {
            currentStage = Stage.values()[stagePos];
        }
        fight.getPlayerInWorld().forEach(p -> p.playSound(p, Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.0f));
    }

    private void tickDragonSkills() {
        List<Class<? extends EpicDragonSkill>> availableSkills = stageAvailableDragonSkills.getOrDefault(currentStage, Collections.emptyList());
        currentDragonSkills.removeIf(epicDragonSkill -> {
            if (!availableSkills.contains(epicDragonSkill.getClass())) {
                epicDragonSkill.end(SkillEndReason.STAGE_SWITCH);
                epicDragonSkill.unregister();
                return true;
            }
            return !epicDragonSkill.cycle();
        });
    }

    private void tickTeamSkills() {
        currentTeamSkills.removeIf(epicTeamSkill -> !epicTeamSkill.cycle());
    }

    private void tickPassiveSkills() {
        this.currentPassiveSkills.forEach(EpicPassiveSkill::tick);
    }

    private void scanSkills() {
        List<Class<?>> classList = new ClazzScanner("com.ghostchu.mods.epicdragonfight2.skill").getScanResult();
        for (Class<?> c : classList) {
            try {
                if (c.isAssignableFrom(EpicDragonSkill.class)) {
                    handleDragonSkillLoad(c);
                }
                if (c.isAssignableFrom(EpicTeamSkill.class)) {
                    handleTeamSkillLoad(c);
                }
                if (c.isAssignableFrom(EpicPassiveSkill.class)) {
                    handlePassiveSkillLoad(c);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "跳过 " + c.getName() + " 技能注册：注册时出现错误，是否是不合法的技能？", e);
            }
        }
    }

    private void handlePassiveSkillLoad(Class<?> c) {
        //noinspection unchecked
        availablePassiveSkills.add((Class<? extends EpicPassiveSkill>) c);
    }

    private void handleTeamSkillLoad(Class<?> c) {
        //Method method = c.getDeclaredMethod("getAdaptStages");
        //Stage[] acceptedStages = (Stage[]) method.invoke(null);
//        for (Stage acceptedStage : acceptedStages) {
//            List<Class<? extends EpicDragonSkill>> registered =stageAvailableDragonSkills.getOrDefault(acceptedStage, new ArrayList<>());
//            //noinspection unchecked
//            registered.add((Class<? extends EpicDragonSkill>) c);
//            stageAvailableDragonSkills.put(acceptedStage,registered);
//        }
        //noinspection unchecked
        availableTeamSkills.add((Class<? extends EpicDragonSkill>) c);
        logger.info("已注册：团队技能 [" + c.getName() + "]");
    }

    private void handleDragonSkillLoad(Class<?> c) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = c.getDeclaredMethod("getAdaptStages");
        Stage[] acceptedStages = (Stage[]) method.invoke(null);
        for (Stage acceptedStage : acceptedStages) {
            List<Class<? extends EpicDragonSkill>> registered = stageAvailableDragonSkills.getOrDefault(acceptedStage, new ArrayList<>());
            //noinspection unchecked
            registered.add((Class<? extends EpicDragonSkill>) c);
            stageAvailableDragonSkills.put(acceptedStage, registered);
            logger.info("已注册：敌对技能 [" + c.getName() + "]");
        }
    }


}
