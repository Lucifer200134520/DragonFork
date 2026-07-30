package me.yujiro.dragon.abilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
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

public class DragonsRails extends FireAbility implements AddonAbility {

    private long cooldown, drawTimeLimit, bombInterval;
    private double speed, bombDamage, kidnapDamage, throwPower;
    private int maxRides;

    private enum State { DRAWING, RIDING }
    private State state;

    private long drawStartTime, lastBombTime, lastKidnapDamageTime;
    private List<Location> path;
    private double[] cumulativeDistances;
    private double totalLength;
    private double currentDist;
    private int direction;
    private int rideCount;
    private LivingEntity kidnapped;
    private boolean hasbluefire;

    private List<Bomb> bombs;

    public DragonsRails(Player player) {
        super(player);
        if (player == null) return;

        if (!bPlayer.canBend(this) || hasAbility(player, this.getClass()) || bPlayer.isOnCooldown(this)) {
            return;
        }

        setFields();
        start();
    }

    public void setFields() {
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsRails.Cooldown", 15000);
        this.drawTimeLimit = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsRails.DrawTimeLimit", 40000);
        this.bombInterval = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsRails.BombInterval", 500);
        this.speed = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsRails.Speed", 1.8);
        this.bombDamage = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsRails.BombDamage", 4.0);
        this.kidnapDamage = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsRails.KidnapDamageTick", 1.0);
        this.throwPower = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsRails.ThrowPower", 2.5);
        this.maxRides = ConfigManager.getConfig().getInt("ExtraAbilities.Yujiro.DragonsRails.MaxRides", 5);

        this.state = State.DRAWING;
        this.drawStartTime = System.currentTimeMillis();
        this.path = new ArrayList<>();
        this.path.add(player.getLocation().add(0, 1, 0));

        this.currentDist = 0;
        this.direction = 1;
        this.rideCount = 0;
        this.bombs = new ArrayList<>();
        this.hasbluefire = bPlayer.canUseSubElement(Element.BLUE_FIRE);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    @Override
    public long getCooldown() { return cooldown; }

    @Override
    public Location getLocation() { return player.getLocation(); }

    @Override
    public String getName() { return "DragonsRails"; }

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

        if (state == State.DRAWING) {
            if (!bPlayer.getBoundAbilityName().equalsIgnoreCase(this.getName())) {
                remove();
                return;
            }

            if (System.currentTimeMillis() - drawStartTime > drawTimeLimit) {
                remove();
                return;
            }

            Location currentLoc = player.getLocation().add(0, 1, 0);
            if (currentLoc.distance(path.get(path.size() - 1)) > 1.0) {
                path.add(currentLoc);
            }

            for (int i = 0; i < path.size(); i++) {
                if (hasbluefire) {
                    ParticleEffect.SOUL_FIRE_FLAME.display(path.get(i), 1, 0, 0, 0, 0);
                } else {
                    ParticleEffect.FLAME.display(path.get(i), 1, 0, 0, 0, 0);
                }
            }
        }
        else if (state == State.RIDING) {
            player.setFallDistance(0);

            currentDist += speed * direction;

            if (currentDist >= totalLength) {
                rideCount++;
                if (rideCount >= maxRides) {
                    dismountAndThrow();
                    return;
                }
                direction = -1;
                currentDist = totalLength;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.5f);
            } else if (currentDist <= 0) {
                rideCount++;
                if (rideCount >= maxRides) {
                    dismountAndThrow();
                    return;
                }
                direction = 1;
                currentDist = 0;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.5f);
            }


            Location exactTrackLoc = getPointAtDistance(currentDist);
            Location nextTrackLoc = getPointAtDistance(currentDist + (speed * direction));

            Vector tangent = nextTrackLoc.toVector().subtract(exactTrackLoc.toVector());
            if (tangent.lengthSquared() > 0.001) {
                tangent.normalize().multiply(speed);
            } else {
                tangent = player.getLocation().getDirection().multiply(speed);
            }

            Vector toTrack = exactTrackLoc.toVector().subtract(player.getLocation().toVector());


            if (toTrack.lengthSquared() > 64.0) {
                Location tp = exactTrackLoc.clone();
                tp.setYaw(player.getLocation().getYaw());
                tp.setPitch(player.getLocation().getPitch());
                player.teleport(tp);
            } else {

                Vector finalVel = tangent.add(toTrack.multiply(0.3));
                player.setVelocity(finalVel);
            }

