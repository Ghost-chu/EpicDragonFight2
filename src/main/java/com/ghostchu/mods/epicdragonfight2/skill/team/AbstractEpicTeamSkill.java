package com.ghostchu.mods.epicdragonfight2.skill.team;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.EpicDragonFight2;
import com.ghostchu.mods.epicdragonfight2.skill.AbstractEpicSkill;
import com.ghostchu.mods.epicdragonfight2.util.RandomUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public abstract class AbstractEpicTeamSkill extends AbstractEpicSkill implements EpicTeamSkill, Listener {
    @NotNull
    private final DragonFight fight;
    @NotNull
    private final Random random = new Random();
    private final String skillName;
    private final int chargeNeedTime;
    private int charged = 0;
    private boolean ready = false;

    public AbstractEpicTeamSkill(@NotNull DragonFight fight, String skillName) {
        super(fight, skillName);
        this.fight = fight;
        this.skillName = skillName;
        this.chargeNeedTime = getSkillConfig().getInt("need-change");
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public boolean cycle() {
        charged++;
        if (charged % 200 == 0 && !ready) {
            getPlugin().getLogger().info("团队终结技充能进度：" + charged + " / " + chargeNeedTime);
        }
        if (charged >= chargeNeedTime) {
            if (!ready) {
                ready = true;
                BaseComponent[] components = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', String.format(getPlugin().getConfig().getString("team-skills.get-ready.broadcast"), getSkillConfig().getString("name"))));
                TextComponent component = new TextComponent(components);
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/edfteamskillactive " + fight.getUUID().toString()));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("点击激活 RCF 终结技")));
                getPlayerInWorld().forEach(p -> {
                    p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 0);
                    p.spigot().sendMessage(component);
                });
                return true;
            }
        }
        return false;
    }


    public ConfigurationSection getSkillConfig() {
        return EpicDragonFight2.getInstance().getConfig().getConfigurationSection("team-skills").getConfigurationSection(skillName);
    }

    public void broadcastUseMessage(CommandSender executor) {
        String msg;
        if (getSkillConfig().isList("broadcast")) {
            msg = RandomUtil.randomPick(getSkillConfig().getStringList("broadcast"));
        } else {
            msg = getSkillConfig().getString("broadcast", "");
        }
        msg = String.format(msg, getSkillConfig().getString("name"));
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        broadcast(msg);
    }

    @Override
    public String preAnnounce() {
        if (getSkillConfig().isList("broadcast")) {
            List<String> broadcastEntries = getSkillConfig().getStringList("broadcast");
            int selected = random.nextInt(broadcastEntries.size());
            return broadcastEntries.get(selected);
        } else {
            return getSkillConfig().getString("broadcast", "");
        }
    }


    @Override
    public abstract boolean execute(CommandSender executor);

}
