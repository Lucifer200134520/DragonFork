package me.yujiro.dragon.abilities;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import me.yujiro.dragon.DragonElement;

public class DragonsComet extends FireAbility implements AddonAbility {

    private long cooldown, chargetime;
    private double cometradius, chargeradius, hitboxradius, speed, damage, knockback, range;

    private long starttime;
    private Location origin, loc;
    private Vector dir;
    private boolean hasbluefire, charged, cometstarted;

    public DragonsComet(Player player) {
        super(player);

        if (!bPlayer.canBend(this) || CoreAbility.hasAbility(player, this.getClass()) || bPlayer.isOnCooldown(this) || GeneralMethods.isRegionProtectedFromBuild(this, player.getLocation())) {
            return;
        }

        setFields();
        start();
    }

    public void setFields() {
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsComet.Cooldown", 12000);
        this.chargetime = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsComet.ChargeTime", 3000);

        this.cometradius = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsComet.CometRadius", 2.5);
        this.chargeradius = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsComet.ChargeRadius", 3.0);
        this.hitboxradius = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsComet.HitboxRadius", 3.0);

        this.speed = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsComet.Speed", 1.2);
        this.damage = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsComet.Damage", 8.0);
        this.knockback = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsComet.Knockback", 2.0);
        this.range = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsComet.Range", 35.0);

        this.starttime = System.currentTimeMillis();
        this.origin = player.getLocation();
        this.dir = origin.getDirection();
        this.loc = origin.clone();

        this.hasbluefire = bPlayer.canUseSubElement(Element.BLUE_FIRE);
        this.charged = false;
        this.cometstarted = false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return loc;
    }

    @Override
    public String getName() {
        return "DragonsComet";
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public Element getElement() {
        return DragonElement.DRAGON_FIRE;
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || bPlayer.isChiBlocked() || GeneralMethods.isRegionProtectedFromBuild(this, player.getLocation())) {
            this.remove();
            return;
        }

        if (!bPlayer.getBoundAbilityName().equalsIgnoreCase(this.getName())) {
            this.remove();
            return;
        }

        if (System.currentTimeMillis() - starttime > chargetime && !charged) {
            charged = true;
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
            ParticleEffect.EXPLOSION_LARGE.display(player.getLocation().add(0, 1, 0), 2, 0.5, 0.5, 0.5, 0);
        }

        if (!cometstarted && !player.isSneaking()) {
            this.remove();
            return;
        }

        if (!charged) {
            this.origin = player.getLocation();
            this.dir = origin.getDirection();
            this.loc = origin.clone();

            for (double theta = 0; theta < 360; theta += 10) {
                Vector zpositive = new Vector(0, 0, chargeradius);
                Location temploc = loc.clone().add(zpositive.clone().rotateAroundY(Math.toRadians(theta))).add(0, Math.cos(Math.toRadians(theta * 4)), 0).add(0, 1, 0);
                if (!hasbluefire) {
                    ParticleEffect.FLAME.display(temploc, 2, 0.1, 0.3, 0.1, 0.01);
                } else {
                    ParticleEffect.SOUL_FIRE_FLAME.display(temploc, 2, 0.1, 0.3, 0.1, 0.01);
                }
            }
        } else {
            if (!cometstarted) {
                origin = player.getEyeLocation();
                dir = origin.getDirection();
                loc = player.getEyeLocation().clone().add(dir.clone().multiply(cometradius + 1));
                playCometSphere(loc, cometradius);
            } else {
                if (Math.random() < 0.3) {
                    FireAbility.playFirebendingSound(loc);
                }
                loc.add(dir.clone().multiply(speed));
                playCometSphere(loc, cometradius);
                ParticleEffect.SMOKE_LARGE.display(loc, 3, 0.3, 0.3, 0.3, 0.02);

                for (Entity target : GeneralMethods.getEntitiesAroundPoint(loc, hitboxradius)) {
                    if (target != player && target instanceof LivingEntity && !target.isDead()) {
                        triggerImpact();
                        return;
                    }
                }

                if (loc.distance(origin) > range) {
                    this.remove();
                    return;
                }

                if (GeneralMethods.isSolid(loc.getBlock()) || GeneralMethods.isRegionProtectedFromBuild(this, loc)) {
                    triggerImpact();
                    return;
                }
            }
        }
    }


