package me.yujiro.dragon.abilities;

import org.bukkit.Location;
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

public class DragonsJudgment extends FireAbility implements AddonAbility {

    private long cooldown, chargetime, tornadoDuration;
    private double speed, range, damage, tornadoRadius;

    private enum State { CHARGING, FIRING, TORNADO }
    private State state;

    private long startTime, tornadoStartTime;
    private boolean charged;
    private Location origin, currentLoc, tornadoCenter;
    private Vector direction;
    private double helixAngle, tornadoTick;
    private int activeTick; 

    public DragonsJudgment(Player player) {
        super(player);
        if (player == null) return;

        if (!bPlayer.canBend(this) || hasAbility(player, this.getClass()) || bPlayer.isOnCooldown(this) || GeneralMethods.isRegionProtectedFromBuild(this, player.getLocation())) {
            return;
        }

        setFields();
        start();
    }

    public void setFields() {
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsJudgment.Cooldown", 15000);
        this.chargetime = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsJudgment.ChargeTime", 2500);
        this.tornadoDuration = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsJudgment.TornadoDuration", 4000);

        this.speed = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJudgment.Speed", 1.2);
        this.range = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJudgment.Range", 40.0);
        this.damage = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJudgment.DamagePerTick", 2.0); // Урон за полсекунды
        this.tornadoRadius = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJudgment.TornadoRadius", 6.0);

        this.startTime = System.currentTimeMillis();
        this.charged = false;
        this.state = State.CHARGING;
        this.helixAngle = 0;
        this.tornadoTick = 0;
        this.activeTick = 0;
    }

    @Override
    public long getCooldown() { return cooldown; }

    @Override
    public Location getLocation() {
        if (state == State.TORNADO) return tornadoCenter;
        if (state == State.FIRING) return currentLoc;
        return player.getLocation();
    }

    @Override
    public String getName() { return "DragonsJudgment"; }

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

        activeTick++;

