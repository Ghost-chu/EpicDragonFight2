package com.ghostchu.mods.epicdragonfight2.skill;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.EpicDragonFight2;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class AbstractEpicSkill {
    private final DragonFight fight;
    private final String skillName;
    @NotNull
    private final Random random = new Random();

    public AbstractEpicSkill(@NotNull DragonFight fight, String skillName) {
        this.fight = fight;
        this.skillName = skillName;
    }

    public EpicDragonFight2 getPlugin() {
        return this.fight.getPlugin();
    }

    public String getSkillName() {
        return skillName;
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
        this.fight.broadcast(string);
    }

    public void broadcastTitle(@NotNull String title, @NotNull String subTitle) {
        this.fight.broadcastTitle(title, subTitle);
    }

    public void broadcastActionBar(@NotNull String string) {
        this.fight.broadcastActionBar(string);
    }

    public List<Player> randomPlayers() {
        return this.fight.randomPlayers();
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
        entity.setMetadata("summon_by_edf2", new FixedMetadataValue(getPlugin(), true));
        fight.markEntitySummonedByPlugin(entity);
    }

    public boolean isMarkedSummonedByPlugin(Entity entity) {
        if (entity.hasMetadata("summon_by_edf2")) {
            return true;
        }
        return fight.isMarkedSummonedByPlugin(entity);
    }
}
