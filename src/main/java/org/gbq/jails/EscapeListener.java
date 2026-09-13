package org.gbq.jails;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EscapeListener implements Listener {
    private final Jails plugin;
    
    public EscapeListener(Jails plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.isEscaping(player)) return;
        
        EscapeManager.EscapeZone zone = plugin.getEscapeManager().getZoneAt(player.getLocation());
        if (zone != null) {
            String itemName = zone.getInteractItem() != null && zone.getInteractItem().getItemMeta() != null ? 
                    ChatColor.stripColor(zone.getInteractItem().getItemMeta().getDisplayName()) : 
                    plugin.getLang().getMessage("escape.default-item-name");
            
            String message = plugin.getLang().getMessage("escape.area-found", "interact-item", itemName);
            player.sendMessage(message);
        }
        
        plugin.getEscapeManager().checkLocationBroadcast(player);
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        
        Player player = event.getPlayer();
        
        if (!plugin.isEscaping(player)) return;
        
        EscapeManager.EscapeZone zone = plugin.getEscapeManager().getZoneAt(player.getLocation());
        if (zone == null) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        ItemStack zoneItem = zone.getInteractItem();
        if (zoneItem != null && itemsAreSimilar(item, zoneItem)) {
            event.setCancelled(true);
            performEscape(player, zone);
        }
    }
    
    private boolean itemsAreSimilar(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) return false;
        if (item1.getType() != item2.getType()) return false;
        if (!item1.hasItemMeta() || !item2.hasItemMeta()) return false;
        
        ItemStack clone1 = item1.clone();
        ItemStack clone2 = item2.clone();
        
        clone1.setAmount(1);
        clone2.setAmount(1);
        
        return clone1.isSimilar(clone2);
    }
    
    private void performEscape(Player player, EscapeManager.EscapeZone zone) {
        if (plugin.getConfig().getBoolean("escape.require-passport", true)) {
            if (!plugin.getEscapeManager().hasEscapeTicket(player)) {
                player.sendMessage(plugin.getLang().getMessage("escape.passport-missing", "swcoin", plugin.getSwcoinName()));
                return;
            }
            removeTicket(player);
        }
      
        if (plugin.getConfig().getBoolean("escape.require-full-guard-armor", true)) {
            if (!plugin.getEscapeManager().hasFullGuardArmor(player)) {
                player.sendMessage(plugin.getLang().getMessage("escape.armor-missing"));
                return;
            }
        }
        
        player.sendMessage(plugin.getLang().getMessage("escape.success"));
        
        Location dest = zone.getTeleportDestination();
        if (dest != null) {
            player.teleport(dest);
        }
        
        for (String cmd : zone.getPlayerCommands()) {
            player.performCommand(cmd.replace("{player}", player.getName()));
        }
        
        for (String cmd : zone.getConsoleCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                cmd.replace("{player}", player.getName()));
        }
        
        plugin.unjailPlayer(plugin.getServer().getConsoleSender(), player.getName(), true);
        
        String broadcast = plugin.getLang().getMessage("escape.broadcast-escape", "player", player.getName());
        plugin.getServer().broadcastMessage(broadcast);
    }
    
    private void removeTicket(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getItemChecker().isEscapeTicket(item)) {
                item.setAmount(item.getAmount() - 1);
                break;
            }
        }
    }
}