package com.ghostchu.mods.epicdragonfight2.skill.control;

import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.EpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.passive.EpicPassiveSkill;
import com.ghostchu.mods.epicdragonfight2.skill.team.EpicTeamSkill;
import com.ghostchu.mods.epicdragonfight2.util.ClazzScanner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SkillController {
    private final Logger logger;
    private EpicTeamSkill currentTeamSkill;
    private EpicDragonSkill currentDragonSkill;
    private List<EpicPassiveSkill> currentPassiveSkills = new ArrayList<>();
    private Map<Stage, List<Class<? extends EpicDragonSkill>>> stageAvailableDragonSkills = new HashMap<>();
    private List<Class<? extends EpicDragonSkill>> availableTeamSkills = new ArrayList<>();
    private List<Class<? extends EpicPassiveSkill>> availablePassiveSkills = new ArrayList<>();

    public SkillController(Logger logger){
        this.logger = logger;
        scanSkills();
    }


    public void setCurrentDragonSkill(EpicDragonSkill currentDragonSkill) {
        this.currentDragonSkill = currentDragonSkill;
    }

    public void setCurrentTeamSkill(EpicTeamSkill currentTeamSkill) {
        this.currentTeamSkill = currentTeamSkill;
    }

    public EpicDragonSkill getCurrentDragonSkill() {
        return currentDragonSkill;
    }

    public EpicTeamSkill getCurrentTeamSkill() {
        return currentTeamSkill;
    }

    private void scanSkills(){
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
            }catch (Exception e){
                logger.log(Level.WARNING,"跳过 "+c.getName()+" 技能注册：注册时出现错误，是否是不合法的技能？", e);
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
        logger.info("已注册：团队技能 ["+c.getName()+"]");
    }

    private void handleDragonSkillLoad(Class<?> c) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = c.getDeclaredMethod("getAdaptStages");
        Stage[] acceptedStages = (Stage[]) method.invoke(null);
        for (Stage acceptedStage : acceptedStages) {
            List<Class<? extends EpicDragonSkill>> registered =stageAvailableDragonSkills.getOrDefault(acceptedStage, new ArrayList<>());
            //noinspection unchecked
            registered.add((Class<? extends EpicDragonSkill>) c);
            stageAvailableDragonSkills.put(acceptedStage,registered);
            logger.info("已注册：敌对技能 ["+c.getName()+"]");
        }
    }


}