    private void playCometSphere(Location center, double radius) {
        for (double i = 0; i <= Math.PI; i += Math.PI / 10) {
            double r = Math.sin(i) * radius;
            double y = Math.cos(i) * radius;
            for (double a = 0; a < Math.PI * 2; a += Math.PI * 2 / 10) {
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r;
                Location partLoc = center.clone().add(x, y, z);
                if (hasbluefire) {
                    ParticleEffect.SOUL_FIRE_FLAME.display(partLoc, 1, 0.05, 0.05, 0.05, 0);
                } else {
                    ParticleEffect.FLAME.display(partLoc, 1, 0.05, 0.05, 0.05, 0);
                }
            }
        }
    }

    public void triggerImpact() {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
        ParticleEffect.EXPLOSION_HUGE.display(loc, 2, 0, 0, 0, 0);

        for (double theta = 0; theta < 360; theta += 15) {
            for (double phi = -90; phi <= 90; phi += 15) {
                Vector shockwave = new Vector(
                        Math.cos(Math.toRadians(theta)) * Math.cos(Math.toRadians(phi)),
                        Math.sin(Math.toRadians(phi)),
                        Math.sin(Math.toRadians(theta)) * Math.cos(Math.toRadians(phi))
                ).normalize().multiply(Math.random() * hitboxradius * 1.5);

                Location partLoc = loc.clone().add(shockwave);
                if (hasbluefire) {
                    ParticleEffect.SOUL_FIRE_FLAME.display(partLoc, 1, 0.2, 0.2, 0.2, 0.05);
                } else {
                    ParticleEffect.FLAME.display(partLoc, 1, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }

        for (Entity target : GeneralMethods.getEntitiesAroundPoint(loc, hitboxradius * 1.5)) {
            if (target != player && target instanceof LivingEntity && !target.isDead()) {
                DamageHandler.damageEntity(target, damage, this);
                target.setFireTicks(100);

                Vector push = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(knockback).setY(0.6);
                target.setVelocity(target.getVelocity().add(push));
            }
        }

        for (Block block : GeneralMethods.getBlocksAroundPoint(loc, cometradius * 1.2)) {
            if (GeneralMethods.isSolid(block) && !TempBlock.isTempBlock(block) && !GeneralMethods.isRegionProtectedFromBuild(this, block.getLocation())) {
                if (block.getType() != Material.BEDROCK && block.getType() != Material.BARRIER && block.getType() != Material.OBSIDIAN) {
                    TempBlock tb = new TempBlock(block, Material.AIR.createBlockData());
                    tb.setRevertTime(8000);
                }
            }
        }

        this.remove();
    }

    public void onClick() {
        if (charged && !cometstarted) {
            cometstarted = true;
            origin = player.getEyeLocation();
            dir = origin.getDirection().normalize();
        }
    }

    @Override
    public void remove() {
        if (cometstarted) {
            bPlayer.addCooldown(this);
        }
        super.remove();
    }

    @Override
    public String getDescription() {
        return "Выпустите гигантскую огненную комету, которая при столкновении создает разрушительный взрыв и кратер.";
    }

    @Override
    public String getInstructions() {
        return "Удерживайте Shift, чтобы зарядить комету. Нажмите ЛКМ, чтобы запустить её во врагов.";
    }

    @Override
    public void load() {
        org.bukkit.configuration.file.FileConfiguration config = com.projectkorra.projectkorra.configuration.ConfigManager.getConfig();
        config.addDefault("ExtraAbilities.Yujiro.DragonsComet.Cooldown", 12000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsComet.ChargeTime", 3000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsComet.CometRadius", 2.5);
        config.addDefault("ExtraAbilities.Yujiro.DragonsComet.ChargeRadius", 3.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsComet.HitboxRadius", 3.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsComet.Speed", 1.2);
        config.addDefault("ExtraAbilities.Yujiro.DragonsComet.Damage", 8.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsComet.Knockback", 2.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsComet.Range", 35.0);
        com.projectkorra.projectkorra.configuration.ConfigManager.defaultConfig.save();
    }

    @Override
    public void stop() {
    }

    @Override
    public String getAuthor() {
        return "__Yujiro";
    }

    @Override
    public String getVersion() {
        return "2.0 (FORK)";
    }
}