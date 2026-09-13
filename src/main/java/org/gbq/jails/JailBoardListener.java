package org.gbq.jails;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class JailBoardListener implements Listener {
    private final Jails plugin;

    public JailBoardListener(Jails plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign))
            return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta())
            return;
        if (!item.getItemMeta().hasDisplayName() ||
                !item.getItemMeta().getDisplayName().equals(plugin.getLang().getMessage("items.configurator-stick")))
            return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        boolean toggled = plugin.getBoardManager().toggleSelection(player, block.getLocation());

        if (!toggled) {
            player.sendMessage(plugin.getLang().getMessage("jailboard.need-create-first"));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getBoardManager().cancelBoard(event.getPlayer());
    }
}