package me.yujiro.dragon.abilities;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
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

public class DragonsBolt extends FireAbility implements AddonAbility {

    private long cooldown;
    private double damage;
    private double craterRadius;
    private long revertTime;
    private long duration;
    private long startTime;
    private Particle.DustOptions lightningColor;

    public DragonsBolt(Player player) {
        super(player);
        if (player == null) return;

        if (!bPlayer.canBend(this) || CoreAbility.hasAbility(player, this.getClass()) || bPlayer.isOnCooldown(this) || GeneralMethods.isRegionProtectedFromBuild(this, player.getLocation())) {
            return;
        }

        setFields();
        start();
        bPlayer.addCooldown(this);
    }

    public void setFields() {
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsBolt.Cooldown", 10000);
        this.damage = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsBolt.Damage", 8.0);
        this.craterRadius = ConfigManager.getConfig().getDouble("ExtraAbilities.Yujiro.DragonsBolt.CraterRadius", 4.0);
        this.revertTime = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsBolt.RevertTime", 7000);
        this.duration = ConfigManager.getConfig().getLong("ExtraAbilities.Yujiro.DragonsBolt.Duration", 4000);

        this.startTime = System.currentTimeMillis();
        this.lightningColor = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.2f);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 2.0f);
    }

    @Override
    public long getCooldown() { return cooldown; }

    @Override
    public Location getLocation() { return player.getLocation(); }

    @Override
    public String getName() { return "DragonsBolt"; }

    @Override
    public boolean isHarmlessAbility() { return false; }

    @Override
    public boolean isSneakAbility() { return false; }

    @Override
    public Element getElement() { return DragonElement.DRAGON_FIRE; }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || bPlayer.isChiBlocked() || GeneralMethods.isRegionProtectedFromBuild(this, player.getLocation())){
            this.remove();
            return;
        }

        if (System.currentTimeMillis() > startTime + duration) {
            this.remove();
            return;
        }

        Location center = player.getLocation().add(0, 1, 0);
        for (int i = 0; i < 6; i++) {
            double theta = Math.random() * 2 * Math.PI;
            double phi = Math.random() * Math.PI;
            double r = 1.2;

            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.sin(phi) * Math.sin(theta);
            double z = r * Math.cos(phi);

            Location pLoc = center.clone().add(x, y, z);
            ParticleEffect.SOUL_FIRE_FLAME.display(pLoc, 1, 0, 0, 0, 0);

            if (Math.random() < 0.3) {
                ParticleEffect.REDSTONE.display(pLoc, 1, 0, 0, 0, 0, lightningColor);
            }
        }

        if (Math.random() < 0.1) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 0.5f, 1.5f);
        }
    }

    public void triggerCounter(Entity attacker) {
        Location targetLoc = attacker.getLocation();

        targetLoc.getWorld().strikeLightningEffect(targetLoc);

        Location start = player.getEyeLocation();
        Location end = targetLoc.clone().add(0, 1, 0);
        Vector dir = end.toVector().subtract(start.toVector());
        double distance = dir.length();
        dir.normalize();

        for (double d = 0; d < distance; d += 0.5) {
            Location beamLoc = start.clone().add(dir.clone().multiply(d));
            ParticleEffect.REDSTONE.display(beamLoc, 3, 0.2, 0.2, 0.2, 0, lightningColor);
            ParticleEffect.SOUL_FIRE_FLAME.display(beamLoc, 2, 0.1, 0.1, 0.1, 0);
        }

        ParticleEffect.EXPLOSION_HUGE.display(targetLoc, 2, 0.5, 0.5, 0.5, 0);

        DamageHandler.damageEntity(attacker, damage, this);
        attacker.setFireTicks(100);

        Vector knockback = end.toVector().subtract(player.getLocation().toVector()).normalize().multiply(2.0).setY(0.8);
        attacker.setVelocity(knockback);

        for (Block block : GeneralMethods.getBlocksAroundPoint(targetLoc, craterRadius)) {
            if (GeneralMethods.isSolid(block) && !TempBlock.isTempBlock(block) && !GeneralMethods.isRegionProtectedFromBuild(this, block.getLocation())) {
                if (block.getType() != Material.BEDROCK && block.getType() != Material.OBSIDIAN) {
                    TempBlock tb = new TempBlock(block, Material.AIR.createBlockData());
                    tb.setRevertTime(revertTime);
                }
            }
        }

        this.remove();
    }

    @Override
    public void remove() { super.remove(); }

    @Override
    public String getDescription() {
        return "Создайте Громовой Щит. Если вы получите урон от оружия или магии, обидчика настигнет разряд молнии.";
    }

    @Override
    public String getInstructions() {
        return "Нажмите Shift один раз для активации ауры. Любая атака по вам вызовет разрушительный контрудар.";
    }

    @Override
    public void load() {

        com.projectkorra.projectkorra.ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new me.yujiro.dragon.listeners.AbilityListener(), com.projectkorra.projectkorra.ProjectKorra.plugin);


        org.bukkit.configuration.file.FileConfiguration config = com.projectkorra.projectkorra.configuration.ConfigManager.getConfig();


        config.addDefault("ExtraAbilities.Yujiro.DragonsBolt.Cooldown", 10000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsBolt.Damage", 8.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsBolt.CraterRadius", 4.0);
        config.addDefault("ExtraAbilities.Yujiro.DragonsBolt.RevertTime", 7000);
        config.addDefault("ExtraAbilities.Yujiro.DragonsBolt.Duration", 4000);

        com.projectkorra.projectkorra.configuration.ConfigManager.defaultConfig.save();

        com.projectkorra.projectkorra.ProjectKorra.log.info("Аддон Dragon 2.0 успешно загружен! Конфиги сгенерированы.");
    }

    @Override
    public void stop() {}

    @Override
    public String getAuthor() { return "__Yujiro"; }

    @Override
    public String getVersion() { return "2.0 (FORK)"; }
}