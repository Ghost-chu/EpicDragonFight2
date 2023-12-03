package com.ghostchu.mods.epicdragonfight2.skill.passive;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.EpicDragonFight2;
import com.ghostchu.mods.epicdragonfight2.skill.AbstractEpicSkill;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public abstract class AbstactEpicPassiveSkill extends AbstractEpicSkill implements EpicPassiveSkill {

    private final String skillName;

    public AbstactEpicPassiveSkill(@NotNull DragonFight fight, String skillName) {
        super(fight, skillName);
        this.skillName = skillName;
    }

    public boolean cycle() {
        return tick();
    }

    public ConfigurationSection getSkillConfig() {
        return EpicDragonFight2.getInstance().getConfig().getConfigurationSection("passive-skills").getConfigurationSection(skillName);
    }


    @Override
    public abstract boolean tick();
}
