package org.gbq.jails;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class jailTabComplete implements TabCompleter {
    private Jails plugin;

    public jailTabComplete(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                List<String> playerNames = new ArrayList<>();
                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    playerNames.add(onlinePlayer.getName());
                }
                return playerNames;
            } else if (args.length == 2) {
                return Arrays.asList("15", "30", "60", "120", "240", "320");
            } else if (args.length == 3) {
                FileConfiguration cellsConfig = plugin.getCellsConfig();
                if (cellsConfig.contains("cells")) {
                    return new ArrayList<>(cellsConfig.getConfigurationSection("cells").getKeys(false));
                }
            } else if (args.length == 4) {
                return Arrays.asList("<причина>");
            }
        }
        return new ArrayList<>();
    }
}
