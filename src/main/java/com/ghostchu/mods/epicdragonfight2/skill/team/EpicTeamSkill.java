package com.ghostchu.mods.epicdragonfight2.skill.team;

import org.bukkit.command.CommandSender;

public interface EpicTeamSkill {
   String preAnnounce();
   boolean tick();
   boolean execute(CommandSender executor);
}
