package me.yujiro.dragon.abilities.combos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import me.yujiro.dragon.DragonElement;
import net.md_5.bungee.api.ChatColor;

public class DragonsDomain extends FireAbility implements ComboAbility, AddonAbility {

    private long cooldown, duration, startTime, lastGeyserTime;
    private double radius, damage;
    private boolean hasbluefire;
    private Location center;

    private Map<Location, Long> activeGeysers;
    private Set<String> activeCooldownsTracker;

    public DragonsDomain(Player player) {
        super(player);
        if (player == null) return;

        if (!bPlayer.canBendIgnoreBinds(this) || hasAbility(player, this.getClass()) || bPlayer.isOnCooldown(this)) {
            return;
        }

        double checkRadius = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsDomain.Radius", 13.0);
        Location[] checks = {
                player.getLocation(),
                player.getLocation().clone().add(checkRadius, 0, 0),
                player.getLocation().clone().add(-checkRadius, 0, 0),
                player.getLocation().clone().add(0, 0, checkRadius),
                player.getLocation().clone().add(0, 0, -checkRadius)
        };

        for (Location l : checks) {
            if (GeneralMethods.isRegionProtectedFromBuild(this, l)) {
                player.sendMessage(ChatColor.RED + "Здесь недостаточно места или регион защищен приватом!");
                return;
            }
        }

        setFields();
        start();
    }

