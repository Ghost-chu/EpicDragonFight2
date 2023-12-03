package com.ghostchu.mods.epicdragonfight2.skill.enemy;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.EpicDragonFight2;
import com.ghostchu.mods.epicdragonfight2.skill.AbstractEpicSkill;
import com.ghostchu.mods.epicdragonfight2.util.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractEpicDragonSkill extends AbstractEpicSkill implements EpicDragonSkill, Listener {
    private int ticker = 0;
    private boolean listenerRegistered = false;
    private int duration = 0;
    private boolean started;
    private boolean ended;

    public AbstractEpicDragonSkill(@NotNull DragonFight fight, String skillName) {
        super(fight, skillName);
    }

    public ConfigurationSection getSkillConfig() {
        return EpicDragonFight2.getInstance().getConfig().getConfigurationSection("skills")
                .getConfigurationSection(getSkillName());
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getTick() {
        return this.ticker;
    }

    public boolean isFreshInstalled() {
        return this.ticker == 0;
    }

    @Override
    public String preAnnounce() {
        if (getSkillConfig().isList("broadcast")) {
            return RandomUtil.randomPick(getSkillConfig().getStringList("broadcast"));
        } else {
            return getSkillConfig().getString("broadcast", "");
        }
    }

    public void registerListener() {
        if (!this.listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(this, EpicDragonFight2.getInstance());
            this.listenerRegistered = true;
        }
    }

    public int getCleanTick() {
        return getTick() - skillStartWaitingTicks();
    }

    public boolean isWaitingStart() {
        return getTick() < skillStartWaitingTicks();
    }


    public void unregisterListener() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean cycle() {
        if (this.isFreshInstalled()) {
            this.duration = this.start();
            setStarted(true);
            this.register();
            ++this.ticker;
            return false;
        }
        if (this.ticker > this.duration) {
            this.unregister();
            this.end(SkillEndReason.REACH_TIME_LIMIT);
            setEnded(true);
            return true;
        }
        boolean result = this.tick();
        ++this.ticker;
        if (result) {
            this.unregister();
            this.end(SkillEndReason.SKILL_ENDED);
            setEnded(true);
        }
        return result;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    @Override
    public boolean isEnded() {
        return ended;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    private void register() {
        this.registerListener();
    }

    public void unregister() {
        this.unregisterListener();
    }

    public abstract int start();

    public abstract void end(@NotNull SkillEndReason var1);

    public abstract boolean tick();


}
