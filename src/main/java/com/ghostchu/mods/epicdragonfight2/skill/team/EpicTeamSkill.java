package com.ghostchu.mods.epicdragonfight2.skill.team;

import org.bukkit.command.CommandSender;

public interface EpicTeamSkill {
    String preAnnounce();

    boolean cycle();

    boolean execute(CommandSender executor);
}
