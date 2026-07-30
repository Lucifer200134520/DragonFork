package me.yujiro.dragon.abilities.combos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

import me.yujiro.dragon.DragonElement;

public class DragonsSlam extends FireAbility implements ComboAbility, AddonAbility {

    private long cooldown;
    private double maxradius, speed, damage, jumpheight, hitbox, angleincrement;

    private boolean hasbluefire, hastouchedground;
    private Location origin;
    private double currentradius;
    private double angle;
    private Set<Entity> affectedEntities;

    public DragonsSlam(Player player) {
        super(player);

        if (CoreAbility.hasAbility(player, this.getClass()) || bPlayer.isOnCooldown(this) || GeneralMethods.isRegionProtectedFromBuild(this, player.getLocation())) {
            return;
        }

        setFields();
        start();
    }

    public void setFields() {
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsSlam.Cooldown", 8000);
        this.maxradius = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsSlam.MaxRadius", 10.0);
        this.speed = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsSlam.Speed", 1.0);
        this.angleincrement = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsSlam.AngleIncrement", 15.0);
        this.damage = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsSlam.Damage", 6.0);
        this.jumpheight = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsSlam.JumpHeight", 2.0);
        this.hitbox = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsSlam.Hitbox", 2.0);

        this.hasbluefire = bPlayer.canUseSubElement(Element.BLUE_FIRE);
        this.hastouchedground = false;
        this.currentradius = 0;
        this.angle = 0;
        this.affectedEntities = new HashSet<>();

        player.setVelocity(new Vector(0, jumpheight, 0));
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return origin != null ? origin : player.getLocation();
    }

    @Override
    public String getName() {
        return "DragonsSlam";
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
        if (player.isDead() || !player.isOnline() || bPlayer.isChiBlocked() || GeneralMethods.isRegionProtectedFromBuild(this, player.getLocation())){
            this.remove();
            return;
        }

        if (!hastouchedground) {
            player.setFallDistance(0);
            Location ploc = player.getLocation().clone().subtract(0, 0.5, 0);

            if (hasbluefire) {
                ParticleEffect.SOUL_FIRE_FLAME.display(GeneralMethods.getRightSide(ploc, 0.3), 2, 0.1, 0.1, 0.1, 0.02);
                ParticleEffect.SOUL_FIRE_FLAME.display(GeneralMethods.getLeftSide(ploc, 0.3), 2, 0.1, 0.1, 0.1, 0.02);
            } else {
                ParticleEffect.FLAME.display(GeneralMethods.getRightSide(ploc, 0.3), 2, 0.1, 0.1, 0.1, 0.02);
                ParticleEffect.FLAME.display(GeneralMethods.getLeftSide(ploc, 0.3), 2, 0.1, 0.1, 0.1, 0.02);
            }

            if (player.isOnGround() && player.getVelocity().getY() <= 0) {
                bPlayer.addCooldown(this);
                hastouchedground = true;
                origin = player.getLocation().add(0, 0.5, 0);

                player.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                ParticleEffect.EXPLOSION_LARGE.display(origin, 3, 1, 0.5, 1, 0);
            }
        } else {
            currentradius += speed;
            angle += angleincrement;

            if (currentradius > maxradius) {
                this.remove();
                return;
            }

            Vector zpositive = new Vector(0, 0, currentradius);
            for (double theta = 0; theta < 360; theta += 10) {
                Location temploc = origin.clone().add(zpositive.clone().rotateAroundY(Math.toRadians(theta)));
                temploc.add(0, Math.sin(Math.toRadians(angle)), 0);

                if (hasbluefire) {
                    ParticleEffect.SOUL_FIRE_FLAME.display(temploc, 1, 0.2, 0.2, 0.2, 0.01);
                } else {
                    ParticleEffect.FLAME.display(temploc, 1, 0.2, 0.2, 0.2, 0.01);
                }
            }

            for (Entity target : GeneralMethods.getEntitiesAroundPoint(origin, currentradius + hitbox)) {
                if (target instanceof LivingEntity && !target.equals(player) && !affectedEntities.contains(target)) {
                    if (target.getLocation().distance(origin) >= Math.max(0, currentradius - hitbox)) {
                        DamageHandler.damageEntity(target, damage, this);
                        target.setFireTicks(60);
                        Vector push = target.getLocation().toVector().subtract(origin.toVector()).normalize().multiply(1.2).setY(0.5);
                        target.setVelocity(target.getVelocity().add(push));
                        affectedEntities.add(target);
                    }
                }
            }
        }
    }

    @Override
    public Object createNewComboInstance(Player player) {
        return new DragonsSlam(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("DragonsComet", ClickType.RIGHT_CLICK_BLOCK));
        combo.add(new AbilityInformation("DragonsScales", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("DragonsScales", ClickType.SHIFT_UP));
        return combo;
    }

    @Override
    public String getDescription() {
        return "Взлетите в воздух и с силой обрушьтесь на землю, создавая расширяющуюся огненную волну.";
    }

    @Override
    public String getInstructions() {
        return "DragonsComet (ПКМ по блоку) -> DragonsScales (Зажать Shift) -> DragonsScales (Отпустить Shift)";
    }

    @Override
    public void load() {
        org.bukkit.configuration.file.FileConfiguration config = com.projectkorra.projectkorra.configuration.ConfigManager.getConfig();

        config.addDefault("ExtraAbilities.Yujiro.DragonsSlam.Cooldown", 8000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsSlam.MaxRadius", 10.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsSlam.Speed", 1.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsSlam.AngleIncrement", 15.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsSlam.Damage", 6.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsSlam.JumpHeight", 2.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsSlam.Hitbox", 2.0);

        com.projectkorra.projectkorra.configuration.ConfigManager.defaultConfig.save();}

    @Override
    public void stop() {}

    @Override
    public String getAuthor() {
        return "__Yujiro";
    }

    @Override
    public String getVersion() {
        return "2.0 (FORK)";
    }
}