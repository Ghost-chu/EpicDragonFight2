package com.ghostchu.mods.epicdragonfight2.skill.team;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.EpicDragonFight2;
import com.ghostchu.mods.epicdragonfight2.util.RandomUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public abstract class AbstractEpicTeamSkill implements EpicTeamSkill, Listener {
    @NotNull
    private final DragonFight fight;
    @NotNull
    private final Random random = new Random();
    private final String skillName;
    private final int chargeNeedTime;
    private int charged = 0;
    private boolean ready = false;

    public AbstractEpicTeamSkill(@NotNull DragonFight fight, String skillName) {
        this.fight = fight;
        this.skillName = skillName;
        this.chargeNeedTime = getSkillConfig().getInt("need-charge");
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public boolean tick() {
        charged++;
        if(charged % 200 == 0 && !ready){
            getPlugin().getLogger().info("团队终结技充能进度："+charged +" / "+chargeNeedTime);
        }
        if (charged >= chargeNeedTime) {
            if (!ready) {
                ready = true;
                BaseComponent[] components = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', String.format(getPlugin().getConfig().getString("team-skills.get-ready.broadcast"),getSkillConfig().getString("name"))));
                TextComponent component = new TextComponent(components);
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/edfteamskillactive "+fight.getUUID().toString()));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("点击激活 RCF 终结技")));
                getPlayerInWorld().forEach(p-> {
                    p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP,1,0);
                    p.spigot().sendMessage(component);
                });
                return true;
            }
        }
        return false;
    }

    public EpicDragonFight2 getPlugin() {
        return this.fight.getPlugin();
    }

    public String getSkillName() {
        return skillName;
    }

    public ConfigurationSection getSkillConfig() {
        return EpicDragonFight2.getInstance().getConfig().getConfigurationSection("team-skills").getConfigurationSection(skillName);
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
    private Player randomPlayer() {
        return this.fight.randomPlayer();
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
        broadcast(msg,executor);
    }

    public void broadcast(@NotNull String string, CommandSender sender) {
        this.fight.broadcast(string
                .replace("<dragon_name>", getDragon().getName())
                .replace("<player_name>", sender.getName()));
    }

    public void broadcastTitle(@NotNull String title, @NotNull String subTitle, CommandSender sender) {
        this.fight.broadcastTitle(title.replace("<dragon_name>", getDragon().getName()).replace("<player_name>", sender.getName()), subTitle.replace("<dragon_name>", getDragon().getName()));
    }

    public void broadcastActionBar(@NotNull String string, CommandSender sender) {
        this.fight.broadcastActionBar(string.replace("<dragon_name>", getDragon().getName()).replace("<player_name>", sender.getName()));
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

    public void applyDifficultRate(LivingEntity entity) {
        fight.applyDifficultRate(entity);
    }

    @Override
    public abstract boolean execute(CommandSender executor);

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


    public void markEntitySummonedByPlugin(Entity entity) {
        fight.markEntitySummonedByPlugin(entity);
    }

    public boolean isMarkedSummonedByPlugin(Entity entity) {
        return fight.isMarkedSummonedByPlugin(entity);
    }

}
