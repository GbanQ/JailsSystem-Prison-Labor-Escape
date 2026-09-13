package org.gbq.jails;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class JailBoardManager implements Listener {
    private final Jails plugin;
    private final List<Board> boards = new ArrayList<>();
    private BukkitRunnable updateTask;

    private final Map<UUID, List<Location>> selectedSigns = new HashMap<>();
    private final Set<UUID> selectionMode = new HashSet<>();

    public JailBoardManager(Jails plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        load();
        startAutoUpdate();
    }

    private void startAutoUpdate() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllBoards();
            }
        };
        updateTask.runTaskTimer(plugin, 1200L, 1200L);
    }

    public void stopAutoUpdate() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }

    public void load() {
        boards.clear();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection sec = config.getConfigurationSection("boards");
        if (sec == null)
            return;
        for (String key : sec.getKeys(false)) {
            Board board = new Board();
            for (String locStr : sec.getStringList(key)) {
                String[] parts = locStr.split(";");
                if (parts.length == 4) {
                    try {
                        Location loc = new Location(
                                plugin.getServer().getWorld(parts[0]),
                                Double.parseDouble(parts[1]),
                                Double.parseDouble(parts[2]),
                                Double.parseDouble(parts[3]));
                        board.addLocation(loc);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Неверная локация в конфиге boards: " + locStr);
                    }
                }
            }
            if (!board.getLocations().isEmpty()) {
                boards.add(board);
            }
        }
    }

    public void save() {
        FileConfiguration config = plugin.getConfig();
        config.set("boards", null);
        int i = 0;
        for (Board board : boards) {
            List<String> list = new ArrayList<>();
            for (Location loc : board.getLocations()) {
                list.add(loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ());
            }
            config.set("boards.board" + i, list);
            i++;
        }
        plugin.saveConfig();
    }

    public void enableSelectionMode(Player player) {
        selectionMode.add(player.getUniqueId());
        selectedSigns.remove(player.getUniqueId());
        player.sendMessage(plugin.getLang().getMessage("jailboard.selection-enabled"));
        player.sendMessage(plugin.getLang().getMessage("jailboard.selection-instruction1"));
        player.sendMessage(plugin.getLang().getMessage("jailboard.selection-instruction2"));
        player.sendMessage(plugin.getLang().getMessage("jailboard.selection-cancel"));
        player.getInventory().addItem(plugin.getBoardConfigurator());
    }

    public boolean toggleSelection(Player player, Location loc) {
        if (!selectionMode.contains(player.getUniqueId()))
            return false;

        UUID playerId = player.getUniqueId();
        List<Location> selections = selectedSigns.computeIfAbsent(playerId, k -> new ArrayList<>());

        if (selections.contains(loc)) {
            selections.remove(loc);
            player.sendMessage(plugin.getLang().getMessage("jailboard.sign-removed", "count", selections.size()));
            player.spawnParticle(Particle.SMOKE, loc.clone().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0);
        } else {
            selections.add(loc);
            player.sendMessage(plugin.getLang().getMessage("jailboard.sign-added", "count", selections.size()));
            player.spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0);
        }
        return true;
    }

    public boolean confirmBoard(Player player) {
        if (!selectionMode.contains(player.getUniqueId())) {
            player.sendMessage(plugin.getLang().getMessage("jailboard.need-create-first"));
            return false;
        }

        UUID playerId = player.getUniqueId();
        List<Location> selections = selectedSigns.get(playerId);

        if (selections == null || selections.isEmpty()) {
            player.sendMessage(plugin.getLang().getMessage("jailboard.confirm-no-selection"));
            return false;
        }

        org.bukkit.World world = null;
        for (Location loc : selections) {
            if (world == null)
                world = loc.getWorld();
            if (!loc.getWorld().equals(world)) {
                player.sendMessage(plugin.getLang().getMessage("jailboard.confirm-different-worlds"));
                return false;
            }
        }

        for (Location loc : selections) {
            removeBoard(loc);
        }

        Board board = new Board();
        board.addAllLocations(selections);
        boards.add(board);
        save();
        updateBoard(board);

        int size = selections.size();
        selections.clear();
        selectionMode.remove(playerId);

        removeConfiguratorItem(player);

        player.sendMessage(plugin.getLang().getMessage("jailboard.confirm-success", "count", size));
        return true;
    }

    private void removeConfiguratorItem(Player player) {
        ItemStack configurator = plugin.getBoardConfigurator();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(configurator)) {
                item.setAmount(item.getAmount() - 1);
                break;
            }
        }
    }

    public void cancelBoard(Player player) {
        UUID playerId = player.getUniqueId();
        selectionMode.remove(playerId);
        selectedSigns.remove(playerId);
        removeConfiguratorItem(player);
        player.sendMessage(plugin.getLang().getMessage("jailboard.cancel"));
    }

    public void clearSelection(Player player) {
        selectedSigns.remove(player.getUniqueId());
        player.sendMessage(plugin.getLang().getMessage("jailboard.clear"));
    }

    public void updateAllBoards() {
        for (Board board : boards) {
            updateBoard(board);
        }
    }

    private void updateBoard(Board board) {
        List<Location> locs = board.getLocations();
        if (locs.isEmpty())
            return;

        Map<Double, List<Location>> rowsMap = new LinkedHashMap<>();
        for (Location loc : locs) {
            rowsMap.computeIfAbsent(loc.getY(), k -> new ArrayList<>()).add(loc);
        }

        List<Double> sortedYs = new ArrayList<>(rowsMap.keySet());
        sortedYs.sort(Collections.reverseOrder());
        List<List<Location>> rows = new ArrayList<>();
        for (double y : sortedYs) {
            rows.add(rowsMap.get(y));
        }

        if (rows.isEmpty())
            return;

        List<Location> headerRow = rows.get(0);
        int headerSize = headerRow.size();

        for (Location loc : headerRow) {
            setSignText(loc, "", "", "", "");
        }

        if (headerSize > 0) {
            int centerIdx = headerSize / 2;
            if (headerSize % 2 == 0 && headerSize > 0) {
                centerIdx--;
            }

            Map<String, JailData> jailed = plugin.getJailedPlayers();
            int totalCount = jailed.size();
            int onlineCount = 0;
            int soonCount = 0;

            for (Map.Entry<String, JailData> entry : jailed.entrySet()) {
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline())
                    onlineCount++;
                if (entry.getValue().getTime() <= 3600)
                    soonCount++;
            }

            int offlineCount = totalCount - onlineCount;

            setSignText(headerRow.get(centerIdx),
                    "§0§lСПИСОК",
                    "§0§lЗАКЛЮЧЕННЫХ",
                    "§7Всего: §e" + totalCount + " §7чел",
                    "§7Онлайн: §a" + onlineCount + " §7| §c" + offlineCount + "офф");
        }

        Map<String, JailData> jailed = plugin.getJailedPlayers();
        List<Map.Entry<String, JailData>> prisoners = new ArrayList<>(jailed.entrySet());

        int prisonerIndex = 0;

        for (int r = 1; r < rows.size(); r++) {
            List<Location> row = rows.get(r);
            for (Location loc : row) {
                String[] lines = new String[4];
                Arrays.fill(lines, "");

                for (int slot = 0; slot < 2; slot++) {
                    if (prisonerIndex < prisoners.size()) {
                        Map.Entry<String, JailData> entry = prisoners.get(prisonerIndex);
                        String name = entry.getKey();
                        int timeSeconds = entry.getValue().getTime();
                        int minutes = (int) Math.ceil(timeSeconds / 60.0);

                        Player player = plugin.getServer().getPlayer(name);
                        boolean isOnline = player != null && player.isOnline();
                        String onlineStatus = isOnline ? "§a●" : "§7○";

                        String displayName = name.length() > 9 ? name.substring(0, 7) + ".." : name;

                        lines[slot * 2] = "§e" + (prisonerIndex + 1) + " §f" + displayName + " " + onlineStatus;
                        lines[slot * 2 + 1] = "§7Осталось: §c" + minutes + "м";

                        prisonerIndex++;
                    }
                }

                setSignText(loc, lines[0], lines[1], lines[2], lines[3]);
            }
        }
    }

    private void setSignText(Location loc, String line1, String line2, String line3, String line4) {
        Block block = loc.getBlock();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            sign.setLine(0, line1);
            sign.setLine(1, line2);
            sign.setLine(2, line3);
            sign.setLine(3, line4);
            sign.update();
        }
    }

    public void removeBoard(Location loc) {
        Board boardToRemove = null;
        for (Board board : boards) {
            if (board.contains(loc)) {
                boardToRemove = board;
                break;
            }
        }

        if (boardToRemove != null) {
            for (Location boardLoc : boardToRemove.getLocations()) {
                setSignText(boardLoc, "", "", "", "");
            }
            boards.remove(boardToRemove);
            save();
        }
    }

    public boolean isPartOfBoard(Location loc) {
        for (Board board : boards) {
            if (board.contains(loc))
                return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
                event.getClickedBlock() != null &&
                event.getClickedBlock().getState() instanceof Sign) {
            Location loc = event.getClickedBlock().getLocation();
            if (isPartOfBoard(loc)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (isPartOfBoard(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getLang().getMessage("jailboard.cannot-edit-board-sign"));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getState() instanceof Sign) {
            Location loc = event.getBlock().getLocation();
            if (isPartOfBoard(loc)) {
                removeBoard(loc);
            }
        }
    }

    private static class Board {
        private final List<Location> locations = new ArrayList<>();

        public void addLocation(Location loc) {
            locations.add(loc);
        }

        public void addAllLocations(Collection<Location> locs) {
            locations.addAll(locs);
        }

        public List<Location> getLocations() {
            return locations;
        }

        public boolean contains(Location loc) {
            return locations.contains(loc);
        }
    }
}