package me.yujiro.dragon.abilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ParticleEffect;

import me.yujiro.dragon.DragonElement;

public class DragonsScales extends FireAbility implements AddonAbility {

    private long cooldown, duration;
    private double clickradius, shiftrange, shiftlength, shiftgrowspeed, shiftheight, collisionradius;

    private long starttime;
    private Location origin, loc;
    private Vector dir;
    private boolean hasbluefire, hasshifted;
    private double currentshiftrange;
    private List<Location> conelocs;
    private List<Location> walllocs;

    private Vector normalisedxzvector, orthagonalleft, orthagonalright;

    public DragonsScales(Player player) {
        super(player);

        if (!bPlayer.canBend(this) || hasAbility(player, this.getClass()) || bPlayer.isOnCooldown(this) || GeneralMethods.isRegionProtectedFromBuild(this, player.getLocation())) {
            return;
        }

        setFields();
        start();
    }

    public void setFields() {
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsScales.Cooldown", 4000);
        this.duration = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsScales.Duration", 5000);

        this.clickradius = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsScales.ClickRadius", 2.0);
        this.shiftrange = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsScales.ShiftRange", 5.0);
        this.shiftlength = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsScales.ShiftLength", 4.0);
        this.shiftgrowspeed = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsScales.ShiftGrowSpeed", 0.5);
        this.shiftheight = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsScales.ShiftHeight", 3.0);
        this.collisionradius = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsScales.CollisionRadius", 1.5);

        this.starttime = System.currentTimeMillis();
        this.origin = player.getEyeLocation();
        this.dir = origin.getDirection();
        this.loc = origin.clone().add(dir);
        this.hasbluefire = bPlayer.canUseSubElement(Element.BLUE_FIRE);
        this.hasshifted = false;

        ProjectKorra.getCollisionInitializer().addSmallAbility(this);

        this.currentshiftrange = 0;
        this.conelocs = new ArrayList<>();
        this.walllocs = new ArrayList<>();

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.5f);
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
        return "DragonsScales";
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
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

        if (System.currentTimeMillis() - starttime > duration) {
            this.remove();
            return;
        }

        this.origin = player.getEyeLocation();
        this.dir = origin.getDirection();
        this.loc = origin.clone().add(dir);

        if (!hasshifted) {
            this.conelocs.clear();
            for (double d = 0.2; d < clickradius; d += 0.5) {
                Vector orthagonal = GeneralMethods.getOrthogonalVector(dir, 90, d);
                for (double theta = 0; theta < 360; theta += 30) {
                    Location temploc = loc.clone().add(orthagonal.clone().rotateAroundAxis(dir, Math.toRadians(theta)).add(dir.clone().multiply(d)));
                    conelocs.add(temploc);

                    if (Math.random() < 0.3) {
                        if (!hasbluefire) {
                            ParticleEffect.FLAME.display(temploc, 1, 0, 0, 0, 0.01);
                        } else {
                            ParticleEffect.SOUL_FIRE_FLAME.display(temploc, 1, 0, 0, 0, 0.01);
                        }
                    }
                }
            }
        } else {
            currentshiftrange += shiftgrowspeed;
            if (currentshiftrange > shiftrange) {
                this.remove();
                return;
            }

            this.walllocs.clear();
            for (double d = -shiftlength / 2; d < shiftlength / 2; d += 0.4) {
                for (double h = 0; h < shiftheight; h += 0.4) {
                    Location templocleft = player.getLocation().add(orthagonalleft.clone().multiply(currentshiftrange)).add(new Vector(0, h, 0)).add(normalisedxzvector.clone().multiply(d));
                    Location templocright = player.getLocation().add(orthagonalright.clone().multiply(currentshiftrange)).add(new Vector(0, h, 0)).add(normalisedxzvector.clone().multiply(d));

                    walllocs.add(templocleft);
                    walllocs.add(templocright);

                    if (!hasbluefire) {
                        ParticleEffect.FLAME.display(templocleft, 1, 0.1, 0.1, 0.1, 0.02);
                        ParticleEffect.FLAME.display(templocright, 1, 0.1, 0.1, 0.1, 0.02);
                    } else {
                        ParticleEffect.SOUL_FIRE_FLAME.display(templocleft, 1, 0.1, 0.1, 0.1, 0.02);
                        ParticleEffect.SOUL_FIRE_FLAME.display(templocright, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            }
        }
    }

    @Override
    public double getCollisionRadius() {
        return collisionradius;
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public List<Location> getLocations() {
        return hasshifted ? walllocs : conelocs;
    }

    public void onClick() {
        this.remove();
    }

    public void onShift() {
        if (!hasshifted) {
            ProjectKorra.getCollisionInitializer().addLargeAbility(this);
            hasshifted = true;
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);

            double pitch = Math.toRadians(player.getLocation().getPitch());
            double yaw = Math.toRadians(player.getLocation().getYaw());
            normalisedxzvector = new Vector(-Math.cos(pitch) * Math.sin(yaw), 0, Math.cos(pitch) * Math.cos(yaw)).normalize();
            orthagonalleft = normalisedxzvector.clone().rotateAroundY(Math.toRadians(90));
            orthagonalright = normalisedxzvector.clone().rotateAroundY(Math.toRadians(270));
        }
    }

    @Override
    public void remove() {
        bPlayer.addCooldown(this);
        super.remove();
    }

    @Override
    public String getDescription() {
        return "Создайте плотный огненный щит (Чешую Дракона), чтобы блокировать вражеские атаки.";
    }

    @Override
    public String getInstructions() {
        return "ЛКМ для создания фронтального щита. ЛКМ снова для отмены. Нажмите Shift, чтобы расширить щит и защитить фланги.";
    }

    @Override
    public void load() {
        org.bukkit.configuration.file.FileConfiguration config = com.projectkorra.projectkorra.configuration.ConfigManager.getConfig();
        config.addDefault("ExtraAbilities.Yujiro.DragonsScales.Cooldown", 4000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsScales.Duration", 5000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsScales.ClickRadius", 2.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsScales.ShiftRange", 5.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsScales.ShiftLength", 4.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsScales.ShiftGrowSpeed", 0.5);
        config.addDefault("ExtraAbilities.Yujiro.DragonsScales.ShiftHeight", 3.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsScales.CollisionRadius", 1.5);
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