        if (state == State.CHARGING) {
            if (!player.isSneaking()) {
                remove();
                return;
            }

            long timeElapsed = System.currentTimeMillis() - startTime;
            Location rightHand = GeneralMethods.getRightSide(player.getLocation().add(0, 1.2, 0), 0.8);
            Location leftHand = GeneralMethods.getLeftSide(player.getLocation().add(0, 1.2, 0), 0.8);

            ParticleEffect.FLAME.display(rightHand, 2, 0.1, 0.1, 0.1, 0.01);
            ParticleEffect.SOUL_FIRE_FLAME.display(leftHand, 2, 0.1, 0.1, 0.1, 0.01);

            if (timeElapsed % 1000 < 50) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 0.5f);
            }

            if (timeElapsed >= chargetime && !charged) {
                charged = true;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
                ParticleEffect.EXPLOSION_LARGE.display(player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
            }
        }
        else if (state == State.FIRING) {
            helixAngle += 25;
            currentLoc.add(direction.clone().multiply(speed));

            Vector ortho = GeneralMethods.getOrthogonalVector(direction, 90, 1.2);
            Location redSpiral = currentLoc.clone().add(ortho.clone().rotateAroundAxis(direction, Math.toRadians(helixAngle)));
            Location blueSpiral = currentLoc.clone().add(ortho.clone().rotateAroundAxis(direction, Math.toRadians(helixAngle + 180)));

            ParticleEffect.FLAME.display(redSpiral, 3, 0.05, 0.05, 0.05, 0);
            ParticleEffect.SOUL_FIRE_FLAME.display(blueSpiral, 3, 0.05, 0.05, 0.05, 0);
            ParticleEffect.SMOKE_NORMAL.display(currentLoc, 1, 0.2, 0.2, 0.2, 0);

            if (Math.random() < 0.2) {
                player.getWorld().playSound(currentLoc, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);
            }

            if (currentLoc.distance(origin) > range || GeneralMethods.isSolid(currentLoc.getBlock()) || GeneralMethods.isRegionProtectedFromBuild(this, currentLoc)) {
                triggerTornado();
                return;
            }

            for (Entity target : GeneralMethods.getEntitiesAroundPoint(currentLoc, 2.0)) {
                if (target != player && target instanceof LivingEntity && !target.isDead()) {
                    triggerTornado();
                    return;
                }
            }
        }
        else if (state == State.TORNADO) {
            long elapsedTornado = System.currentTimeMillis() - tornadoStartTime;
            if (elapsedTornado > tornadoDuration) {
                tornadoCenter.getWorld().playSound(tornadoCenter, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
                ParticleEffect.EXPLOSION_HUGE.display(tornadoCenter.clone().add(0, 2, 0), 3, 0, 0, 0, 0);
                remove();
                return;
            }

            tornadoTick += 15;
            for (int i = 0; i < 3; i++) {
                for (double y = 0; y < 6; y += 0.5) {
                    double radius = 1.0 + (y * 0.5);
                    double angle = Math.toRadians(tornadoTick + (y * 40) + (i * 120));

                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location partLoc = tornadoCenter.clone().add(x, y, z);

                    if (i == 0) {
                        ParticleEffect.SOUL_FIRE_FLAME.display(partLoc, 1, 0, 0, 0, 0);
                    } else if (i == 1) {
                        ParticleEffect.FLAME.display(partLoc, 1, 0, 0, 0, 0);
                    } else {
                        ParticleEffect.SMOKE_LARGE.display(partLoc, 1, 0, 0, 0, 0);
                    }
                }
            }

            if (tornadoTick % 10 == 0) {
                tornadoCenter.getWorld().playSound(tornadoCenter, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
            }


            for (Entity target : GeneralMethods.getEntitiesAroundPoint(tornadoCenter, tornadoRadius)) {
                if (target != player && target instanceof LivingEntity && !target.isDead()) {


                    Vector directionToCenter = tornadoCenter.toVector().subtract(target.getLocation().toVector());
                    double dist = directionToCenter.length();

                    if (dist > 0.5) {
                        directionToCenter.normalize();

                        Vector swirl = directionToCenter.clone().crossProduct(new Vector(0, 1, 0)).normalize();


                        Vector trapForce = directionToCenter.multiply(0.5).add(swirl.multiply(0.5));


                        if (target.getLocation().getY() < tornadoCenter.getY() + 3) {
                            trapForce.setY(0.25);
                        } else {
                            trapForce.setY(0);
                        }


                        target.setVelocity(trapForce);
                    } else {

                        target.setVelocity(new Vector(0, 0.05, 0));
                    }

                    target.setFallDistance(0);


                    if (activeTick % 10 == 0) {
                        DamageHandler.damageEntity(target, damage, this);
                        target.setFireTicks(80);
                    }
                }
            }
        }
    }

    public void onClick() {
        if (state == State.CHARGING && charged) {
            state = State.FIRING;
            origin = player.getEyeLocation();
            currentLoc = origin.clone();
            direction = origin.getDirection().normalize();
            player.getWorld().playSound(origin, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.5f, 0.8f);
        }
    }

    private void triggerTornado() {
        state = State.TORNADO;
        tornadoCenter = currentLoc.clone().subtract(direction.clone().multiply(1.5));
        tornadoStartTime = System.currentTimeMillis();
        tornadoCenter.getWorld().playSound(tornadoCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.2f);
    }

    @Override
    public void remove() {
        if (state != State.CHARGING) bPlayer.addCooldown(this);
        super.remove();
    }

    @Override
    public String getDescription() { return "Двойная спираль пламени, которая при столкновении образует Огненное Торнадо."; }
    @Override
    public String getInstructions() { return "Удерживайте Shift для зарядки. Нажмите ЛКМ, чтобы выпустить спираль."; }
    @Override
    public void load() {
        org.bukkit.configuration.file.FileConfiguration config = com.projectkorra.projectkorra.configuration.ConfigManager.getConfig();
        config.addDefault("ExtraAbilities.Yujiro.DragonsJudgment.Cooldown", 15000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJudgment.ChargeTime", 2500);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJudgment.TornadoDuration", 4000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJudgment.Speed", 1.2);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJudgment.Range", 40.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJudgment.DamagePerTick", 2.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJudgment.TornadoRadius", 6.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJudgment.PullStrength", 0.08);
        com.projectkorra.projectkorra.configuration.ConfigManager.defaultConfig.save();}

    @Override
    public void stop() {}
    @Override
    public String getAuthor() { return "Lucifer200134520"; }
    @Override
    public String getVersion() { return "1.0"; }
}
