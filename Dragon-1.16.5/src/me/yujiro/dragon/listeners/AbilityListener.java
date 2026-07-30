package me.yujiro.dragon.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;

import me.yujiro.dragon.abilities.DragonsBolt;
import me.yujiro.dragon.abilities.DragonsBreath;
import me.yujiro.dragon.abilities.DragonsComet;
import me.yujiro.dragon.abilities.DragonsJet;
import me.yujiro.dragon.abilities.DragonsScales;
import me.yujiro.dragon.abilities.DragonsJudgment;
import me.yujiro.dragon.abilities.DragonsCanvas;
import me.yujiro.dragon.abilities.DragonsRails;

public class AbilityListener implements Listener {

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (event.isCancelled() || bPlayer == null) return;

        String bound = bPlayer.getBoundAbilityName();
        if (bound == null || bound.isEmpty()) return;

        if (bound.equalsIgnoreCase("DragonsJet")) {
            if (!CoreAbility.hasAbility(player, DragonsJet.class)) {
                new DragonsJet(player);
            } else {
                CoreAbility.getAbility(player, DragonsJet.class).onLeftClick();
            }
        } else if (bound.equalsIgnoreCase("DragonsBreath")) {
            if (CoreAbility.hasAbility(player, DragonsBreath.class)){
                CoreAbility.getAbility(player, DragonsBreath.class).onClick();
            }
        } else if (bound.equalsIgnoreCase("DragonsComet")) {
            if (CoreAbility.hasAbility(player, DragonsComet.class)){
                CoreAbility.getAbility(player, DragonsComet.class).onClick();
            }
        } else if (bound.equalsIgnoreCase("DragonsScales")) {
            if (CoreAbility.hasAbility(player, DragonsScales.class)){
                CoreAbility.getAbility(player, DragonsScales.class).onClick();
            } else {
                new DragonsScales(player);
            }
        } else if (bound.equalsIgnoreCase("DragonsJudgment")) {
            if (CoreAbility.hasAbility(player, DragonsJudgment.class)){
                CoreAbility.getAbility(player, DragonsJudgment.class).onClick();
            }
        } else if (bound.equalsIgnoreCase("DragonsCanvas")) {
            if (CoreAbility.hasAbility(player, DragonsCanvas.class)){
                CoreAbility.getAbility(player, DragonsCanvas.class).onClick();
            }
        } else if (bound.equalsIgnoreCase("DragonsRails")) {
            if (CoreAbility.hasAbility(player, DragonsRails.class)){
                CoreAbility.getAbility(player, DragonsRails.class).onClick();
            } else {
                new DragonsRails(player);
            }
        }
    }

    @EventHandler
    public void onShift(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (event.isCancelled() || bPlayer == null) return;

        String bound = bPlayer.getBoundAbilityName();
        if (bound == null || bound.isEmpty()) return;

        if (bound.equalsIgnoreCase("DragonsBreath")) {
            new DragonsBreath(player);
        } else if (bound.equalsIgnoreCase("DragonsComet")) {
            new DragonsComet(player);
        } else if (bound.equalsIgnoreCase("DragonsScales")) {
            if (CoreAbility.hasAbility(player, DragonsScales.class)){
                CoreAbility.getAbility(player, DragonsScales.class).onShift();
            }
        } else if (bound.equalsIgnoreCase("DragonsBolt")) {
            new DragonsBolt(player);
        } else if (bound.equalsIgnoreCase("DragonsJudgment")) {
            new DragonsJudgment(player);
        } else if (bound.equalsIgnoreCase("DragonsCanvas")) {
            new DragonsCanvas(player);
        } else if (bound.equalsIgnoreCase("DragonsRails")) {
            if (CoreAbility.hasAbility(player, DragonsRails.class)){
                CoreAbility.getAbility(player, DragonsRails.class).onShift();
            }
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player defender = (Player) event.getEntity();
            if (CoreAbility.hasAbility(defender, DragonsBolt.class)) {
                DragonsBolt ability = CoreAbility.getAbility(defender, DragonsBolt.class);
                event.setCancelled(true);
                ability.triggerCounter(event.getDamager());
            }
        }
    }

    @EventHandler
    public void onAbilityDamage(AbilityDamageEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player defender = (Player) event.getEntity();
            if (CoreAbility.hasAbility(defender, DragonsBolt.class)) {
                DragonsBolt ability = CoreAbility.getAbility(defender, DragonsBolt.class);
                event.setCancelled(true);
                Player attacker = event.getAbility().getPlayer();
                if (attacker != null) {
                    ability.triggerCounter(attacker);
                }
            }
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (event.isCancelled() || bPlayer == null) return;

        String bound = bPlayer.getBoundAbilityName();
        if (bound == null || bound.isEmpty()) return;

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR)){
            if (bound.equalsIgnoreCase("DragonsJet") && CoreAbility.hasAbility(player, DragonsJet.class)) {
                CoreAbility.getAbility(player, DragonsJet.class).onRightClick();
            }
        }
    }

    @EventHandler
    public void onGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (CoreAbility.hasAbility(player, DragonsJet.class)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGlideIntoWall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!event.getCause().equals(DamageCause.FLY_INTO_WALL)) return;

        Player player = (Player) event.getEntity();
        if (CoreAbility.hasAbility(player, DragonsJet.class)) {
            event.setCancelled(true);
        }
    }
}