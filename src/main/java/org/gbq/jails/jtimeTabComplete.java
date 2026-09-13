package org.gbq.jails;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class jtimeTabComplete implements TabCompleter {

    private final Jails plugin;
    private final List<String> SUBCOMMANDS = Arrays.asList("add", "remove", "set");

    public jtimeTabComplete(Jails plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (!sender.isOp() && !sender.hasPermission("jails.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterSuggestions(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (SUBCOMMANDS.contains(subCommand)) {
                List<String> playerNames = new ArrayList<>(plugin.getJailedPlayers().keySet());
                return filterSuggestions(playerNames, args[1]);
            }
            return Collections.emptyList();
        }

        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (SUBCOMMANDS.contains(subCommand)) {
                return Collections.singletonList("<секунды>");
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterSuggestions(List<String> suggestions, String input) {
        if (suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        if (input.isEmpty()) {
            return suggestions;
        }

        List<String> filtered = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }
}