package me.yujiro.dragon.abilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

import me.yujiro.dragon.DragonElement;

public class DragonsCanvas extends FireAbility implements AddonAbility {

    private long cooldown;
    private double damage, hitbox, drawDistance;
    private int maxPoints, speed;

    private boolean firing, hasbluefire;
    private List<Location> path;
    private int currentPointIndex;
    private Particle.DustOptions inkColor;

    public DragonsCanvas(Player player) {
        super(player);
        if (player == null) return;

        if (!bPlayer.canBend(this) || hasAbility(player, this.getClass()) || bPlayer.isOnCooldown(this) || GeneralMethods.isRegionProtectedFromBuild(this, player.getLocation())) {
            return;
        }

        setFields();
        start();
    }

    public void setFields() {
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsCanvas.Cooldown", 8000);
        this.damage = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsCanvas.Damage", 6.0);
        this.hitbox = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsCanvas.Hitbox", 3.0);
        this.drawDistance = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsCanvas.DrawDistance", 7.0);
        this.maxPoints = ConfigManager.getConfig().getInt("ExtraAbilities.Yujiro.DragonsCanvas.MaxPoints", 100);
        this.speed = ConfigManager.getConfig().getInt("ExtraAbilities.Yujiro.DragonsCanvas.Speed", 3);

        this.hasbluefire = bPlayer.canUseSubElement(Element.BLUE_FIRE);
        this.firing = false;
        this.path = new ArrayList<>();
        this.currentPointIndex = 0;

        this.inkColor = new Particle.DustOptions(hasbluefire ? Color.fromRGB(0, 200, 255) : Color.fromRGB(255, 150, 0), 1.0f);
    }

    @Override
    public long getCooldown() { return cooldown; }

    @Override
    public Location getLocation() {
        if (path != null && !path.isEmpty() && currentPointIndex < path.size()) {
            return path.get(currentPointIndex);
        }
        return player != null ? player.getLocation() : null;
    }

    @Override
    public String getName() { return "DragonsCanvas"; }

    @Override
    public boolean isHarmlessAbility() { return false; }

    @Override
    public boolean isSneakAbility() { return true; }

    @Override
    public Element getElement() { return DragonElement.DRAGON_FIRE; }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || bPlayer.isChiBlocked()) {
            remove();
            return;
        }

        if (!firing) {
            if (!player.isSneaking()) {
                startFiring();
                return;
            }
            if (!bPlayer.getBoundAbilityName().equalsIgnoreCase(this.getName())) {
                remove();
                return;
            }
            Location target = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(drawDistance));
            if (path.isEmpty() || path.get(path.size() - 1).distance(target) > 0.4) {
                if (path.size() < maxPoints) {
                    path.add(target);
                    player.getWorld().playSound(target, Sound.BLOCK_ANVIL_DESTROY, 0.5f, 1.5f);
                }
            }
            for (int i = 0; i < path.size(); i += 2) {
                ParticleEffect.REDSTONE.display(path.get(i), 1, 0, 0, 0, 0, inkColor);
            }
        } else {
            if (path.isEmpty()) {
                remove();
                return;
            }
            for (int i = 0; i < speed; i++) {
                if (currentPointIndex >= path.size()) {
                    triggerFinish();
                    return;
                }
                Location head = path.get(currentPointIndex);
                if (GeneralMethods.isSolid(head.getBlock()) || GeneralMethods.isRegionProtectedFromBuild(this, head)) {
                    triggerFinish();
                    return;
                }

                if (hasbluefire) {
                    ParticleEffect.SOUL_FIRE_FLAME.display(head, 8, 0.8, 0.8, 0.8, 0.05);
                    ParticleEffect.FLASH.display(head, 1, 0, 0, 0, 0);
                } else {
                    ParticleEffect.FLAME.display(head, 8, 0.8, 0.8, 0.8, 0.05);
                    ParticleEffect.LAVA.display(head, 2, 0.4, 0.4, 0.4, 0);
                }

                if (currentPointIndex % 2 == 0) head.getWorld().playSound(head, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 1.0f);

                boolean hit = false;
                for (Entity target : GeneralMethods.getEntitiesAroundPoint(head, hitbox)) {
                    if (target != player && target instanceof LivingEntity && !target.isDead()) {
                        hit = true;
                        break;
                    }
                }

                if (hit) {
                    triggerFinish();
                    return;
                }
                currentPointIndex++;
            }
        }
    }

    private void startFiring() {
        firing = true;
        if (path == null || path.size() < 3) {
            remove();
            return;
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);
        Vector firstDir = path.get(Math.min(2, path.size() - 1)).toVector().subtract(path.get(0).toVector()).normalize();
        for (Location loc : path) loc.setDirection(firstDir);
    }

    private void triggerFinish() {
        Location end = getLocation();
        if (end != null) {
            end.getWorld().playSound(end, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
            ParticleEffect.EXPLOSION_HUGE.display(end, 3, 1, 1, 1, 0);

            for (int i = 0; i < 20; i++) {
                Vector blastDir = new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize();
                Location blastLoc = end.clone().add(blastDir);
                if (hasbluefire) {
                    ParticleEffect.SOUL_FIRE_FLAME.display(blastLoc, 1, 0.2, 0.2, 0.2, 0.1);
                } else {
                    ParticleEffect.FLAME.display(blastLoc, 1, 0.2, 0.2, 0.2, 0.1);
                }
            }

            for (Entity target : GeneralMethods.getEntitiesAroundPoint(end, hitbox * 1.5)) {
                if (target != player && target instanceof LivingEntity && !target.isDead()) {
                    DamageHandler.damageEntity(target, damage, this);
                    target.setFireTicks(100);
                    Vector knockback = target.getLocation().toVector().subtract(end.toVector()).normalize().multiply(1.2).setY(0.4);
                    target.setVelocity(target.getVelocity().add(knockback));
                }
            }
        }
        remove();
    }

    public void onClick() { if (!firing) startFiring(); }

    @Override
    public void remove() {
        if (firing || (path != null && path.size() > 5)) bPlayer.addCooldown(this);
        super.remove();
    }

    @Override
    public String getDescription() { return "Рисуйте траекторию пламени прямо в воздухе. В конце пути дракон взрывается."; }
    @Override
    public String getInstructions() { return "Удерживайте Shift и ведите камерой, чтобы нарисовать путь. Отпустите Shift."; }
    @Override
    public void load() {
        org.bukkit.configuration.file.FileConfiguration config = com.projectkorra.projectkorra.configuration.ConfigManager.getConfig();
        config.addDefault("ExtraAbilities.Yujiro.DragonsCanvas.Cooldown", 8000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsCanvas.Damage", 6.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsCanvas.Hitbox", 3.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsCanvas.DrawDistance", 7.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsCanvas.MaxPoints", 100);
        config.addDefault("ExtraAbilities.Yujiro.DragonsCanvas.Speed", 3);
        com.projectkorra.projectkorra.configuration.ConfigManager.defaultConfig.save();}
    @Override
    public void stop() {}
    @Override
    public String getAuthor() { return "Lucifer200134520"; }
    @Override
    public String getVersion() { return "1.0"; }
}