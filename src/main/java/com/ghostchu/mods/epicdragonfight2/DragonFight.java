package com.ghostchu.mods.epicdragonfight2;

import com.ghostchu.mods.epicdragonfight2.skill.control.SkillController;
import com.ghostchu.mods.epicdragonfight2.util.RandomUtil;
import com.ghostchu.mods.epicdragonfight2.util.Util;
import com.google.common.util.concurrent.AtomicDouble;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.ghostchu.mods.epicdragonfight2.EpicDragonFight2.getSource;

public class DragonFight implements Listener {
    private final World world;
    private final EnderDragon dragon;
    private final Random random = new Random(System.currentTimeMillis());
    private final EpicDragonFight2 plugin;
    private final Map<String, AtomicDouble> damageRankBoard = new HashMap<>();
    @NotNull
    private final Set<String> playerPlayWithIn = new HashSet<>();
    private final UUID uuid;
    private final SkillController skillController;

    public DragonFight(EpicDragonFight2 plugin, UUID uuid, World world, EnderDragon dragon) {
        this.plugin = plugin;
        this.world = world;
        this.dragon = dragon;
        this.uuid = uuid;
        this.skillController = new SkillController(plugin.getLogger(), this);
        markEntitySummonedByPlugin(this.dragon);
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(300, TimeUnit.SECONDS, 10);
    }

    public void broadcast(String minimessage) {
        Component component = MiniMessage.miniMessage().deserialize(minimessage);
        Map<String, ComponentLike> preDefinedVars = new HashMap<>();
        preDefinedVars.put("<dragon_name>", LegacyComponentSerializer.legacySection().deserialize(dragon.getName()));
        preDefinedVars.put("<dragon_health>", Component.text(String.format("%.2f", dragon.getHealth())));
        Player randomPlayer = randomPlayer();
        Component playerComponent = Component.text("无");
        if (randomPlayer != null)
            playerComponent = LegacyComponentSerializer.legacySection().deserialize(randomPlayer.getDisplayName());
        preDefinedVars.put("<random_other_player_name>", playerComponent);
        preDefinedVars.put("<players_in_fight>", LegacyComponentSerializer.legacySection().deserialize(Util.list2String(getPlayerInWorld().stream().map(Player::getDisplayName).toList())));
        preDefinedVars.put("<player_amount_in_fight>", Component.text(getPlayerInWorld().size()));
        Component com = component.compact();
        BaseComponent[] serialized = BungeeComponentSerializer.get().serialize(com);
        getPlayerInWorld().forEach(p -> {
            Map<String, ComponentLike> perPlayerVars = new HashMap<>(preDefinedVars);
            perPlayerVars.put("<player_name>", LegacyComponentSerializer.legacySection().deserialize(p.getDisplayName()));
            Component preFilled = Util.fillArgs(BungeeComponentSerializer.get().deserialize(serialized), perPlayerVars);
            p.spigot().sendMessage(BungeeComponentSerializer.get().serialize(preFilled));
        });
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
        this.skillController.tick();
        if(getWorld().getGameTime() % 200 == 0){
            for (Map.Entry<String, AtomicDouble> stringAtomicDoubleEntry : this.damageRankBoard.entrySet()) {
                plugin.getLogger().info("计分板 dump：");
                plugin.getLogger().info(stringAtomicDoubleEntry.getKey()+": "+stringAtomicDoubleEntry.getValue());
            }
        }
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
    @NotNull
    public List<Player> randomPlayers(){
        return RandomUtil.randomPick(getPlayerInWorld(), plugin.getConfig().getInt("global-skill-players-max-pick", 12));
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
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (event.getPlayer().getWorld() == getWorld()) {
            event.setCancelled(true);
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
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL ||
                event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }
        if (!isMarkedSummonedByPlugin(event.getEntity())) {
            return;
        }
        event.setDamage(0.0);
    }

    @EventHandler(ignoreCancelled = true)
    public void creeperSecurity(EntityDamageByEntityEvent event) {
        if (!isMarkedSummonedByPlugin(event.getDamager())) {
            return;
        }
        if (!isMarkedSummonedByPlugin(event.getEntity())) {
            return;
        }
        if(!(event.getDamager() instanceof TNTPrimed) || !(event.getDamager() instanceof Creeper)) return;
        event.setDamage(0.0);
    }

    @EventHandler(ignoreCancelled = true)
    public void creeperSecurity(EntityDamageByBlockEvent event) {
        if (event.getDamager() == null) return;
        if (!isMarkedSummonedByPlugin(event.getEntity())) {
            return;
        }
        if (Tag.BEDS.isTagged(event.getDamager().getType())) {
            return;
        }
        event.setDamage(0.0);
    }

    @EventHandler(ignoreCancelled = true)
    public void dragonSecurity(EntityDamageByEntityEvent event) {
        if (isMarkedSummonedByPlugin(event.getEntity())) {
            if (isMarkedSummonedByPlugin(event.getDamager())) {
                event.setCancelled(true);
            }
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
                Component title = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("death-respawn-title.title"));
                Component subTitle = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("death-respawn-title.subtitle"));
                Component chatMsg = MiniMessage.miniMessage().deserialize(
                        plugin.getConfig().getString("death-respawn-message")
                );
                Component component = Util.fillArgs(chatMsg, Map.of("<player_name>", LegacyComponentSerializer.legacySection().deserialize(event.getEntity().getDisplayName())));
                event.getEntity().sendTitle(LegacyComponentSerializer.legacySection().serialize(title), LegacyComponentSerializer.legacySection().serialize(subTitle), 10, 100, 20);
                event.getEntity().spigot().sendMessage(BungeeComponentSerializer.get().serialize(component));
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

    public Map<String, AtomicDouble> getDamageRankBoard() {
        return this.damageRankBoard;
    }

    @NotNull
    public Set<String> getPlayerPlayWithIn() {
        return this.playerPlayWithIn;
    }
}
