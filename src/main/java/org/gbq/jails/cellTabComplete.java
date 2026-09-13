package org.gbq.jails;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class cellTabComplete implements TabCompleter {
    private Jails plugin;

    public cellTabComplete(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player))
            return Collections.emptyList();

        if (args.length == 1) {
            return Arrays.asList("create", "delete", "rename");
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("delete") || sub.equals("rename")) {
                FileConfiguration cellsConfig = plugin.getCellsConfig();
                if (cellsConfig.contains("cells")) {
                    return new ArrayList<>(cellsConfig.getConfigurationSection("cells").getKeys(false));
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("rename")) {
            return Collections.singletonList("new_name");
        }
        return Collections.emptyList();
    }
}
