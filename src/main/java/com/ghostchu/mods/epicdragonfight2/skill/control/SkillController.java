package com.ghostchu.mods.epicdragonfight2.skill.control;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.EpicSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.EpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import com.ghostchu.mods.epicdragonfight2.skill.passive.EpicPassiveSkill;
import com.ghostchu.mods.epicdragonfight2.skill.team.EpicTeamSkill;
import com.ghostchu.mods.epicdragonfight2.util.ClazzScanner;
import com.ghostchu.mods.epicdragonfight2.util.RandomUtil;
import org.apache.commons.lang3.StringUtils;
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
    private List<Class<? extends EpicTeamSkill>> availableTeamSkills = new ArrayList<>();
    private List<Class<? extends EpicPassiveSkill>> availablePassiveSkills = new ArrayList<>();
    private int emptyWindowForTeamSkills;
    private int emptyWindowForDragonSkills;

    private Class<? extends EpicDragonSkill> lastSelectedDragonSkill = null;

    public SkillController(Logger logger, DragonFight fight) {
        this.logger = logger;
        this.fight = fight;
        scanSkills();
    }

    public void tick() {
        try {
            tickStage();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        try {
            tickPassiveSkills();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        //tickTeamSkills();
        try {
            tickDragonSkills();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        try {
            assignNewPassiveSkill();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        //assignNewTeamSkill();
        try {
            assignNewDragonSkill(true);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        try {
            removeDragonDamageCooldown();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void removeDragonDamageCooldown() {
        if (currentStage == Stage.STAGE_5) {
            fight.getDragon().setNoDamageTicks(0);
        }
    }

    private void assignNewDragonSkill(boolean canReSelect) {
        if (!currentDragonSkills.isEmpty()) return;
        emptyWindowForDragonSkills++;
        if (emptyWindowForDragonSkills >= fight.getPlugin().getConfig().getInt("skill-pick-latency")) {
            emptyWindowForDragonSkills = 0;
            EpicDragonSkill dragonSkill = spawnNewInstance(RandomUtil.randomPick(stageAvailableDragonSkills.getOrDefault(currentStage, Collections.emptyList())));
            if (dragonSkill != null) {
                if (dragonSkill.getClass() == lastSelectedDragonSkill) {
                    if (canReSelect) {
                        logger.info("技能重复，尝试随机新的技能……");
                        assignNewDragonSkill(false);
                        return;
                    }
                }
                lastSelectedDragonSkill = dragonSkill.getClass();
                currentDragonSkills.add(dragonSkill);
                fight.broadcast(dragonSkill.preAnnounce());
                logger.info("已安装技能 " + dragonSkill.getClass().getName());
            }
        }
    }

    private void assignNewTeamSkill() {
        if (!currentTeamSkills.isEmpty()) return;
        emptyWindowForTeamSkills++;
        if (emptyWindowForTeamSkills >= fight.getPlugin().getConfig().getInt("skill-pick-latency")) {
            emptyWindowForTeamSkills = 0;
            EpicTeamSkill teamSkill = spawnNewInstance(RandomUtil.randomPick(availableTeamSkills));
            if (teamSkill != null) {
                currentTeamSkills.add(teamSkill);
            }
        }
    }

    private <T> T spawnNewInstance(Class<T> clazz) {
        if (clazz == null) return null;
        try {
            return clazz.getDeclaredConstructor(DragonFight.class).newInstance(fight);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void assignNewPassiveSkill() {
    }

    private void tickStage() {
        if (currentStage == null) {
            currentStage = Stage.values()[stagePos];
            broadcastStageMessage();
            logger.info("Stage switched to " + currentStage.name());
            return;
        }
        double percent = fight.getDragon().getHealth() / fight.getDragon().getMaxHealth();
        if (percent <= currentStage.getPercentage()) {
            if ((stagePos + 1) < Stage.values().length) {
                stagePos++;
                currentStage = Stage.values()[stagePos];
                fight.getPlayerInWorld().forEach(p -> p.playSound(p, Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.0f));
                logger.info("Stage switched to " + currentStage.name());
                broadcastStageMessage();
            }
        }
    }

    private void broadcastStageMessage() {
        String minimessage = fight.getPlugin().getConfig().getString("stage-messages." + currentStage.name());
        if (StringUtils.isNotEmpty(minimessage)) {
            fight.broadcast(minimessage);
        }
    }

    private void tickDragonSkills() {
        List<Class<? extends EpicDragonSkill>> availableSkills = stageAvailableDragonSkills.getOrDefault(currentStage, Collections.emptyList());
        currentDragonSkills.removeIf(epicDragonSkill -> {
            if (!availableSkills.contains(epicDragonSkill.getClass())) {
                epicDragonSkill.end(SkillEndReason.STAGE_SWITCH);
                epicDragonSkill.unregister();
                return true;
            }
            if (epicDragonSkill.cycle()) {
                // epicDragonSkill.end(SkillEndReason.SKILL_ENDED);
                epicDragonSkill.unregister();
                return true;
            }
            return false;
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
            logger.info("Handle " + c.getName());
            try {
                if (EpicDragonSkill.class.isAssignableFrom(c)) {
                    logger.info("Registered as DragonSkill " + c.getName());
                    handleDragonSkillLoad(c);
                }
                if (EpicTeamSkill.class.isAssignableFrom(c)) {
                    logger.info("Registered as TeamSkill " + c.getName());
                    handleTeamSkillLoad(c);
                }
                if (EpicPassiveSkill.class.isAssignableFrom(c)) {
                    logger.info("Registered as PassiveSkill " + c.getName());
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
        availableTeamSkills.add((Class<? extends EpicTeamSkill>) c);
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