            if (hasbluefire) {
                ParticleEffect.SOUL_FIRE_FLAME.display(player.getLocation(), 5, 0.5, 0.5, 0.5, 0.1);
            } else {
                ParticleEffect.FLAME.display(player.getLocation(), 5, 0.5, 0.5, 0.5, 0.1);
            }

            if (kidnapped == null) {
                for (Entity e : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), 2.0)) {
                    if (e instanceof LivingEntity && !e.equals(player)) {
                        kidnapped = (LivingEntity) e;
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.8f);
                        break;
                    }
                }
            }

            Vector forwardDir = tangent.clone().normalize();

            if (kidnapped != null) {
                if (kidnapped.isDead() || !kidnapped.isValid()) {
                    kidnapped = null;
                } else {
                    kidnapped.teleport(player.getLocation().add(forwardDir.clone().multiply(1.5)));
                    kidnapped.setFallDistance(0);
                    kidnapped.setFireTicks(40);

                    if (System.currentTimeMillis() - lastKidnapDamageTime > 500) {
                        DamageHandler.damageEntity(kidnapped, kidnapDamage, this);
                        lastKidnapDamageTime = System.currentTimeMillis();
                    }
                }
            }

            if (System.currentTimeMillis() - lastBombTime > bombInterval) {
                lastBombTime = System.currentTimeMillis();

                Vector flatForward = forwardDir.clone().setY(0).normalize();
                if (flatForward.lengthSquared() < 0.1) flatForward = new Vector(1, 0, 0);

                Vector right = flatForward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                Vector left = right.clone().multiply(-1);

                bombs.add(new Bomb(player.getLocation().add(0, 1, 0), right.add(new Vector(0, -0.5, 0)).normalize().multiply(1.5)));
                bombs.add(new Bomb(player.getLocation().add(0, 1, 0), left.add(new Vector(0, -0.5, 0)).normalize().multiply(1.5)));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.5f);
            }

            Iterator<Bomb> it = bombs.iterator();
            while (it.hasNext()) {
                if (it.next().tick()) {
                    it.remove();
                }
            }
        }
    }


    private void smoothPath() {
        if (path.size() < 3) return;
        for (int passes = 0; passes < 3; passes++) {
            List<Location> smoothed = new ArrayList<>();
            smoothed.add(path.get(0));
            for (int i = 1; i < path.size() - 1; i++) {
                Location p0 = path.get(i - 1);
                Location p1 = path.get(i);
                Location p2 = path.get(i + 1);
                double x = (p0.getX() + p1.getX() * 2 + p2.getX()) / 4.0;
                double y = (p0.getY() + p1.getY() * 2 + p2.getY()) / 4.0;
                double z = (p0.getZ() + p1.getZ() * 2 + p2.getZ()) / 4.0;
                smoothed.add(new Location(p1.getWorld(), x, y, z));
            }
            smoothed.add(path.get(path.size() - 1));
            path = smoothed;
        }
    }

    private Location getPointAtDistance(double dist) {
        if (dist <= 0) return path.get(0).clone();
        if (dist >= totalLength) return path.get(path.size() - 1).clone();

        for (int i = 0; i < path.size() - 1; i++) {
            double startDist = cumulativeDistances[i];
            double endDist = cumulativeDistances[i + 1];

            if (dist >= startDist && dist <= endDist) {
                double segmentLength = endDist - startDist;
                double ratio = (dist - startDist) / segmentLength;

                Location p1 = path.get(i);
                Location p2 = path.get(i + 1);
                Vector dir = p2.toVector().subtract(p1.toVector());

                return p1.clone().add(dir.multiply(ratio));
            }
        }
        return path.get(path.size() - 1).clone();
    }

    public void onClick() {
        if (state == State.DRAWING) {
            if (path.size() < 3) {
                remove();
                return;
            }


            smoothPath();

            cumulativeDistances = new double[path.size()];
            cumulativeDistances[0] = 0;
            for (int i = 1; i < path.size(); i++) {
                cumulativeDistances[i] = cumulativeDistances[i - 1] + path.get(i).distance(path.get(i - 1));
            }
            totalLength = cumulativeDistances[path.size() - 1];

            state = State.RIDING;
            currentDist = 0;

            Location startTp = path.get(0).clone();
            startTp.setYaw(player.getLocation().getYaw());
            startTp.setPitch(player.getLocation().getPitch());
            player.teleport(startTp);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.0f);
        }
    }

    public void onShift() {
        if (state == State.RIDING) {
            dismountAndThrow();
        }
    }

    private void dismountAndThrow() {
        if (kidnapped != null) {
            Vector throwDir = player.getLocation().getDirection().normalize().multiply(throwPower).setY(0.8);
            kidnapped.setVelocity(throwDir);

            final LivingEntity target = kidnapped;
            new org.bukkit.scheduler.BukkitRunnable() {
                int ticks = 0;
                public void run() {
                    if (ticks++ > 40 || target.isDead() || target.isOnGround() || GeneralMethods.isSolid(target.getLocation().getBlock())) {
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
                        ParticleEffect.EXPLOSION_LARGE.display(target.getLocation(), 2, 1, 1, 1, 0);
                        DamageHandler.damageEntity(target, bombDamage * 2, DragonsRails.this);
                        this.cancel();
                    } else {
                        if (hasbluefire) {
                            ParticleEffect.SOUL_FIRE_FLAME.display(target.getLocation(), 2, 0.5, 0.5, 0.5, 0);
                        } else {
                            ParticleEffect.FLAME.display(target.getLocation(), 2, 0.5, 0.5, 0.5, 0);
                        }
                    }
                }
            }.runTaskTimer(com.projectkorra.projectkorra.ProjectKorra.plugin, 0L, 1L);
        }
        remove();
    }

    private class Bomb {
        private Location loc;
        private Vector velocity;
        private int ticks;

        public Bomb(Location loc, Vector velocity) {
            this.loc = loc;
            this.velocity = velocity;
            this.ticks = 0;
        }

        public boolean tick() {
            ticks++;
            velocity.setY(velocity.getY() - 0.05);
            loc.add(velocity);

            if (hasbluefire) {
                ParticleEffect.SOUL_FIRE_FLAME.display(loc, 2, 0.1, 0.1, 0.1, 0);
            } else {
                ParticleEffect.FLAME.display(loc, 2, 0.1, 0.1, 0.1, 0);
            }

            boolean hit = false;
            Block b = loc.getBlock();
            if (GeneralMethods.isSolid(b) && !GeneralMethods.isRegionProtectedFromBuild(DragonsRails.this, loc)) {
                hit = true;
            } else {
                for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, 2.0)) {
                    if (e instanceof LivingEntity && !e.equals(player) && !e.equals(kidnapped)) {
                        hit = true;
                        break;
                    }
                }
            }

            if (hit || ticks > 60) {
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                ParticleEffect.EXPLOSION_LARGE.display(loc, 1, 0.5, 0.5, 0.5, 0);

                for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, 3.0)) {
                    if (e instanceof LivingEntity && !e.equals(player) && !e.equals(kidnapped)) {
                        DamageHandler.damageEntity(e, bombDamage, DragonsRails.this);
                        e.setFireTicks(60);
                    }
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public void remove() {
        bPlayer.addCooldown(this);
        super.remove();
    }

    @Override
    public String getDescription() { return "Создайте огненные рельсы в воздухе, промчитесь по ним и обрушьте на врагов град бомб."; }
    @Override
    public String getInstructions() { return "ЛКМ, чтобы начать прокладку пути. ЛКМ снова, чтобы поехать по нему. Нажмите Shift во время езды, чтобы спрыгнуть."; }

    @Override
    public void load() {
        org.bukkit.configuration.file.FileConfiguration config = com.projectkorra.projectkorra.configuration.ConfigManager.getConfig();
        config.addDefault("ExtraAbilities.Yujiro.DragonsRails.Cooldown", 15000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsRails.DrawTimeLimit", 40000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsRails.BombInterval", 500);
        config.addDefault("ExtraAbilities.Yujiro.DragonsRails.Speed", 1.8);
        config.addDefault("ExtraAbilities.Yujiro.DragonsRails.BombDamage", 4.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsRails.KidnapDamageTick", 1.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsRails.ThrowPower", 2.5);
        config.addDefault("ExtraAbilities.Yujiro.DragonsRails.MaxRides", 5);
        com.projectkorra.projectkorra.configuration.ConfigManager.defaultConfig.save();
    }

    @Override
    public void stop() {}
    @Override
    public String getAuthor() { return "Lucifer200134520"; }
    @Override
    public String getVersion() { return "1.0"; }
}