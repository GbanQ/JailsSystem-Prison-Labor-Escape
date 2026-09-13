package org.gbq.jails;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class LockpickListener implements Listener {

    private final Jails plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, Boolean> lockpickingPlayers = new HashMap<>();
    private final Map<String, LockpickSession> sessions = new HashMap<>();
    private final Set<String> startingPlayers = new HashSet<>();

    private int minigameRounds;
    private double minigameBaseSpeed;
    private int minigameZoneWidthMin;
    private int minigameZoneWidthMax;
    private int minigameStartDelay;
    private double minigameSpeedMultiplier;

    public LockpickListener(Jails plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void reloadConfig() {
        loadConfig();
    }

    private void loadConfig() {
        this.minigameRounds = plugin.getConfig().getInt("lockpick.minigame.rounds", 6);
        this.minigameBaseSpeed = plugin.getConfig().getDouble("lockpick.minigame.base-speed", 2.2);
        this.minigameZoneWidthMin = plugin.getConfig().getInt("lockpick.minigame.zone-width-min", 8);
        this.minigameZoneWidthMax = plugin.getConfig().getInt("lockpick.minigame.zone-width-max", 13);
        this.minigameStartDelay = plugin.getConfig().getInt("lockpick.minigame.start-delay", 40);
        this.minigameSpeedMultiplier = plugin.getConfig().getDouble("lockpick.minigame.speed-multiplier", 1.5);
    }

    private class LockpickSession {
        Player player;
        Block doorBlock;
        Door doorData;
        int round;
        int totalRounds;
        boolean active;
        boolean directionForward;
        double currentPosition;
        double currentSpeed;
        int targetZoneCenter;
        int currentZoneWidth;
        BukkitTask task;
        ItemStack hoeInHand;
        ItemStack foundHoe;
        boolean durabilityDecreased;
        boolean completed;

        LockpickSession(Player player, Block doorBlock, Door doorData) {
            this.player = player;
            this.doorBlock = doorBlock;
            this.doorData = doorData;
            this.round = 0;
            this.totalRounds = minigameRounds;
            this.currentSpeed = minigameBaseSpeed;
            this.durabilityDecreased = false;
            this.completed = false;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;
        if (!plugin.isEnableLockpicking())
            return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
            return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.IRON_DOOR)
            return;
        ItemStack item = event.getItem();
        
        ItemChecker itemChecker = plugin.getItemChecker();
        if (!itemChecker.isLockpick(item))
            return;

        Player player = event.getPlayer();
        String playerName = player.getName();

        if (startingPlayers.contains(playerName) || sessions.containsKey(playerName)) {
            player.sendMessage(plugin.getLang().getMessage("lockpick.already-attempt"));
            return;
        }

        long cooldownMs = plugin.getLockpickCooldown() * 1000L;
        if (cooldowns.containsKey(playerName) && System.currentTimeMillis() - cooldowns.get(playerName) < cooldownMs) {
            player.sendMessage(plugin.getLang().getMessage("lockpick.cooldown", "seconds", plugin.getLockpickCooldown()));
            return;
        }

        startingPlayers.add(playerName);

        boolean hasHoeInHand = itemChecker.isLockpick(player.getInventory().getItemInMainHand());

        ItemStack foundHoe = null;
        if (!hasHoeInHand) {
            for (ItemStack i : player.getInventory().getContents()) {
                if (itemChecker.isLockpick(i)) {
                    foundHoe = i;
                    break;
                }
            }
            if (foundHoe == null) {
                player.sendMessage(plugin.getLang().getMessage("lockpick.need-lockpick"));
                startingPlayers.remove(playerName);
                return;
            }
        }

        Door door = (Door) block.getBlockData();
        LockpickSession session = new LockpickSession(player, block, door);
        session.hoeInHand = hasHoeInHand ? player.getInventory().getItemInMainHand() : null;
        session.foundHoe = foundHoe;

        cooldowns.put(playerName, System.currentTimeMillis());
        lockpickingPlayers.put(playerName, true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 100, false, false));

        sessions.put(playerName, session);
        startingPlayers.remove(playerName);

        player.sendTitle(plugin.getLang().getMessage("lockpick.start-title"), 
                         plugin.getLang().getMessage("lockpick.start-subtitle"), 10, 70, 20);
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 0.5f, 0.5f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (sessions.containsKey(playerName)) {
                startNextRound(session);
            }
        }, minigameStartDelay);
    }

    private void startNextRound(LockpickSession session) {
        if (session.round >= session.totalRounds) {
            complete(session, true);
            return;
        }

        session.round++;
        session.active = true;
        session.currentPosition = 50.0;
        session.directionForward = true;
        session.currentSpeed = minigameBaseSpeed * Math.pow(minigameSpeedMultiplier, session.round - 1);

        Random rand = new Random();
        session.currentZoneWidth = minigameZoneWidthMin + rand.nextInt(minigameZoneWidthMax - minigameZoneWidthMin + 1);
        int maxCenter = 100 - session.currentZoneWidth / 2;
        int minCenter = session.currentZoneWidth / 2;
        session.targetZoneCenter = minCenter + rand.nextInt(maxCenter - minCenter + 1);

        session.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!session.active)
                return;

            if (session.directionForward) {
                session.currentPosition += session.currentSpeed;
                if (session.currentPosition >= 100) {
                    session.currentPosition = 100;
                    session.directionForward = false;
                }
            } else {
                session.currentPosition -= session.currentSpeed;
                if (session.currentPosition <= 0) {
                    session.currentPosition = 0;
                    session.directionForward = true;
                }
            }

            drawProgressBar(session);
        }, 0L, 1L);
    }

    private void drawProgressBar(LockpickSession session) {
        int pos = (int) Math.round(session.currentPosition);
        int center = session.targetZoneCenter;
        int halfWidth = session.currentZoneWidth / 2;
        int low = Math.max(0, center - halfWidth);
        int high = Math.min(100, center + halfWidth);

        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i <= 100; i += 2) {
            if (i == pos) {
                bar.append("§e|");
            } else if (i >= low && i <= high) {
                bar.append("§a|");
            } else {
                bar.append("§7|");
            }
        }
        bar.append("§8]  §7Штифт §e").append(session.round).append("§7/").append(session.totalRounds);
        session.player.sendActionBar(bar.toString());
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking())
            return;
        Player player = event.getPlayer();
        LockpickSession session = sessions.get(player.getName());
        if (session == null || !session.active || session.completed)
            return;

        session.active = false;
        if (session.task != null) {
            session.task.cancel();
        }

        int pos = (int) Math.round(session.currentPosition);
        int center = session.targetZoneCenter;
        int halfWidth = session.currentZoneWidth / 2;
        int low = center - halfWidth;
        int high = center + halfWidth;

        if (pos >= low && pos <= high) {
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.8f, 1.2f);

            if (session.round < session.totalRounds) {
                player.sendTitle(plugin.getLang().getMessage("lockpick.round-success-title"), 
                                 plugin.getLang().getMessage("lockpick.round-success-subtitle"), 10, 30, 10);
                startNextRound(session);
            } else {
                complete(session, true);
            }
        } else {
            player.sendTitle(plugin.getLang().getMessage("lockpick.round-fail-title"), 
                             plugin.getLang().getMessage("lockpick.round-fail-subtitle"), 10, 40, 10);
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1, 1);
            complete(session, false);
        }
    }

    private void complete(LockpickSession session, boolean success) {
        if (session.completed)
            return;
        session.completed = true;

        if (!sessions.containsKey(session.player.getName()))
            return;

        Player player = session.player;
        String playerName = player.getName();

        if (!session.durabilityDecreased) {
            decreaseDurability(session);
            session.durabilityDecreased = true;
        }

        sessions.remove(playerName);
        lockpickingPlayers.remove(playerName);
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        if (success) {
            session.doorData.setOpen(true);
            session.doorBlock.setBlockData(session.doorData);
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().playSound(loc, Sound.BLOCK_IRON_DOOR_OPEN, 1.0f, 1.2f);
            player.sendTitle(plugin.getLang().getMessage("lockpick.complete-title"), 
                             plugin.getLang().getMessage("lockpick.complete-subtitle"), 10, 70, 20);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (session.doorBlock.getType() == Material.IRON_DOOR) {
                    session.doorData.setOpen(false);
                    session.doorBlock.setBlockData(session.doorData);
                    session.doorBlock.getWorld().playSound(session.doorBlock.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE,
                            1.0f, 1.0f);
                }
            }, plugin.getLockpickDoorOpenTime());
        } else {
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.2f);
        }
    }

    private void decreaseDurability(LockpickSession session) {
        Player player = session.player;
        ItemStack hoeToDamage = session.hoeInHand != null ? session.hoeInHand : session.foundHoe;
        if (hoeToDamage == null)
            return;

        int maxDura = hoeToDamage.getType().getMaxDurability();
        int loss = (int) (maxDura * (plugin.getLockpickDurabilityLoss() / 100.0));
        int newDura = hoeToDamage.getDurability() + loss;
        
        if (newDura >= maxDura) {
            if (session.hoeInHand != null) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().remove(hoeToDamage);
            }
            player.sendMessage(plugin.getLang().getMessage("lockpick.break-message"));
        } else {
            hoeToDamage.setDurability((short) newDura);
            player.sendMessage(plugin.getLang().getMessage("lockpick.durability-warning"));
        }
    }

    // ==================== ЗАПРЕТ ДЕЙСТВИЙ ВО ВРЕМЯ ВЗЛОМА ====================
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (lockpickingPlayers.getOrDefault(player.getName(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (lockpickingPlayers.getOrDefault(player.getName(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (lockpickingPlayers.getOrDefault(player.getName(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (lockpickingPlayers.getOrDefault(player.getName(), false)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to == null)
                return;

            if (Math.abs(from.getYaw() - to.getYaw()) > 0.001 ||
                    Math.abs(from.getPitch() - to.getPitch()) > 0.001) {

                Location newTo = to.clone();
                newTo.setYaw(from.getYaw());
                newTo.setPitch(from.getPitch());
                event.setTo(newTo);
            }
            
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                player.teleport(from);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        cleanupPlayer(event.getPlayer().getName());
    }

    private void cleanupPlayer(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }

        LockpickSession session = sessions.remove(playerName);
        if (session != null && session.task != null) {
            session.task.cancel();
        }
        lockpickingPlayers.remove(playerName);
        startingPlayers.remove(playerName);
        cooldowns.remove(playerName);
    }

    public void cleanupAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            cleanupPlayer(player.getName());
        }
    }
}