    public void setFields() {
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsDomain.Cooldown", 120000);
        this.duration = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsDomain.Duration", 15000);
        this.radius = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsDomain.Radius", 13.0);
        this.damage = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsDomain.DamagePerSecond", 2.0);

        this.hasbluefire = bPlayer.canUseSubElement(Element.BLUE_FIRE);
        this.center = player.getLocation().clone();
        this.startTime = System.currentTimeMillis();
        this.lastGeyserTime = startTime;

        this.activeGeysers = new HashMap<>();
        this.activeCooldownsTracker = new HashSet<>();

        createFloor();

        player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.5f);
        player.getWorld().playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 2.0f);
    }

    @Override
    public long getCooldown() { return cooldown; }

    @Override
    public Location getLocation() { return center; }

    @Override
    public String getName() { return "DragonsDomain"; }

    @Override
    public boolean isHarmlessAbility() { return false; }

    @Override
    public boolean isSneakAbility() { return false; }

    @Override
    public Element getElement() { return DragonElement.DRAGON_FIRE; }

    private void createFloor() {
        Material primary = hasbluefire ? Material.SOUL_SOIL : Material.MAGMA_BLOCK;
        Material secondary = hasbluefire ? Material.SOUL_FIRE : Material.LAVA;

        for (double x = -radius; x <= radius; x++) {
            for (double z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    Location loc = center.clone().add(x, 0, z);
                    Block top = GeneralMethods.getTopBlock(loc, 3);

                    if (top != null && GeneralMethods.isSolid(top) && !GeneralMethods.isRegionProtectedFromBuild(this, top.getLocation())) {
                        if (!TempBlock.isTempBlock(top) && top.getType() != Material.BEDROCK) {
                            Material type = Math.random() > 0.15 ? primary : secondary;
                            TempBlock tb = new TempBlock(top, type.createBlockData());
                            tb.setRevertTime(duration);
                        }
                    }
                }
            }
        }
    }


    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || bPlayer.isChiBlocked()) {
            remove();
            return;
        }


        if (player.hasMetadata("DomainClash_Loser")) {
            remove();
            return;
        }


        if (player.hasMetadata("DomainClash_Active")) {
            this.startTime += 50;
            for (Map.Entry<Location, Long> entry : activeGeysers.entrySet()) {
                entry.setValue(entry.getValue() + 50);
            }
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > duration) {
            remove();
            return;
        }

        if (player.getLocation().distance(center) <= radius) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false));
            if (elapsed % 2000 < 50) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false));
            }

            for (int i = 1; i <= 9; i++) {
                String abilName = bPlayer.getAbilities().get(i);
                if (abilName != null && !abilName.equals("DragonsDomain")) {
                    if (bPlayer.isOnCooldown(abilName)) {
                        if (!activeCooldownsTracker.contains(abilName)) {
                            bPlayer.removeCooldown(abilName);
                            bPlayer.addCooldown(abilName, 1000);
                            activeCooldownsTracker.add(abilName);
                        }
                    } else {
                        activeCooldownsTracker.remove(abilName);
                    }
                }
            }
        }

        if (elapsed - lastGeyserTime > 2500) {
            lastGeyserTime = elapsed;
            for (Entity target : GeneralMethods.getEntitiesAroundPoint(center, radius)) {
                if (target instanceof LivingEntity && !target.equals(player)) {
                    Location gLoc = target.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
                    activeGeysers.put(gLoc, System.currentTimeMillis() + 1000);

                    gLoc.getWorld().playSound(gLoc, Sound.BLOCK_LAVA_AMBIENT, 1.5f, 1.5f);
                    if (hasbluefire) {
                        ParticleEffect.SOUL_FIRE_FLAME.display(gLoc, 10, 0.5, 0, 0.5, 0.1);
                    } else {
                        ParticleEffect.LAVA.display(gLoc, 10, 0.5, 0, 0.5, 0.1);
                    }
                }
            }
        }

        Iterator<Map.Entry<Location, Long>> it = activeGeysers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, Long> entry = it.next();
            Location gLoc = entry.getKey();

            if (System.currentTimeMillis() > entry.getValue()) {
                it.remove();
                continue;
            }

            for (double y = 0; y < 4.5; y += 0.5) {
                Location pLoc = gLoc.clone().add(0, y, 0);
                if (hasbluefire) {
                    ParticleEffect.SOUL_FIRE_FLAME.display(pLoc, 2, 0.4, 0.4, 0.4, 0.05);
                    ParticleEffect.SMOKE_NORMAL.display(pLoc, 1, 0.2, 0.2, 0.2, 0);
                } else {
                    ParticleEffect.FLAME.display(pLoc, 2, 0.4, 0.4, 0.4, 0.05);
                    ParticleEffect.LAVA.display(pLoc, 1, 0.2, 0.2, 0.2, 0);
                }
            }

            if (elapsed % 200 < 50) gLoc.getWorld().playSound(gLoc, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 1.2f);

            for (Entity t : GeneralMethods.getEntitiesAroundPoint(gLoc, 1.5)) {
                if (t instanceof LivingEntity && !t.equals(player)) {
                    DamageHandler.damageEntity(t, 2.0, this);
                    t.setFireTicks(80);
                    t.setVelocity(t.getVelocity().add(new Vector(0, 0.25, 0)));
                }
            }
        }

        for (double theta = 0; theta < 360; theta += 5) {
            double x = Math.cos(Math.toRadians(theta)) * radius;
            double z = Math.sin(Math.toRadians(theta)) * radius;

            for (double y = 0; y < 7; y += 1.5) {
                Location wallLoc = center.clone().add(x, y, z);
                if (Math.random() < 0.4) {
                    if (hasbluefire) {
                        ParticleEffect.SOUL_FIRE_FLAME.display(wallLoc, 1, 0, 0.5, 0, 0);
                    } else {
                        ParticleEffect.FLAME.display(wallLoc, 1, 0, 0.5, 0, 0);
                        ParticleEffect.LAVA.display(wallLoc, 1, 0, 0, 0, 0);
                    }
                }
            }
        }

        for (int i = 0; i < 15; i++) {
            double rx = (Math.random() - 0.5) * radius * 2;
            double rz = (Math.random() - 0.5) * radius * 2;
            if (rx * rx + rz * rz <= radius * radius) {
                Location ashLoc = center.clone().add(rx, Math.random() * 6 + 1, rz);
                ParticleEffect.ASH.display(ashLoc, 1, 0.5, 0.5, 0.5, 0);

                if (Math.random() < 0.1) {
                    if (hasbluefire) {
                        ParticleEffect.SOUL_FIRE_FLAME.display(ashLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    } else {
                        ParticleEffect.FLAME.display(ashLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
        }

        for (Entity target : GeneralMethods.getEntitiesAroundPoint(center, radius + 2)) {
            if (target instanceof LivingEntity && !target.equals(player)) {
                LivingEntity livingTarget = (LivingEntity) target;

                double distance = target.getLocation().distance(center);

                if (distance >= radius - 1.5) {
                    Vector push = center.toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.2).setY(0.3);
                    target.setVelocity(push);
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
                    target.setFireTicks(100);
                }

                if (distance <= radius) {
                    livingTarget.setFireTicks(60);
                    livingTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1, true, false));

                    if (elapsed % 1000 < 50) {
                        DamageHandler.damageEntity(livingTarget, damage, this);
                    }
                }
            }
        }
    }

    @Override
    public Object createNewComboInstance(Player player) {
        return new DragonsDomain(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("DragonsComet", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("DragonsComet", ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("DragonsComet", ClickType.LEFT_CLICK));
        return combo;
    }

    @Override
    public void remove() {
        bPlayer.addCooldown(this);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        ParticleEffect.EXPLOSION_HUGE.display(center.clone().add(0, 1, 0), 5, radius/2, 1, radius/2, 0);
        super.remove();
    }

    @Override
    public String getDescription() {
        return "Территориальная магия Воинов Солнца. Превратите поле боя в замкнутую огненную арену.";
    }

    @Override
    public String getInstructions() {
        return "DragonsComet (Нажать Shift) -> (Отпустить Shift) -> (Левый клик)";
    }

    @Override
    public void load() {
        org.bukkit.configuration.file.FileConfiguration config = com.projectkorra.projectkorra.configuration.ConfigManager.getConfig();
        config.addDefault("ExtraAbilities.Yujiro.DragonsDomain.Cooldown", 120000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsDomain.Duration", 15000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsDomain.Radius", 13.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsDomain.DamagePerSecond", 2.0);
        com.projectkorra.projectkorra.configuration.ConfigManager.defaultConfig.save();
    }
    @Override
    public void stop() {}
    @Override
    public String getAuthor() { return "Lucifer200134520"; }
    @Override
    public String getVersion() { return "1.0"; }
}