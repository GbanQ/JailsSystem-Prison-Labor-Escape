package org.gbq.jails;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WorkBlocksCommand implements CommandExecutor {
    private final Jails plugin;
    private final WorkBlockGUI gui;

    public WorkBlocksCommand(Jails plugin, WorkBlockGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLang().getMessage("general.command-only-player"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.isOp() && !player.hasPermission("jails.workblocks")) {
            player.sendMessage(plugin.getLang().getMessage("general.no-permission"));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getLang().getMessage("jailtools.usage"));
                return true;
            }
            try {
                WorkType type = WorkType.valueOf(args[1].toUpperCase());
                gui.giveWorkItems(player, type);
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getLang().getMessage("jailtools.invalid-type", "types", String.join(", ", WorkType.names())));
            }
            return true;
        }
        gui.openMainMenu(player);
        return true;
    }
}