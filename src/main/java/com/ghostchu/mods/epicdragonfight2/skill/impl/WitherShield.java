package com.ghostchu.mods.epicdragonfight2.skill.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.SkillEndReason;
import com.ghostchu.mods.epicdragonfight2.util.RandomUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBarViewer;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public class WitherShield extends AbstractEpicDragonSkill {
    private final BossBar bossBar = BossBar.bossBar(Component.text("凋零护盾 - ???"), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    private int spawned = 0;

    public WitherShield(@NotNull DragonFight fight) {
        super(fight, "wither-shield");
    }

    @Override
    public int start() {
        spawned = 0;
        List<Player> playerList = getPlayerInWorld();
        for (int i = 0; i < Math.min(playerList.size(), 5); i++) {
            Player player = playerList.get(i);
            Wither wither = (Wither) getWorld().spawnEntity(player.getLocation().add(0, 20, 0), EntityType.WITHER);
            wither.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, false));
            markEntitySummonedByPlugin(wither);
            spawned++;
        }
        return Integer.MAX_VALUE;
    }


    @Override
    public boolean tick() {
        getPlayerInWorld().forEach(p -> bossBar.addViewer(p));
        List<Wither> withers = getAllWithers();
        bossBar.name(Component.text("凋零护盾 - " + withers.size() + " / " + spawned));
        bossBar.progress((float) withers.size() / spawned);
        return withers.isEmpty();
    }

    @EventHandler(ignoreCancelled = true)
    public void dragonAttacked(EntityDamageEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            event.setCancelled(true);
            event.setDamage(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void dragonAttacked(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            event.setCancelled(true);
            event.setDamage(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void dragonAttacked(EntityDamageByBlockEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            event.setCancelled(true);
            event.setDamage(0);
        }
    }


    public List<Wither> getAllWithers() {
        return getWorld().getEntitiesByClass(Wither.class)
                .stream()
                .filter(this::isMarkedSummonedByPlugin)
                .toList();
    }

    @EventHandler(ignoreCancelled = true)
    public void witherTargeting(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Wither)) {
            return;
        }
        if (!(event.getTarget() instanceof Player)) {
            // random a target
            Player player = RandomUtil.randomPick(getPlayerInWorld());
            if (player == null) {
                event.setCancelled(true);
                event.setTarget(null);
            } else {
                event.setTarget(player);
            }
        }
    }

    @Override
    public long skillStartWaitingTicks() {
        return 0;
    }

    @Override
    public @NotNull Stage[] getAdaptStages() {
        return Stage.values();
    }

    @Override
    public void end(@NotNull SkillEndReason var1) {
        getAllWithers().forEach(Entity::remove);
        Iterator<? extends BossBarViewer> viewers = bossBar.viewers().iterator();
        while (viewers.hasNext()) {
            viewers.next();
            viewers.remove();
        }
        getPlayerInWorld().forEach(p -> p.playSound(p, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0));
        broadcast("凋零护盾已被击破！");
    }


}
