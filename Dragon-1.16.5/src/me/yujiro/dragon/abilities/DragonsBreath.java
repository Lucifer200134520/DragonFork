package me.yujiro.dragon.abilities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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

public class DragonsBreath extends FireAbility implements AddonAbility {

    // Config
    private long cooldown;
    private long chargetime;
    private long duration;
    private double damage;
    private double range;
    private int firehelixes;

    // Variables
    private long chargestarttime;
    private long breathstarttime;
    private Location origin;
    private Location loc;
    private Vector dir;
    private boolean charged;
    private boolean started;
    private boolean hasbluefire;

    private double arbitraryangleincrement;
    private double angle;
    private double angledifference;
    private BossBar barduration;

    public DragonsBreath(Player player) {
        super(player);

        if (!bPlayer.canBend(this) || hasAbility(player, this.getClass()) || bPlayer.isOnCooldown(this) || GeneralMethods.isRegionProtectedFromBuild(this, player.getLocation())) {
            return;
        }

        setFields();
        start();
    }

    public void setFields() {
        // Config
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsBreath.Cooldown", 8000);
        this.chargetime = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsBreath.ChargeTime", 2000);
        this.duration = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsBreath.Duration", 4000);
        this.damage = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsBreath.Damage", 3.0);
        this.range = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsBreath.Range", 15.0);
        this.firehelixes = ConfigManager.getConfig().getInt("ExtraAbilities.Yujiro.DragonsBreath.FireHelixes", 3);

        // Variables
        this.chargestarttime = System.currentTimeMillis();
        this.charged = false;
        this.started = false;
        this.arbitraryangleincrement = 0;

        if (firehelixes != 0) {
            this.angledifference = 360 / (firehelixes + 1);
        }


        this.hasbluefire = bPlayer.canUseSubElement(Element.BLUE_FIRE);


        BarColor barColor = hasbluefire ? BarColor.BLUE : BarColor.RED;
        this.barduration = Bukkit.getServer().createBossBar("Дыхание Дракона", barColor, BarStyle.SEGMENTED_10);
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "DragonsBreath";
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


        if (!bPlayer.getBoundAbilityName().equalsIgnoreCase(this.getName())) {
            this.remove();
            return;
        }

        long timecharged = System.currentTimeMillis() - chargestarttime;


        if (!started) {
            if (timecharged < chargetime && !player.isSneaking()) {
                this.remove();
                return;
            }

            if (timecharged < chargetime) {

                Location chest = player.getLocation().add(0, 1.2, 0);
                ParticleEffect.SMOKE_NORMAL.display(chest, 2, 0.5, 0.5, 0.5, 0.01);
                if (Math.random() < 0.2) {
                    playFirebendingSound(chest);
                }
            }
            else if (!charged) {

                charged = true;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
                ParticleEffect.FLAME.display(player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            }
            return;
        }


        player.setFireTicks(0);
        long timeelapsed = System.currentTimeMillis() - breathstarttime;
        double progress = 1.0 - ((double) timeelapsed / (double) duration);

        if (progress <= 0) {
            barduration.setProgress(0);
            this.remove();
            return;
        } else {
            barduration.setProgress(progress);
        }

        this.origin = player.getEyeLocation();
        this.dir = origin.getDirection().normalize();

        if (Math.random() < 0.2) {
            playFirebendingSound(origin);
        }

        arbitraryangleincrement += 5;

        for (double d = 0; d < range; d += 0.5) {
            angle = arbitraryangleincrement + 10 * d;
            this.loc = origin.clone().add(dir.clone().multiply(d));


            double hitboxRadius = Math.log(d + 2) * 1.5;

            if (d < 1) {
                Vector smokeorthagonal = GeneralMethods.getOrthogonalVector(dir, 90, 1);
                for (double smokeangle = 0; smokeangle < 360; smokeangle += 20) {
                    Location smokeloc = loc.clone().add(dir.clone().multiply(2)).add(smokeorthagonal.clone().multiply(d + 0.5).rotateAroundAxis(dir, Math.toRadians(smokeangle)));
                    ParticleEffect.SMOKE_NORMAL.display(smokeloc, 1, 0.1, 0.1, 0.1, 0);
                }
            }

            for (int i = 0; i < firehelixes; i++) {
                Vector orthagonal = GeneralMethods.getOrthogonalVector(dir, 90, Math.log(d + 2) * Math.log(d + 2));
                Location helixloc = loc.clone().add(dir.clone().multiply(2)).add(orthagonal.clone().rotateAroundAxis(dir, Math.toRadians(angle + angledifference * i)));

                if (!GeneralMethods.isObstructed(loc, helixloc)) {
                    if (hasbluefire) {
                        ParticleEffect.SOUL_FIRE_FLAME.display(helixloc, 1, 0.1, 0.1, 0.1, 0.02);
                    } else {
                        ParticleEffect.FLAME.display(helixloc, 1, 0.1 ,0.1 ,0.1, 0.02);
                    }
                }
            }


            if (hasbluefire) {
                ParticleEffect.SOUL_FIRE_FLAME.display(loc.clone().add(dir.clone().multiply(2)), 2, d/4, d/4, d/4, 0);
            } else {
                ParticleEffect.FLAME.display(loc.clone().add(dir.clone().multiply(2)), 2, d/4, d/4, d/4, 0);
            }


            for (Entity target : GeneralMethods.getEntitiesAroundPoint(loc, hitboxRadius)) {
                if (target != player && target instanceof LivingEntity && !target.isDead()) {
                    DamageHandler.damageEntity(target, damage, this);
                    target.setFireTicks(60);


                    target.setVelocity(target.getVelocity().add(dir.clone().multiply(0.15)));
                }
            }
        }
    }


    public void onClick() {
        if (charged && !started) {
            started = true;
            breathstarttime = System.currentTimeMillis();
            this.barduration.addPlayer(player);


            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.0f);
        }
    }

    @Override
    public void remove() {
        if (barduration != null) {
            barduration.removeAll();
        }
        if (this.started) {
            bPlayer.addCooldown(this);
        }
        super.remove();
    }

    @Override
    public String getDescription() {
        return "Создайте разрушительное пламя, способное соперничать с дыханием самих драконов.";
    }

    @Override
    public String getInstructions() {
        return "Удерживайте Shift для накопления энергии (пока не услышите звон). Затем нажмите Левую кнопку мыши, чтобы высвободить поток пламени!";
    }

    @Override
    public void load() {
        org.bukkit.configuration.file.FileConfiguration config = com.projectkorra.projectkorra.configuration.ConfigManager.getConfig();
        config.addDefault("ExtraAbilities.Yujiro.DragonsBreath.Cooldown", 8000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsBreath.ChargeTime", 2000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsBreath.Duration", 4000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsBreath.Damage", 3.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsBreath.Range", 15.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsBreath.FireHelixes", 3);

        com.projectkorra.projectkorra.configuration.ConfigManager.defaultConfig.save();
    }

    @Override
    public void stop() {
    }

    @Override
    public String getAuthor() {
        return "__Yujiro ";
    }

    @Override
    public String getVersion() {
        return "2.0 (FORK)";
    }
}