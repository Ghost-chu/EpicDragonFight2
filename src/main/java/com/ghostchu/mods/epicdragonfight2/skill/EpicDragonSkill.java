package com.ghostchu.mods.epicdragonfight2.skill;

import com.ghostchu.mods.epicdragonfight2.Stage;
import org.jetbrains.annotations.NotNull;

public interface EpicDragonSkill {
    String preAnnounce();

    int skillStartWaitingTicks();

    @NotNull Stage[] getAdaptStages();

    boolean cycle();

    void unregister();

    void end(@NotNull SkillEndReason var1);

}
