package com.ghostchu.mods.epicdragonfight2.skill.enemy;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.EpicDragonFight2;
import com.ghostchu.mods.epicdragonfight2.util.RandomUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public abstract class AbstractEpicDragonSkill implements EpicDragonSkill, Listener {
    @NotNull
    private final DragonFight fight;
    @NotNull
    private final Random random = new Random();
    private final String skillName;
    private int ticker = 0;
    private boolean listenerRegistered = false;
    private int duration = 0;

    private boolean started;
    private boolean ended;

    public AbstractEpicDragonSkill(@NotNull DragonFight fight, String skillName) {
        this.fight = fight;
        this.skillName = skillName;
    }


    public EpicDragonFight2 getPlugin() {
        return this.fight.getPlugin();
    }

    public String getSkillName() {
        return skillName;
    }

    public ConfigurationSection getSkillConfig() {
        return EpicDragonFight2.getInstance().getConfig().getConfigurationSection("skills").getConfigurationSection(skillName);
    }

    public Vector fromToVector(Location toLocation, Location fromLocation) {
        Vector dir = toLocation.subtract(fromLocation).toVector().normalize();
        dir.multiply(2);
        return dir;
    }

    @NotNull
    public DragonFight getFight() {
        return this.fight;
    }

    @NotNull
    protected List<Player> getPlayerInWorld() {
        return this.fight.getPlayerInWorld();
    }

    @Nullable
    public Player randomPlayer() {
        return this.fight.randomPlayer();
    }


    public void broadcast(@NotNull String string) {
        this.fight.broadcast(string.replace("<dragon_name>", getDragon().getName()));
    }

    public void broadcastTitle(@NotNull String title, @NotNull String subTitle) {
        this.fight.broadcastTitle(title.replace("<dragon_name>", getDragon().getName()), subTitle.replace("<dragon_name>", getDragon().getName()));
    }

    public void broadcastActionBar(@NotNull String string) {
        this.fight.broadcastActionBar(string.replace("<dragon_name>", getDragon().getName()));
    }

    @NotNull
    public World getWorld() {
        return this.fight.getWorld();
    }

    @NotNull
    public EnderDragon getDragon() {
        return this.fight.getDragon();
    }

    @NotNull
    public Random getRandom() {
        return this.random;
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

    public void applyDifficultRate(LivingEntity entity) {
        fight.applyDifficultRate(entity);
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

    public Location getFalloutPosition(Entity entity) {
        Location eLoc = entity.getLocation();
        ChunkSnapshot snapshot = entity.getLocation().getChunk().getChunkSnapshot();
        int chunkXOffset = eLoc.getBlockX() & 0xF;
        int chunkZOffset = eLoc.getBlockZ() & 0xF;
        int highestY = snapshot.getHighestBlockYAt(chunkXOffset, chunkZOffset);
        int worldMinHeight = entity.getWorld().getMinHeight();
        if (highestY > entity.getLocation().getBlockY()) {
            highestY = entity.getLocation().getBlockY();
            for (int i = highestY; i >= worldMinHeight; i--) {
                if (snapshot.getBlockType(chunkXOffset, i, chunkZOffset).isAir()) {
                    continue;
                }
                highestY = i;
                break;
            }
        }
        Location drawAt = entity.getLocation().clone();
        drawAt.setY(highestY + 0.15f);
        return drawAt;
    }

    public void summonCircle(Location location, int size) {
        for (int d = 0; d <= 90; d += 1) {
            Location particleLoc = new Location(location.getWorld(), location.getX(), location.getY(), location.getZ());
            particleLoc.setX(location.getX() + Math.cos(d) * size);
            particleLoc.setZ(location.getZ() + Math.sin(d) * size);
            location.getWorld().spawnParticle(Particle.WATER_DROP, particleLoc, 1);
        }
    }

    public int createDurationLoopTask(@NotNull Runnable runnable, long duration, @Nullable Supplier<Boolean> shouldBreak, @Nullable Supplier<Boolean> shouldSkip) {
        AtomicInteger loop = new AtomicInteger(0);
        AtomicInteger taskId = new AtomicInteger(-1);
        if (shouldBreak == null) {
            shouldBreak = () -> !this.fight.isValid();
        }
        @Nullable Supplier<Boolean> finalShouldBreak = shouldBreak;
        int cpId = Bukkit.getScheduler().runTaskTimer(EpicDragonFight2.getInstance(), () -> {
            if (finalShouldBreak.get()) {
                Bukkit.getScheduler().cancelTask(taskId.get());
                return;
            }
            if (shouldSkip == null) {
                runnable.run();
            } else if (!shouldSkip.get()) {
                runnable.run();
            }
            int times = loop.incrementAndGet();
            if ((long) times >= duration) {
                Bukkit.getScheduler().cancelTask(taskId.get());
            }
        }, 0L, 1L).getTaskId();
        taskId.set(cpId);
        return cpId;
    }

    public void markEntitySummonedByPlugin(Entity entity) {
        fight.markEntitySummonedByPlugin(entity);
    }

    public boolean isMarkedSummonedByPlugin(Entity entity) {
        return fight.isMarkedSummonedByPlugin(entity);
    }

    public int createDurationListener(@NotNull Listener listener, long duration, @Nullable Supplier<Boolean> shouldStop) {
        AtomicInteger loop = new AtomicInteger(0);
        AtomicInteger taskId = new AtomicInteger(-1);
        if (shouldStop == null) {
            shouldStop = () -> !this.fight.isValid();
        }
        Bukkit.getPluginManager().registerEvents(listener, EpicDragonFight2.getInstance());
        @Nullable Supplier<Boolean> finalShouldStop = shouldStop;
        int cpId = Bukkit.getScheduler().runTaskTimer(EpicDragonFight2.getInstance(), () -> {
            if (finalShouldStop.get()) {
                HandlerList.unregisterAll(listener);
                Bukkit.getScheduler().cancelTask(taskId.get());
            }
            if ((long) loop.incrementAndGet() >= duration) {
                HandlerList.unregisterAll(listener);
                Bukkit.getScheduler().cancelTask(taskId.get());
            }
        }, 0L, 1L).getTaskId();
        taskId.set(cpId);
        return cpId;
    }
}
