package com.ghostchu.mods.epicdragonfight2;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class EpicDragonFight2 extends JavaPlugin implements Listener {
    private static EpicDragonFight2 instance;
    public final NamespacedKey PLUGIN_ENTITY_MARKER = new NamespacedKey(this, "summon");
    private final Set<DragonFight> dragonFightList = new HashSet<>();
    private final Set<World> worlds = new HashSet<>();

    @NotNull
    public static EpicDragonFight2 getInstance() {
        return instance;
    }

    @NotNull
    public static String getSource(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return "不支持的追踪类型";
        }
        return switch (event.getCause()) {
            case MAGIC -> "药水";
            case VOID -> "虚空";
            case LAVA -> "岩浆";
            case FIRE, FIRE_TICK -> "火焰";
            case FALL -> "坠落";
            case DRYOUT -> "干涸";
            case POISON -> "中毒";
            case THORNS -> "荆棘";
            case WITHER -> "凋零";
            case CONTACT -> "碰撞";
            case MELTING -> "熔化";
            case SUICIDE -> "自杀";
            case CRAMMING -> "挤压";
            case DROWNING -> "溺水";
            case HOT_FLOOR -> "岩浆地面";
            case LIGHTNING -> "闪电";
            case STARVATION -> "饥饿";
            case DRAGON_BREATH -> "龙息";
            case SUFFOCATION -> "窒息";
            case FALLING_BLOCK -> "坠落挤压";
            case FLY_INTO_WALL -> "撞击";
            case KILL -> "击杀";
            case FREEZE -> "冻结";
            case SONIC_BOOM -> "音爆";
            case WORLD_BORDER -> "世界边界";
            case CUSTOM -> "插件自定义";
            case PROJECTILE -> {
                if (event instanceof EntityDamageByEntityEvent) {
                    Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                    if (damager instanceof Projectile) {
                        if (((Projectile) damager).getShooter() instanceof Player) {
                            yield ((Player) ((Projectile) damager).getShooter()).getName();
                        }
                        yield damager.getName();
                    }
                    yield damager.getName();
                }
                yield "未知抛射物伤害源";
            }
            case BLOCK_EXPLOSION -> {
                if (event instanceof EntityDamageByBlockEvent) {
                    Block damager = ((EntityDamageByBlockEvent) event).getDamager();
                    if (damager == null) {
                        yield "爆炸";
                    }
                    if (Tag.BEDS.isTagged(damager.getType())) {
                        yield "床爆炸";
                    }
                    if (damager.getType() == Material.RESPAWN_ANCHOR) {
                        yield "重生锚爆炸";
                    }
                    yield damager.getType().name();
                }
                yield "未知方块爆炸伤害源";
            }
            case ENTITY_EXPLOSION, ENTITY_SWEEP_ATTACK, ENTITY_ATTACK -> {
                if (event instanceof EntityDamageByEntityEvent) {
                    Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                    if (damager instanceof TNTPrimed) {
                        yield "TNT";
                    }
                    if (damager instanceof LightningStrike) {
                        yield "闪电";
                    }
                    if (damager instanceof Fireball) {
                        yield "火球";
                    }
                    if (damager instanceof Creeper) {
                        yield "苦力怕";
                    }
                    if (damager instanceof EnderCrystal) {
                        yield "末影水晶";
                    }
                    if (damager instanceof Player) {
                        yield damager.getName();
                    }
                    if (damager instanceof Projectile) {
                        if (((Projectile) damager).getShooter() instanceof Player) {
                            yield ((Player) ((Projectile) damager).getShooter()).getName();
                        }
                        yield damager.getName();
                    }
                    if (damager instanceof Tameable) {
                        AnimalTamer tamer = ((Tameable) damager).getOwner();
                        if (tamer == null || tamer.getName() == null) {
                            yield damager.getName();
                        }
                        yield tamer.getName();
                    }
                    yield damager.getName();
                }
                yield "未知实体伤害源";
            }
        };
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        instance = this;
        this.getLogger().info("SUPER POWER - EPIC DRAGON FIGHT - LOADED!");
//        Bukkit.getScheduler().runTaskTimer(this, this::randomTick, this.getConfig().getInt("random-tick-period"), this.getConfig().getInt("random-tick-period"));
        Bukkit.getScheduler().runTaskTimer(this, () -> this.dragonFightList.forEach(DragonFight::tick), 0L, 1L);
        Bukkit.getPluginManager().registerEvents(this, this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        rescanAndRegisterExistsFights();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        rescanAndRegisterExistsFights();
    }

    private void rescanAndRegisterExistsFights() {
        Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.THE_END)
                .forEach(w -> {
                    Collection<EnderDragon> enderDragons = w.getEntitiesByClass(EnderDragon.class);
                    if (!enderDragons.isEmpty() && !this.worlds.contains(w)) {
                        EnderDragon dragon = enderDragons.iterator().next();
                        if (dragon.getPersistentDataContainer().getOrDefault(PLUGIN_ENTITY_MARKER, PersistentDataType.BOOLEAN, false)) {
                            this.worlds.add(w);
                            DragonFight fight = new DragonFight(this, UUID.randomUUID(), w, dragon);
                            this.registerFight(fight);
                            getLogger().info("已重新注册 " + w.getName() + " 的龙战");
                        }
                    }
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntity(EntityDamageEvent event) {
        if (event.getEntity().getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }
        if (!this.getConfig().getString("summon-entity-name", "Codusk").equals(event.getEntity().getCustomName())) {
            return;
        }
        World world = event.getEntity().getWorld();
        if (!this.getConfig().getStringList("worlds").contains(world.getName())) {
            return;
        }
        if (event instanceof EntityDamageByEntityEvent) {
            if (!(((EntityDamageByEntityEvent) event).getDamager() instanceof Player)) {
                event.setDamage(0.0);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntity(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player && event.getItem().getItemStack().getType() == Material.DRAGON_EGG) {
            ItemMeta meta = event.getItem().getItemStack().getItemMeta();
            if (meta == null) {
                return;
            }
            if (meta.getPersistentDataContainer().has(new NamespacedKey(this, "pick-player"), PersistentDataType.STRING)) {
                return;
            }
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "pick-player"), PersistentDataType.STRING, event.getEntity().getUniqueId().toString());
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "pick-date"), PersistentDataType.LONG, System.currentTimeMillis());
            ItemStack stack = event.getItem().getItemStack();
            stack.setItemMeta(meta);
            event.getItem().setItemStack(stack);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntity(EntityDeathEvent event) {
        if (event.getEntity().getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (event.getEntity().hasAI()) {
            return;
        }
        if (!this.getConfig().getString("summon-entity-name", "Codusk").equals(event.getEntity().getCustomName())) {
            return;
        }
        World world = event.getEntity().getWorld();
        if (!this.getConfig().getStringList("worlds").contains(world.getName())) {
            return;
        }
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) event.getEntity().getLastDamageCause()).getDamager() instanceof Player) {
            for (DragonFight dragonFight : this.dragonFightList) {
                if (!dragonFight.getWorld().equals(world)) continue;
                this.getLogger().warning("World " + world.getName() + " already registered!");
                return;
            }
            world.getEntitiesByClass(EnderDragon.class).forEach(Entity::remove);
            world.spawnEntity(this.getCenterBlockLocation(this.findHighestBedrock(world, 0, 3)), EntityType.ENDER_CRYSTAL).setInvulnerable(true);
            world.spawnEntity(this.getCenterBlockLocation(this.findHighestBedrock(world, 0, -3)), EntityType.ENDER_CRYSTAL).setInvulnerable(true);
            world.spawnEntity(this.getCenterBlockLocation(this.findHighestBedrock(world, 3, 0)), EntityType.ENDER_CRYSTAL).setInvulnerable(true);
            world.spawnEntity(this.getCenterBlockLocation(this.findHighestBedrock(world, -3, 0)), EntityType.ENDER_CRYSTAL).setInvulnerable(true);
            world.getEnderDragonBattle().initiateRespawn();
            this.worlds.add(world);
        }
    }

    private Location findHighestBedrock(World world, int x, int z) {
        Location blockLoc = new Location(world, x, world.getHighestBlockYAt(x, z), z);
        while (blockLoc.getBlock().getType() != Material.BEDROCK && blockLoc.getBlockY() > 0) {
            blockLoc = new Location(world, x, blockLoc.getBlockY() - 1, z);
        }
        return blockLoc;
    }

    public Location getCenterBlockLocation(Location loc) {
        return new Location(loc.getWorld(), (double) loc.getBlockX() + 0.5, (double) loc.getBlockY() + 0.5, (double) loc.getBlockZ() + 0.5);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void dragonSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof EnderDragon enderDragon && this.worlds.contains(event.getLocation().getWorld())) {
            if(event.getLocation().getWorld().getPersistentDataContainer().has(PLUGIN_ENTITY_MARKER,PersistentDataType.BOOLEAN)){
                return;
            }
            DragonFight fight = new DragonFight(this, UUID.randomUUID(), event.getLocation().getWorld(), enderDragon);
            this.registerFight(fight);
            enderDragon.setMaxHealth(this.getConfig().getDouble("dragon-max-health"));
            enderDragon.setHealth(enderDragon.getMaxHealth());
            String dragonName = this.randomName();
            enderDragon.setCustomName(dragonName);
            if (enderDragon.getBossBar() != null) {
                enderDragon.getBossBar().setTitle(dragonName);
                enderDragon.getBossBar().setColor(BarColor.RED);
            }
            String title = this.getConfig().getString("fight-start.title", "");
            title = ChatColor.translateAlternateColorCodes('&', title);
            String subTitle = this.getConfig().getString("fight-start.subtitle", "");
            subTitle = ChatColor.translateAlternateColorCodes('&', subTitle);
            fight.broadcastTitle(title, subTitle);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntity().getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }
        if (event.getEntity() instanceof EnderDragon dragon) {
            this.dragonFightList.forEach(dragonFight -> {
                if (dragonFight.getWorld().equals(event.getEntity().getWorld())) {
                    this.unregisterFight(dragonFight);
                    this.makeSummary(dragonFight, event);
                    AtomicInteger taskId = new AtomicInteger(0);
                    BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> {
                        if (!dragon.isValid()) {
                            Bukkit.getScheduler().runTaskLater(this, () -> {
                                this.findHighestBedrock(dragonFight.getWorld(), 0, 0).add(0.0, 20.0, 0.0).getBlock().setType(Material.DRAGON_EGG);
                                String msg = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("fight-end.egg-placed", ""));
                                dragonFight.getPlayerInWorld().forEach(player -> player.sendMessage(msg));
                            }, 10L);
                            Bukkit.getScheduler().cancelTask(taskId.get());
                        }
                    }, 0L, 1L);
                    taskId.set(task.getTaskId());
                    dragonFight.getWorld().getWorldBorder().reset();
                    dragonFight.getWorld().getEntities().forEach(e -> {
                        if (dragonFight.isMarkedSummonedByPlugin(e) && e.getType() != EntityType.ENDER_DRAGON) {
                            e.remove();
                        }
                    });
                    dragonFight.getWorld().getPersistentDataContainer().set(PLUGIN_ENTITY_MARKER,PersistentDataType.BOOLEAN, true);
                }
            });
        }
    }

    @NotNull
    private String randomName() {
        List<String> name = this.getConfig().getStringList("dragon-name");
        if (name.isEmpty()) {
            return "鳕鱼配置文件没写好之龙，看到请联系鳕鱼";
        }
        if (name.size() == 1) {
            return name.get(0);
        }
        return ChatColor.translateAlternateColorCodes('&', name.get(ThreadLocalRandom.current().nextInt(name.size() - 1)));
    }

    private void makeSummary(@NotNull DragonFight fight, @NotNull EntityDeathEvent event) {
        if (event.getEntity().getLastDamageCause() == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.GREEN).append("末影龙已被击杀！ 本次共有 ").append(ChatColor.RED).append(fight.getPlayerPlayWithIn().size()).append(ChatColor.GREEN).append(" 位小伙伴参与了讨伐！").append("\n");
        builder.append(ChatColor.BLUE).append("==================================").append("\n");
        builder.append(ChatColor.YELLOW).append(" 击杀者：").append(ChatColor.GOLD).append("☠").append(getSource(event.getEntity().getLastDamageCause())).append("\n");
        builder.append(ChatColor.YELLOW).append(" 输出排名 (前10名)：").append("\n");
        builder.append(this.cookBoard(fight.getDamageRankBoard(), 10)).append("\n");
        builder.append(ChatColor.BLUE).append("==================================").append("\n");
        Bukkit.broadcastMessage(builder.toString());
        this.getDataFolder().mkdirs();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        File fightRecordFile = new File(this.getDataFolder(), f.format(new Date()) + ".record.txt");
        try {
            builder.append("完整战斗榜单如下：\n");
            builder.append(this.cookBoard(fight.getDamageRankBoard(), fight.getDamageRankBoard().size())).append("\n");
            builder.append("参与玩家：\n");
            fight.getPlayerPlayWithIn().forEach(str -> builder.append(str).append("\n"));
            fightRecordFile.createNewFile();
            Files.writeString(fightRecordFile.toPath(), ChatColor.stripColor(builder.toString()), StandardCharsets.UTF_8, new OpenOption[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private String cookBoard(@NotNull Map<String, AtomicDouble> board, int size) {
        Map<String, AtomicDouble> descOrderKeyMap = Maps.newLinkedHashMap();
        board.entrySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()))
                .sorted((o1, o2) -> Double.compare(o2.getValue().doubleValue(), o1.getValue().doubleValue()))
                .forEachOrdered(e -> descOrderKeyMap.put(e.getKey(), e.getValue()));
        Iterator<Map.Entry<String, AtomicDouble>> it = descOrderKeyMap.entrySet().iterator();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size && it.hasNext(); ++i) {
            String numberUnit = "th";
            if (i == 0) {
                numberUnit = "st";
            }
            if (i == 1) {
                numberUnit = "nd";
            }
            if (i == 2) {
                numberUnit = "rd";
            }
            builder.append(ChatColor.RED).append(" ").append(i + 1).append(numberUnit).append(" ");
            Map.Entry<String, AtomicDouble> record = it.next();
            builder.append(ChatColor.LIGHT_PURPLE)
                    .append(record.getKey())
                    .append(ChatColor.RED)
                    .append(" 🗡").append((record.getValue()).intValue()).append("\n");
            builder.append("\n");
        }
        return builder.toString().trim().replace("\n\n", " \n");
    }

//    private void randomTick() {
//        this.dragonFightList.forEach(DragonFight::randomTick);
//    }

    private boolean registerFight(@NotNull DragonFight dragonFight) {
        Bukkit.getPluginManager().registerEvents(dragonFight, this);
        if(dragonFight.getDragon() != null && dragonFight.getDragon().isInvulnerable()) {
            dragonFight.getDragon().setInvulnerable(false);
        }
        return this.dragonFightList.add(dragonFight);
    }

    private boolean unregisterFight(@NotNull DragonFight dragonFight) {
        HandlerList.unregisterAll(dragonFight);
        return this.dragonFightList.remove(dragonFight);
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
//        if (command.getName().equalsIgnoreCase("edfteamskillactive")) {
//            if (args.length != 1) {
//                for (DragonFight dragonFight : this.dragonFightList) {
//                    dragonFight.runTeamSkill(sender);
//                }
//                return true;
//            }
//            UUID uuid = UUID.fromString(args[0]);
//            for (DragonFight dragonFight : this.dragonFightList) {
//                if (dragonFight.getUUID().equals(uuid)) {
//                    dragonFight.runTeamSkill(sender);
//                }
//            }
//        }
//        if (command.getName().equalsIgnoreCase("epicdragonfight")) {
//            if (args.length == 0) {
//                this.randomTick();
//            } else if (args.length == 1) {
//                int t = Integer.parseInt(args[0]);
//                this.dragonFightList.forEach(dragonFight -> dragonFight.processRandom(t, true));
//            }
//        }
        return true;
    }

    public Set<DragonFight> getDragonFightList() {
        return this.dragonFightList;
    }
}
