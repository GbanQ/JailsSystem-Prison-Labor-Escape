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

public class JailsTabComplete implements TabCompleter {

    private final List<String> SUBCOMMANDS = Arrays.asList(
            "reload",
            "workinfo",
            "work",
            "works",
            "escapeinfo",
            "escape",
            "rules",
            "help");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();

            if (sender.isOp() || sender.hasPermission("jails.admin")) {
                completions.add("reload");
            }

            if (sender instanceof Player) {
                completions.addAll(Arrays.asList("workinfo", "work", "works", "escapeinfo", "escape", "rules", "help"));
            }

            return filterCompletions(completions, args[0]);
        }

        return Collections.emptyList();
    }

    private List<String> filterCompletions(List<String> completions, String partial) {
        if (partial.isEmpty()) {
            return completions;
        }

        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(partial.toLowerCase())) {
                filtered.add(completion);
            }
        }
        return filtered;
    }
}