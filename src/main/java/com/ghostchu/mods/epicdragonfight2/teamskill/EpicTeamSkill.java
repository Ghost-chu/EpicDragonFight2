package com.ghostchu.mods.epicdragonfight2.teamskill;

import org.bukkit.command.CommandSender;

public interface EpicTeamSkill {
   String preAnnounce();
   boolean tick();
   boolean execute(CommandSender executor);
}
