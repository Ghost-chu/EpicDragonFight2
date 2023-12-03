package com.ghostchu.mods.epicdragonfight2.skill.enemy;

import org.jetbrains.annotations.NotNull;

public interface EpicDragonSkill {
    String preAnnounce();

    int skillStartWaitingTicks();

    boolean cycle();

    void unregister();

    void end(@NotNull SkillEndReason var1);

    boolean isEnded();

    boolean isStarted();

}
