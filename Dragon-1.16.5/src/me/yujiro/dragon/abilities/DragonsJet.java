package me.yujiro.dragon.abilities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ParticleEffect;

import me.yujiro.dragon.DragonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DragonsJet extends FireAbility implements AddonAbility {

    private long cooldown;
    private double manatotal;
    private double manaregen;
    private double manausehover;
    private double manauseleap;
    private double manauseslowfly;
    private double manausefastfly;

    private double leapspeed;
    private double fastflyspeed;
    private double slowflyspeed;

    private Location playerloc;
    private Vector dir;
    private double manaleft;
    private BossBar barduration;
    private boolean bluefire;

    private enum JetState {
        OFF, LEAP, HOVER, FLY;
    }

    private JetState state;

    public DragonsJet(Player player) {
        super(player);

        if (!bPlayer.canBend(this) || hasAbility(player, this.getClass()) || bPlayer.isOnCooldown(this) || GeneralMethods.isRegionProtectedFromBuild(this, player.getLocation())) {
            return;
        }

        setFields();
        start();
        bPlayer.addCooldown(this);
    }

    public void setFields() {
        // Config
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsJet.Cooldown", 5000);

        this.manatotal = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJet.ManaTotal", 100.0);
        this.manaregen = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJet.ManaRegenPerSecond", 5.0);
        this.manausehover = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJet.ManaUseHoverPerSecond", 4.0);
        this.manauseleap = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJet.ManaUseLeap", 15.0);
        this.manauseslowfly = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJet.ManaUseSlowFlyPerSecond", 6.0);
        this.manausefastfly = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJet.ManaUseFastFlyPerSecond", 10.0);

        this.leapspeed = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJet.LeapSpeed", 1.5);
        this.slowflyspeed = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJet.SlowFlySpeed", 0.6);
        this.fastflyspeed = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsJet.FastFlySpeed", 1.2);

        // Variables
        this.playerloc = player.getLocation();
        this.dir = playerloc.getDirection().normalize();
        this.manaleft = manatotal;
        this.bluefire = bPlayer.canUseSubElement(Element.BLUE_FIRE);
        this.state = JetState.OFF;

        this.barduration = Bukkit.getServer().createBossBar("Запас пламени", bluefire ? BarColor.BLUE : BarColor.RED, BarStyle.SOLID);
        this.barduration.addPlayer(player);

        this.onRightClick(); // Автоматически запускаем прыжок при старте
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return playerloc;
    }

    @Override
    public String getName() {
        return "DragonsJet";
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
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

        this.playerloc = player.getLocation();
        this.dir = playerloc.getDirection().normalize();

        if (manaleft <= 0) {
            this.remove();
            return;
        }

        double progress = (manaleft / manatotal);
        if (progress < 0) {
            this.remove();
            return;
        } else {
            barduration.setProgress(Math.min(1.0, progress));
        }

        if (state != JetState.FLY || !player.isSneaking()) {
            if (player.isGliding()) {
                player.setGliding(false);
            }
        }

        if (state == JetState.OFF) {
            if (bPlayer.getBoundAbilityName().equalsIgnoreCase(this.getName())) {
                player.sendActionBar(Component.text("Полет: ВЫКЛ").color(NamedTextColor.DARK_GRAY));
            }

            if ((manaleft + manaregen / 20) < manatotal) {
                manaleft += manaregen / 20;
            } else {
                manaleft = manatotal;
            }

            if (!bPlayer.getBoundAbilityName().equalsIgnoreCase(this.getName())) {
                barduration.setVisible(false);
            } else {
                barduration.setVisible(true);
            }
        } else {
            barduration.setVisible(true);
        }

        if (state == JetState.LEAP) {
            playParticles(false, true, state);
            if (player.getVelocity().getY() <= 0) {
                state = JetState.FLY;
            }
        }

        if (state == JetState.HOVER) {
            player.setFallDistance(0);
            playParticles(true, false, state);
            ParticleEffect.SMOKE_NORMAL.display(playerloc.clone().subtract(0, 0.2, 0), 2, 0.2, 0.2, 0.2, 0.01);

            if (bPlayer.getBoundAbilityName().equalsIgnoreCase(this.getName())) {
                player.sendActionBar(Component.text("Полет: ЗАВИСАНИЕ").color(NamedTextColor.GOLD));
            }

            player.setVelocity(new Vector(0, 0, 0));
            manaleft -= manausehover / 20;
        }

        if (state == JetState.FLY) {
            player.setFallDistance(0);
            playParticles(true, true, state);

            if (bPlayer.getBoundAbilityName().equalsIgnoreCase(this.getName())) {
                player.sendActionBar(Component.text("Полет: АКТИВЕН").color(NamedTextColor.RED));
            }

            if (player.isSneaking()) {
                manaleft -= manausefastfly / 20;
                player.setVelocity(dir.clone().multiply(fastflyspeed));
                player.setGliding(true);
            } else {
                player.setGliding(false);
                manaleft -= manauseslowfly / 20;
                player.setVelocity(dir.clone().multiply(slowflyspeed));
            }
        }
    }

    public void playParticles(boolean feet, boolean hands, JetState position) {
        Location ploc = playerloc.clone();
        if (position == JetState.FLY) {
            if (feet) {
                spawnJetFire(GeneralMethods.getRightSide(ploc.clone().subtract(dir), 0.3));
                spawnJetFire(GeneralMethods.getLeftSide(ploc.clone().subtract(dir), 0.3));
            }
            if (hands) {
                spawnJetFire(GeneralMethods.getRightSide(ploc.clone().add(0, 1.2, 0), 0.4));
                spawnJetFire(GeneralMethods.getLeftSide(ploc.clone().add(0, 1.2, 0), 0.4));
            }
        } else {
            if (feet) {
                spawnJetFire(GeneralMethods.getRightSide(ploc, 0.3));
                spawnJetFire(GeneralMethods.getLeftSide(ploc, 0.3));
            }
            if (hands) {
                spawnJetFire(GeneralMethods.getRightSide(ploc.clone().add(0, 1, 0), 0.4));
                spawnJetFire(GeneralMethods.getLeftSide(ploc.clone().add(0, 1, 0), 0.4));
            }
        }
    }

    private void spawnJetFire(Location loc) {
        if (bluefire) {
            ParticleEffect.SOUL_FIRE_FLAME.display(loc, 2, 0.1, 0.1, 0.1, 0.02);
        } else {
            ParticleEffect.FLAME.display(loc, 2, 0.1, 0.1, 0.1, 0.02);
        }
    }


    public void onLeftClick() {
        if (player.isSneaking()) {
            if (state == JetState.OFF) {
                state = JetState.HOVER;
                player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1f);
            } else {
                state = JetState.OFF;
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1f);
            }
        } else {
            if (state == JetState.FLY) {
                state = JetState.HOVER;
            } else if (state == JetState.HOVER) {
                state = JetState.FLY;
            }
        }
    }


    public void onRightClick() {
        player.getWorld().playSound(playerloc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
        ParticleEffect.EXPLOSION_LARGE.display(playerloc, 1, 0.5, 0.5, 0.5, 0);

        player.setVelocity(new Vector(0, 1, 0).multiply(leapspeed));
        this.state = JetState.LEAP;

        player.sendActionBar(Component.text("ПРЫЖОК").color(NamedTextColor.YELLOW));
        manaleft -= manauseleap;
    }

    @Override
    public void remove() {
        if (barduration != null) {
            barduration.removeAll();
        }
        if (player.isGliding()) {
            player.setGliding(false);
        }
        bPlayer.addCooldown(this);
        super.remove();
    }

    @Override
    public String getDescription() {
        return "Используйте драконье пламя для невероятных маневров в воздухе. Эта способность использует запас 'маны' (пламени).";
    }

    @Override
    public String getInstructions() {
        return "ЛКМ с шифтом - Вкл/Выкл. Просто ЛКМ - переключение между полетом и зависанием. " +
                "ПКМ - резкий прыжок вверх. Удерживайте Shift в полете для ускорения (Элитры).";
    }

    @Override
    public void load() {
        org.bukkit.configuration.file.FileConfiguration config = com.projectkorra.projectkorra.configuration.ConfigManager.getConfig();
        config.addDefault("ExtraAbilities.Yujiro.DragonsJet.Cooldown", 5000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJet.ManaTotal", 100.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJet.ManaRegenPerSecond", 5.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJet.ManaUseHoverPerSecond", 4.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJet.ManaUseLeap", 15.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJet.ManaUseSlowFlyPerSecond", 6.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJet.ManaUseFastFlyPerSecond", 10.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJet.LeapSpeed", 1.5);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJet.SlowFlySpeed", 0.6);
        config.addDefault("ExtraAbilities.Yujiro.DragonsJet.FastFlySpeed", 1.2);
        com.projectkorra.projectkorra.configuration.ConfigManager.defaultConfig.save();
    }

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