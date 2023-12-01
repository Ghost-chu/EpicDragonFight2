package com.ghostchu.mods.epicdragonfight2;

import com.ghostchu.mods.epicdragonfight2.skill.EpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.SkillEndReason;
import com.ghostchu.mods.epicdragonfight2.skill.impl.*;
import com.ghostchu.mods.epicdragonfight2.teamskill.EpicTeamSkill;
import com.ghostchu.mods.epicdragonfight2.teamskill.impl.Purge;
import com.ghostchu.mods.epicdragonfight2.util.RandomUtil;
import com.ghostchu.mods.epicdragonfight2.util.Util;
import com.google.common.util.concurrent.AtomicDouble;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.ghostchu.mods.epicdragonfight2.EpicDragonFight2.getSource;

public class DragonFight implements Listener {

    private final World world;
    private final EnderDragon dragon;
    private final Random random = new Random(System.currentTimeMillis());
    private final EpicDragonFight2 plugin;
    private final Map<String, AtomicDouble> damageRankBoard = new HashMap<>();
    @NotNull
    private final Set<String> playerPlayWithIn = new HashSet<>();
    @NotNull
    private Stage currentStage = Stage.STAGE_1;
    @Nullable
    private EpicDragonSkill currentSkills;
    private int lastRandom = -1;
    private UUID uuid;
    private EpicTeamSkill teamSkill;

    public DragonFight(EpicDragonFight2 plugin ,UUID uuid, World world, EnderDragon dragon) {
        this.plugin = plugin;
        this.world = world;
        this.dragon = dragon;
        this.uuid = uuid;
        markEntitySummonedByPlugin(this.dragon);
    }

    public void broadcast(Component component){
        Map<String, ComponentLike> preDefinedVars = new HashMap<>();
        preDefinedVars.put("<dragon_name>", LegacyComponentSerializer.legacySection().deserialize(dragon.getName()));
        preDefinedVars.put("<dragon_health>", Component.text(String.format("%.2f", dragon.getHealth())));
        Player randomPlayer = randomPlayer();
        Component playerComponent = Component.text("无");
        if(randomPlayer != null) playerComponent = LegacyComponentSerializer.legacySection().deserialize(randomPlayer.getDisplayName());
        preDefinedVars.put("<random_other_player_name>", playerComponent);
        preDefinedVars.put("<players_in_fight>", LegacyComponentSerializer.legacySection().deserialize(Util.list2String(getPlayerInWorld().stream().map(Player::getDisplayName).toList())));
        preDefinedVars.put("<player_amount_in_fight>", Component.text(getPlayerInWorld().size()));

        Component preFilled = Util.fillArgs(component.compact());
    }

    public UUID getUUID() {
        return uuid;
    }

    public void markEntitySummonedByPlugin(Entity entity) {
        entity.getPersistentDataContainer().set(plugin.PLUGIN_ENTITY_MARKER, PersistentDataType.BOOLEAN, true);
    }

    public EpicDragonFight2 getPlugin() {
        return plugin;
    }

    public boolean isMarkedSummonedByPlugin(Entity entity) {
        return entity.getPersistentDataContainer().getOrDefault(plugin.PLUGIN_ENTITY_MARKER, PersistentDataType.BOOLEAN, false);
    }

    public void tick() {
        if (this.currentSkills != null && this.currentSkills.cycle()) {
            this.currentSkills = null;
        }
        if(this.teamSkill == null){
            installTeamSkill(randomTeamSkill(),false);
        }else{
            this.teamSkill.tick();
        }
        this.tickMagic();
    }

    private void installTeamSkill(EpicTeamSkill epicTeamSkill, boolean b) {
        if(this.teamSkill != null & !b){
            plugin.getLogger().info("尝试安装团队终结技 "+epicTeamSkill.getClass().getName()+" 但目前已有一个终结技已安装");
            return;
        }
        this.teamSkill = epicTeamSkill;
        plugin.getLogger().info("已安装团队终结技 "+epicTeamSkill.getClass().getName());
    }


