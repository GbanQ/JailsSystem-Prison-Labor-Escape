package org.gbq.jails;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JailToolsTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        
        if (args.length == 1) {
            return Arrays.asList("give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("BRICKS", "LAUNDRY", "KITCHEN", "LIBRARY", "TRADER");
        }
        return Collections.emptyList();
    }
}