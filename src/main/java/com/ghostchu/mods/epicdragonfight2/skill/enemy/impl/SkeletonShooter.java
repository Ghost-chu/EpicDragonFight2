package com.ghostchu.mods.epicdragonfight2.skill.enemy.impl;

import com.ghostchu.mods.epicdragonfight2.DragonFight;
import com.ghostchu.mods.epicdragonfight2.Stage;
import com.ghostchu.mods.epicdragonfight2.skill.EpicSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.AbstractEpicDragonSkill;
import com.ghostchu.mods.epicdragonfight2.skill.enemy.SkillEndReason;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@EpicSkill
public class SkeletonShooter extends AbstractEpicDragonSkill {

    private final double height;
    private final double skeletonShooterMaxHealth;
    private final int enchantmentLevel;

    public SkeletonShooter(@NotNull DragonFight fight) {
        super(fight, "skeleton-shooter");
        this.height = getSkillConfig().getDouble("skeleton-shooter-spawn-height");
        this.skeletonShooterMaxHealth = getSkillConfig().getDouble("skeleton-shooter-max-health");
        this.enchantmentLevel = getSkillConfig().getInt("bow-damage-enchantment-level");
    }

    @NotNull
    public static Stage[] getAdaptStages() {
        return new Stage[]{Stage.STAGE_1};
    }

    @Override
    public int start() {
        return skillStartWaitingTicks() + 60;
    }

    @Override
    public void end(@NotNull SkillEndReason reason) {

    }

    @Override
    public boolean tick() {
        if (isWaitingStart())
            return false;
        List<Skeleton> skeletonList = new ArrayList<>();
        for (Player player : getPlayerInWorld()) {
            Location stdPos = player.getLocation().add(0, height, 0);
            Location pos1 = stdPos.add(getRandom().nextInt(-10, 10), 0, getRandom().nextInt(-10, 10));
            Location pos2 = stdPos.add(getRandom().nextInt(-10, 10), 0, getRandom().nextInt(-10, 10));
            Skeleton skeleton1 = (Skeleton) player.getWorld().spawnEntity(pos1, EntityType.SKELETON, true);
            Skeleton skeleton2 = (Skeleton) player.getWorld().spawnEntity(pos2, EntityType.SKELETON, true);
            skeletonList.add(skeleton1);
            skeletonList.add(skeleton2);
            skeleton1.setTarget(player);
            skeleton2.setTarget(player);
        }
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(Enchantment.ARROW_DAMAGE, enchantmentLevel, true);
        bow.setItemMeta(bowMeta);
        for (Skeleton skeleton : skeletonList) {
            markEntitySummonedByPlugin(skeleton);
            skeleton.setGravity(false);
            skeleton.setMaxHealth(skeletonShooterMaxHealth);
            skeleton.setHealth(skeleton.getMaxHealth());
            EntityEquipment equipment = skeleton.getEquipment();
            equipment.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            equipment.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            equipment.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            equipment.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            equipment.setItemInMainHand(bow);
            equipment.setItemInMainHandDropChance(0.0F);
            equipment.setItemInOffHandDropChance(0.0F);
            equipment.setBootsDropChance(0.0F);
            equipment.setChestplateDropChance(0.0F);
            equipment.setLeggingsDropChance(0.0F);
            equipment.setHelmetDropChance(0.0F);
        }
        return true;
    }

    @Override
    public int skillStartWaitingTicks() {
        return 20;
    }

}