    private void tickMagic() {
        this.getPlayerInWorld().forEach(player -> {
            List<Player> aroundPlayers = player.getNearbyEntities(5.0, 5.0, 5.0).stream()
                    .filter(entity -> entity instanceof Player)
                    .map(entity -> (Player) entity).toList();
            if (aroundPlayers.size() > 3) {
                player.setCooldown(Material.SHIELD, 0);
                player.setCooldown(Material.ENDER_PEARL, 0);
                player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 30, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 30, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 0));
                if (!player.hasMetadata("stay-together")) {
                    player.setMetadata("stay-together", new FixedMetadataValue(plugin, true));
                    // player enter the stay together
                    if (random.nextBoolean()) {
                        // group says
                        Player groupSayer = RandomUtil.randomPick(aroundPlayers);
                        Bukkit.dispatchCommand(groupSayer, "global " +
                                RandomUtil.randomPick(plugin.getConfig().getStringList("stay-together.group-says")));
                    } else {
                        // alone says
                        Bukkit.dispatchCommand(player, "global " +
                                RandomUtil.randomPick(plugin.getConfig().getStringList("stay-together.alone-says")));
                    }
                }
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 30, 0));
                if (player.hasMetadata("stay-together")) {
                    player.removeMetadata("stay-together", plugin);
                    // player exit stay together
                }
            }
        });
    }

    public void runTeamSkill(CommandSender sender){
        if(this.teamSkill.execute(sender)){
            this.teamSkill = null;
            installTeamSkill(randomTeamSkill(),false);
        }
    }

    private EpicTeamSkill randomTeamSkill() {
        int r = this.random.nextInt(1);
        return switch (r){
            case 0 -> new Purge(this);
            default -> null;
        };
    }

    public void randomTick() {
        int r = this.random.nextInt(10);
        EpicDragonFight2.getInstance().getLogger().info("Random: " + r);
        this.processRandom(r, false);
    }

    private void installSkill(@NotNull EpicDragonSkill skill, boolean force) {
        if (this.currentSkills != null && !force) {
            plugin.getLogger().info("尝试向槽位安装技能: " + skill.getClass().getName() + "，但此时另一个技能正在进行中。");
            return;
        }
        Bukkit.getScheduler().runTaskLater(EpicDragonFight2.getInstance(), () -> {
            plugin.getLogger().info("尝试向槽位安装技能: " + skill.getClass().getName());
            if (Arrays.stream(skill.getAdaptStages()).anyMatch(stage -> stage == this.currentStage)) {
                plugin.getLogger().info("新技能已安装: " + skill.getClass().getName());
                if (this.currentSkills != null) {
                    this.currentSkills.unregister();
                    this.currentSkills.end(SkillEndReason.REACH_TIME_LIMIT);
                }
                this.currentSkills = skill;
                broadcast(skill.preAnnounce());
            } else {
                plugin.getLogger().info("技能安装跳过，当前阶段为 " + this.currentStage.name() + " 但技能要求阶段为: " + Arrays.toString(skill.getAdaptStages()));
            }
        }, 10L);
    }

    public void applyDifficultRate(LivingEntity entity) {
//        ConfigurationSection attributeSection = plugin.getConfig().getConfigurationSection("attributes");
//        for (String attribute : attributeSection.getKeys(false)) {
//            try {
//                Attribute attr = Attribute.valueOf(attribute);
//                if(attr == null) continue;
//                AttributeInstance instance = entity.getAttribute(attr);
//                if (instance == null) continue;
//                ConfigurationSection modifiersSection = attributeSection.getConfigurationSection(attribute);
//                List<AttributeModifier> modifiers = new ArrayList<>();
//                for (String key : modifiersSection.getKeys(false)) {
//                    ConfigurationSection modifierSection = modifiersSection.getConfigurationSection(attribute);
//                    double amount;
//                    if (modifierSection.isString("amount")
//                            && modifierSection.getString("amount").equalsIgnoreCase("player_count")) {
//                        amount = getWorld().getPlayers().size();
//                    } else {
//                        amount = modifiersSection.getDouble("amount");
//                    }
//                    AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), key, amount,
//                            AttributeModifier.Operation.valueOf(modifierSection.getString("operation")));
//                    modifiers.add(modifier);
//                }
//                modifiers.forEach(instance::addModifier);
//            } catch (IllegalArgumentException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public void processRandom(int t, boolean force) {
        if (this.currentSkills != null && !force) {
            return;
        }
        if (t == this.lastRandom) {
            this.processRandom(this.random.nextInt(11), false);
            return;
        }
        switch (t) {
            case 0: {
                this.installSkill(new WardenPowered(this), force);
                break;
            }
            case 1: {
                this.installSkill(new BadWind(this), force);
                break;
            }
            case 2: {
                this.installSkill(new WitherShield(this), force);
                break;
            }
            case 3: {
                this.installSkill(new DragonEffectCloud(this), force);
                break;
            }
            case 4: {
                this.installSkill(new FallingTrident(this), force);
                break;
            }
            case 5: {
                this.installSkill(new NukeExplosion(this), force);
                break;
            }
            case 6: {
                this.installSkill(new Ravage(this), force);
                break;
            }
            case 7: {
                this.installSkill(new RocketRain(this), force);
                break;
            }
            case 8: {
                this.installSkill(new Throw(this), force);
                break;
            }
            case 9: {
                //this.installSkill(new AnvilPowered(this));
                break;
            }
            case 10: {
                // this.installSkill(new TerrainDissolve(this));
            }
            default: {
                plugin.getLogger().info("本轮随机数未命中任何特定技能.");
            }
        }
        this.lastRandom = t;
    }

    @NotNull
    public List<Player> getPlayerInWorld() {
        ArrayList<Player> players = new ArrayList<>();
        for (Player player : this.world.getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) continue;
            players.add(player);
        }
        return players;
    }

    public void installStage(@NotNull Stage stage) {
        this.currentStage = stage;
    }

    @Nullable
    public Player randomPlayer() {
        List<Player> players = this.getPlayerInWorld();
        if (players.isEmpty()) {
            return null;
        }
        if (players.size() == 1) {
            return players.get(0);
        }
        return players.get(this.random.nextInt(Math.max(1, players.size() - 1)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onTargeting(EntityTargetLivingEntityEvent event) {
        if (isMarkedSummonedByPlugin(event.getEntity())) {
            if (event.getTarget() != null && isMarkedSummonedByPlugin(event.getTarget())) {
                Player newTarget = randomPlayer();
                event.setTarget(newTarget);
            }
        }
    }

    public void broadcast(@NotNull String string) {
        this.getPlayerInWorld().forEach(player -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', string)));
    }

    public void broadcastTitle(@NotNull String title, @NotNull String subTitle) {
        this.getPlayerInWorld().forEach(player -> player.sendTitle(ChatColor.translateAlternateColorCodes('&', title), ChatColor.translateAlternateColorCodes('&', subTitle)));
    }

    public void broadcastActionBar(@NotNull String string) {
        this.getPlayerInWorld().forEach(player -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', string))));
    }

    @EventHandler(ignoreCancelled = true)
    public void waterPlace(BlockPlaceEvent event) {
        if (!Objects.equals(event.getBlock().getLocation().getWorld(), this.world)) {
            return;
        }
        if (event.getBlockPlaced().getType() == Material.WATER || Tag.ICE.isTagged(event.getBlockPlaced().getType())) {
            event.setCancelled(true);
            event.setBuild(false);
            event.getPlayer().sendMessage(ChatColor.GRAY + "神秘的力量导致你手中的水元素无法放置");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void waterBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!Objects.equals(event.getBlock().getLocation().getWorld(), this.world)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.GRAY + "神秘的力量导致你手中的水元素无法倾倒");
    }

    @EventHandler(ignoreCancelled = true)
    public void waterBucketEmpty(BlockDispenseEvent event) {
        if (!Objects.equals(event.getBlock().getLocation().getWorld(), this.world)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void enderDragonFinalKillDamage(EntityDamageEvent event) {
        if (!event.getEntity().equals(this.dragon)) {
            return;
        }
        if (event.getFinalDamage() < this.dragon.getHealth()) {
            return;
        }
        if (!(event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent)) {
            event.setCancelled(true);
            return;
        }
        if (!(entityDamageByEntityEvent.getDamager() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void skillExplosionProtection(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            return;
        }
        if (!event.getEntity().equals(this.dragon)) {
            return;
        }
        if (this.dragon.getHealth() < plugin.getConfig().getDouble("skills.explosion-protect.below")) {
            event.setDamage(0.0);
            this.world.spawnParticle(Particle.CRIT_MAGIC, event.getEntity().getLocation(), 20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void skillProjectileProtection(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
            return;
        }
        if (!event.getEntity().equals(this.dragon)) {
            return;
        }
        if (this.dragon.getHealth() < plugin.getConfig().getDouble("skills.projectile-protect.below")) {
            event.setDamage(0.0);
            this.world.spawnParticle(Particle.CRIT_MAGIC, event.getEntity().getLocation(), 20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void skillEnchantmentProtection(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.THORNS) {
            return;
        }
        if (!event.getEntity().equals(this.dragon)) {
            return;
        }
        event.setDamage(0.0);
    }

    @EventHandler(ignoreCancelled = true)
    public void creeperSecurity(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            return;
        }
        if (!isMarkedSummonedByPlugin(event.getEntity())) {
            return;
        }
        event.setDamage(0.0);
    }

    @EventHandler(ignoreCancelled = true)
    public void dragonSecurity(EntityDamageByEntityEvent event) {
        if (!event.getEntity().equals(this.dragon)) {
            return;
        }
        if (isMarkedSummonedByPlugin(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().getWorld().equals(this.world)) {
            return;
        }
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            event.getEntity().spigot().respawn();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                event.getEntity().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 2));
                event.getEntity().addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 120, 2));
                String title = "&x&7&c&d&0&f&fRCF&7中枢弹射系统&8►&c执行成功";
                String subTitle = "&8●认知障碍:&7未检测&8●时序紊乱:&7未检测&8●记忆保全:&7成功";
                title = ChatColor.translateAlternateColorCodes('&', title);
                subTitle = ChatColor.translateAlternateColorCodes('&', subTitle);
                event.getEntity().sendTitle(title, subTitle, 10, 100, 20);
                event.getEntity().sendMessage(ChatColor.RED + "● MSG FR RCF");
                event.getEntity().sendMessage(ChatColor.GRAY + "你好，" + event.getEntity().getName() + "。很高兴你看起来没有生命危险了");
                event.getEntity().sendMessage(ChatColor.GRAY + "我们已经提取了你的 RCF 记录仪中的关键情报，感谢你做出的突出贡献");
                event.getEntity().sendMessage(ChatColor.GRAY + "你的装备在这边，如果准备好的话，就立刻返回战场吧。留给我们的时间不多了");
            }, 2L);
        }, 2L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageEvent event) {
        if (!event.getEntity().getWorld().equals(this.world)) {
            return;
        }
        if (!(event.getEntity() instanceof EnderDragon)) {
            return;
        }
        String source = getSource(event);
        AtomicDouble history = this.damageRankBoard.get(source);
        if (history == null) {
            history = new AtomicDouble(0.0);
            this.damageRankBoard.put(source, history);
            if (Bukkit.getPlayer(source) != null) {
                this.playerPlayWithIn.add(source);
            }
        }
        history.addAndGet(event.getFinalDamage());
    }

    public boolean isValid() {
        return plugin.getDragonFightList().contains(this);
    }

    public World getWorld() {
        return this.world;
    }

    public EnderDragon getDragon() {
        return this.dragon;
    }

    @NotNull
    public Stage getCurrentStage() {
        return this.currentStage;
    }

    @Nullable
    public EpicDragonSkill getCurrentSkills() {
        return this.currentSkills;
    }

    public Map<String, AtomicDouble> getDamageRankBoard() {
        return this.damageRankBoard;
    }

    @NotNull
    public Set<String> getPlayerPlayWithIn() {
        return this.playerPlayWithIn;
    }
}
