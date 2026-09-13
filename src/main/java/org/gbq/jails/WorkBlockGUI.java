package org.gbq.jails;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class WorkBlockGUI implements Listener {
    private final Jails plugin;
    private final Map<UUID, Block> pendingPlace = new HashMap<>();

    public WorkBlockGUI(Jails plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, plugin.getLang().getMessage("work-gui.title"));

        ItemStack panel = createPanel(Material.BLACK_STAINED_GLASS_PANE, "§r");

        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, panel);
            }
        }

        inv.setItem(4, createInfoItem());
        inv.setItem(10, createTypeIcon(WorkType.BRICKS));
        inv.setItem(12, createTypeIcon(WorkType.LAUNDRY));
        inv.setItem(14, createTypeIcon(WorkType.KITCHEN));
        inv.setItem(16, createTypeIcon(WorkType.LIBRARY));
        inv.setItem(22, createPanel(Material.RED_STAINED_GLASS_PANE, plugin.getLang().getMessage("work-gui.close-button")));
        
        inv.setItem(13, createTypeIcon(WorkType.TRADER));

        player.openInventory(inv);
    }

    private ItemStack createPanel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getLang().getMessage("work-gui.info-item.name"));
        meta.setLore(Arrays.asList(
                plugin.getLang().getMessage("work-gui.info-item.lore.0"),
                plugin.getLang().getMessage("work-gui.info-item.lore.1", "brick_seconds", plugin.getBrickTimeReduction()),
                plugin.getLang().getMessage("work-gui.info-item.lore.2", "laundry_seconds", plugin.getLaundryTimeReduction()),
                plugin.getLang().getMessage("work-gui.info-item.lore.3", "kitchen_seconds", plugin.getKitchenTimeReduction()),
                plugin.getLang().getMessage("work-gui.info-item.lore.4"),
                plugin.getLang().getMessage("work-gui.info-item.lore.5"),
                plugin.getLang().getMessage("work-gui.info-item.lore.6"),
                plugin.getLang().getMessage("work-gui.info-item.lore.7"),
                plugin.getLang().getMessage("work-gui.info-item.lore.8")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTypeIcon(WorkType type) {
        ItemStack item = type.getIcon().clone();
        ItemMeta meta = item.getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add("§7━━━━━━━━━━━━━━━━━━");

        if (type.isLibrary()) {
            ConfigurationSection categories = plugin.getConfig().getConfigurationSection("library.categories");
            if (categories != null && !categories.getKeys(false).isEmpty()) {
                lore.add("§fКатегории книг:");
                for (String key : categories.getKeys(false)) {
                    String catName = categories.getString(key + ".name");
                    if (catName != null) {
                        lore.add("§7▪ §f" + catName);
                    }
                }
            } else {
                lore.add("§fКатегории не настроены");
            }
        } else if (type.isTrader()) {
            lore.add("§fПредмет: §6" + ChatColor.stripColor(plugin.getLang().getMessage("items.trader-spawn-egg")));
            lore.add("§7Используйте ПКМ по блоку");
            lore.add("§7на территории чёрного рынка");
            lore.add("§7чтобы призвать NPC-торговца.");
            lore.add("§7Торговец продаёт билеты на паром за SWcoin");
        } else {
            for (WorkBlock wb : type.getBlocks()) {
                String name = ChatColor.stripColor(wb.getCustomName());
                lore.add("§f▪ §7" + name);
            }
        }

        lore.add("§7━━━━━━━━━━━━━━━━━━");
        lore.add(plugin.getLang().getMessage("work-gui.info-item.lore.click-to-get"));

        meta.setLore(lore);

        if (type == WorkType.BRICKS) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    public void giveWorkItems(Player player, WorkType type) {
        player.closeInventory();

        player.sendMessage(plugin.getLang().getMessage("jailtools.gave-items", "type", type.getDisplayName()));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

        if (type.isLibrary()) {
            giveLibraryItems(player);
        } else if (type.isTrader()) {
            giveTraderItems(player);
        } else {
            for (WorkBlock wb : type.getBlocks()) {
                ItemStack item = new ItemStack(wb.getMaterial());
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(wb.getCustomName());
                meta.setLore(Arrays.asList(
                        "§7━━━━━━━━━━━━━━━━━━",
                        "§fРабочий блок: §e" + type.getDisplayName(),
                        "§7Поставьте в нужном месте",
                        "§7и он автоматически настроится",
                        "§7━━━━━━━━━━━━━━━━━━"));
                item.setItemMeta(meta);
                player.getInventory().addItem(item);
            }
        }
    }
    
    private void giveTraderItems(Player player) {
        ItemStack egg = plugin.getItemChecker().createTraderSpawnEgg();
        if (egg != null) {
            player.getInventory().addItem(egg);
            player.sendMessage(plugin.getLang().getMessage("jailtools.trader-egg-given"));
            player.sendMessage(plugin.getLang().getMessage("jailtools.trader-egg-help"));
        } else {
            player.sendMessage(plugin.getLang().getMessage("jailtools.trader-egg-fail"));
        }
    }

    private void giveLibraryItems(Player player) {
        giveWorkBlockItem(player, Material.BARREL, plugin.getLang().getMessage("work-gui.library.book-dispenser"));

        ConfigurationSection categories = plugin.getConfig().getConfigurationSection("library.categories");
        if (categories != null) {
            for (String key : categories.getKeys(false)) {
                String categoryName = categories.getString(key + ".name");
                if (categoryName != null) {
                    giveWorkBlockItem(player, Material.CHISELED_BOOKSHELF, plugin.getLang().getMessage("work-gui.library.bookshelf-prefix") + categoryName);
                }
            }
        } else {
            player.sendMessage(plugin.getLang().getMessage("jailtools.library-error"));
        }

        giveWorkBlockItem(player, Material.CARTOGRAPHY_TABLE, plugin.getLang().getMessage("work-gui.library.cartography-table"));
    }

    private void giveWorkBlockItem(Player player, Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(
                "§7━━━━━━━━━━━━━━━━━━",
                "§fРабочий блок: §e" + plugin.getLang().getMessage("work-gui.work-type.library"),
                "§7Поставьте в нужном месте",
                "§7и он автоматически настроится",
                "§7━━━━━━━━━━━━━━━━━━"));
        item.setItemMeta(meta);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage(plugin.getLang().getMessage("work.general.inventory-full-drop"));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getLang().getMessage("work-gui.title")))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        ItemStack current = event.getCurrentItem();

        if (current == null || !current.hasItemMeta())
            return;

        if (current.getType() == Material.RED_STAINED_GLASS_PANE) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            return;
        }

        for (WorkType type : WorkType.values()) {
            if (current.getType() == type.getIcon().getType()) {
                giveWorkItems(player, type);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
            return;

        String displayName = item.getItemMeta().getDisplayName();
        boolean isWorkBlock = false;
        WorkType foundType = null;

        outer: for (WorkType type : WorkType.values()) {
            if (type.isLibrary()) {
                if (displayName.startsWith(plugin.getLang().getMessage("work-gui.library.book-dispenser")) ||
                        displayName.startsWith(plugin.getLang().getMessage("work-gui.library.bookshelf-prefix")) ||
                        displayName.startsWith(plugin.getLang().getMessage("work-gui.library.cartography-table"))) {
                    isWorkBlock = true;
                    foundType = type;
                    break outer;
                }
            } else if (type.isTrader()) {
                continue;
            } else {
                for (WorkBlock wb : type.getBlocks()) {
                    if (wb.getCustomName().equals(displayName)) {
                        isWorkBlock = true;
                        foundType = type;
                        break outer;
                    }
                }
            }
        }
        if (!isWorkBlock || foundType == null)
            return;

        Block block = event.getBlockPlaced();

        final WorkType finalFoundType = foundType;
        final Material blockType = item.getType();
        final String finalDisplayName = displayName;

        pendingPlace.put(player.getUniqueId(), block);

        player.playSound(block.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                Block b = pendingPlace.remove(player.getUniqueId());
                if (b == null)
                    return;
                if (!b.getType().equals(blockType))
                    return;

                if (b.getState() instanceof Container) {
                    Container container = (Container) b.getState();
                    container.setCustomName(finalDisplayName);
                    container.update();
                }

                if (finalDisplayName.startsWith(plugin.getLang().getMessage("work-gui.library.bookshelf-prefix"))) {
                    String cat = finalDisplayName.substring(plugin.getLang().getMessage("work-gui.library.bookshelf-prefix").length()).trim();
                    plugin.getLogger().info("Добавляем полку категории '" + cat + "' по координатам " + b.getLocation());
                    if (plugin.getJailWorkListener() != null) {
                        plugin.getJailWorkListener().addShelf(cat, b.getLocation());
                    } else {
                        plugin.getLogger().warning("JailWorkListener не инициализирован!");
                    }
                }

                boolean shouldCreateStand = true;

                if (finalDisplayName.contains(plugin.getLang().getMessage("work-gui.laundry.washing-machine")) ||
                        finalDisplayName.contains(plugin.getLang().getMessage("work-gui.laundry.washing-machine-alt")) ||
                        (finalFoundType == WorkType.LAUNDRY && finalDisplayName.contains(plugin.getLang().getMessage("work-gui.laundry.washing-machine"))) ||
                        finalDisplayName.startsWith(plugin.getLang().getMessage("work-gui.library.bookshelf-prefix")) ||
                        finalDisplayName.startsWith(plugin.getLang().getMessage("work-gui.library.cartography-table"))) {
                    shouldCreateStand = false;
                }

                if (shouldCreateStand && b.getType() != Material.CARTOGRAPHY_TABLE) {
                    Location standLoc = b.getLocation().clone().add(0.5, 1.5, 0.5);
                    ArmorStand stand = b.getWorld().spawn(standLoc, ArmorStand.class);
                    stand.setVisible(false);
                    stand.setGravity(false);
                    stand.setCustomNameVisible(true);
                    stand.setCustomName(finalDisplayName);
                    stand.setMarker(true);
                    stand.setSmall(true);
                    stand.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));
                }

                for (int i = 0; i < 5; i++) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            b.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER,
                                    b.getLocation().clone().add(0.5, 1.0, 0.5), 5, 0.3, 0.3, 0.3, 0);
                        }
                    }.runTaskLater(plugin, i * 2L);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getState() instanceof Container) {
            Container container = (Container) block.getState();
            String customName = container.getCustomName();
            if (customName == null) return;

            if (customName.startsWith(plugin.getLang().getMessage("work-gui.library.bookshelf-prefix"))) {
                if (plugin.getJailWorkListener() != null) {
                    plugin.getJailWorkListener().removeShelf(block.getLocation());
                }
            }

            Location standLoc = block.getLocation().clone().add(0.5, 1.5, 0.5);
            for (Entity entity : block.getWorld().getNearbyEntities(standLoc, 0.3, 0.3, 0.3)) {
                if (entity instanceof ArmorStand) {
                    ArmorStand stand = (ArmorStand) entity;
                    if (!stand.isVisible() && stand.getCustomName() != null && stand.getCustomName().equals(customName)) {
                        stand.remove();
                        event.getPlayer().playSound(block.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                        break;
                    }
                }
            }
        }
    }
}