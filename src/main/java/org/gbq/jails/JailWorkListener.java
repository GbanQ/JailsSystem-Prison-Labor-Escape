package org.gbq.jails;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JailWorkListener implements Listener {
    private final Jails plugin;
    private final Map<String, Integer> playerWorkCount = new HashMap<>();
    private final Map<String, String> playerCurrentTask = new HashMap<>();
    private final Map<String, BukkitRunnable> playerTaskTimers = new HashMap<>();
    private final Map<Block, Boolean> washingInProgress = new HashMap<>();
    private final Map<Block, ArmorStand> washingStands = new HashMap<>();
    private final Map<String, Long> lastInteraction = new HashMap<>();

    // Библиотечные константы (это названия блоков, они должны остаться для сравнения)
    private static final String BOOK_DISPENSER_NAME = "§6Выдача книг";
    private static final String BOOKSHELF_PREFIX = "§6Книжная полка: ";
    private static final String MAP_FRAGMENT_NAME = "§7Обрывок карты";

    // Хранилище установленных книжных полок по категориям
    private final Map<String, List<Location>> shelvesByCategory = new HashMap<>();

    // Файл для сохранения полок
    private final File shelvesFile;
    private FileConfiguration shelvesConfig;

    // Хранилище активных заданий на подсветку
    private final Map<Player, LibraryGlowTask> activeGlowTasks = new HashMap<>();

    // Хранилище активных заданий игроков (категория книги, которую нужно положить)
    private final Map<String, String> playerLibraryTask = new HashMap<>();

    // Кэш для категорий полок (чтобы не искать каждый раз)
    private final Map<Location, String> shelfCategoryCache = new HashMap<>();

    // Вложенный класс для задачи подсветки
    private static class LibraryGlowTask {
        final String category;
        final Location shelfLocation;
        final BukkitRunnable particleTask;

        LibraryGlowTask(String category, Location shelfLocation, BukkitRunnable task) {
            this.category = category;
            this.shelfLocation = shelfLocation;
            this.particleTask = task;
        }

        void cancel() {
            particleTask.cancel();
        }
    }

    public JailWorkListener(Jails plugin) {
        this.plugin = plugin;
        this.shelvesFile = new File(plugin.getDataFolder(), "shelves.yml");
        loadShelves();
        startSlownessTask();
    }

    // ================ ЗАГРУЗКА И СОХРАНЕНИЕ ПОЛОК ================
    private void loadShelves() {
        if (!shelvesFile.exists()) {
            try {
                shelvesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        shelvesConfig = YamlConfiguration.loadConfiguration(shelvesFile);
        shelvesByCategory.clear();
        shelfCategoryCache.clear();

        ConfigurationSection categoriesSection = shelvesConfig.getConfigurationSection("shelves");
        if (categoriesSection != null) {
            for (String category : categoriesSection.getKeys(false)) {
                List<Map<?, ?>> locMaps = categoriesSection.getMapList(category);
                List<Location> locs = new ArrayList<>();
                for (Map<?, ?> map : locMaps) {
                    String worldName = (String) map.get("world");
                    double x = ((Number) map.get("x")).doubleValue();
                    double y = ((Number) map.get("y")).doubleValue();
                    double z = ((Number) map.get("z")).doubleValue();
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        Location loc = new Location(world, x, y, z);
                        locs.add(loc);
                        shelfCategoryCache.put(loc, category);
                    }
                }
                if (!locs.isEmpty()) {
                    shelvesByCategory.put(category, locs);
                }
            }
        }
        plugin.getLogger().info("Загружено библиотечных полок: " + shelvesByCategory.values().stream().mapToInt(List::size).sum()); // НЕ ПЕРЕВОДИМ - консоль
    }

    private void saveShelves() {
        shelvesConfig.set("shelves", null);
        for (Map.Entry<String, List<Location>> entry : shelvesByCategory.entrySet()) {
            List<Map<String, Object>> locMaps = new ArrayList<>();
            for (Location loc : entry.getValue()) {
                Map<String, Object> map = new HashMap<>();
                map.put("world", loc.getWorld().getName());
                map.put("x", loc.getX());
                map.put("y", loc.getY());
                map.put("z", loc.getZ());
                locMaps.add(map);
            }
            shelvesConfig.set("shelves." + entry.getKey(), locMaps);
        }
        try {
            shelvesConfig.save(shelvesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addShelf(String category, Location loc) {
        shelvesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(loc);
        shelfCategoryCache.put(loc, category);
        saveShelves();
    }

    public void removeShelf(Location loc) {
        shelfCategoryCache.remove(loc);
        boolean changed = false;
        for (List<Location> list : shelvesByCategory.values()) {
            if (list.remove(loc)) {
                changed = true;
                break;
            }
        }
        if (changed) {
            saveShelves();
        }
    }


    private boolean isLibraryEnabled() {
        return plugin.getConfig().getBoolean("library.enabled", true);
    }

    // ================ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ БИБЛИОТЕКИ ================

    private String getBookCategory(String bookName) {
        ConfigurationSection categories = plugin.getConfig().getConfigurationSection("library.categories");
        if (categories == null) return null;
        for (String key : categories.getKeys(false)) {
            List<String> names = categories.getStringList(key + ".book-names");
            for (String name : names) {
                String coloredName = ChatColor.translateAlternateColorCodes('&', name);
                if (coloredName.equals(bookName)) {
                    return categories.getString(key + ".name");
                }
            }
        }
        return null;
    }

    private boolean isLibraryBook(ItemStack item) {
        if (item == null || item.getType() != Material.BOOK || !item.hasItemMeta()) return false;
        String displayName = item.getItemMeta().getDisplayName();
        return getBookCategory(displayName) != null;
    }

    private String getBookCategory(ItemStack item) {
        if (!isLibraryBook(item)) return null;
        return getBookCategory(item.getItemMeta().getDisplayName());
    }

    private String getShelfCategory(Location loc) {
        return shelfCategoryCache.get(loc);
    }

    private List<Location> getShelvesByCategory(String category) {
        return shelvesByCategory.getOrDefault(category, new ArrayList<>());
    }

    private boolean isMapFragment(ItemStack item) {
        return item != null && item.getType() == Material.PAPER && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().equals(MAP_FRAGMENT_NAME);
    }

    private void giveMapFragment(Player player) {
        ItemStack fragment = new ItemStack(Material.PAPER);
        ItemMeta meta = fragment.getItemMeta();
        meta.setDisplayName(plugin.getLang().getMessage("items.map-fragment"));
        fragment.setItemMeta(meta);
        player.getInventory().addItem(fragment);
        player.sendMessage(plugin.getLang().getMessage("work.library.fragment-found"));
    }

    // ================ ОБРАБОТЧИК ВЗАИМОДЕЙСТВИЯ ================

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND)
            return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null)
            return;
        
        // Защита от спама
        UUID playerId = player.getUniqueId();
        String blockKey = clickedBlock.getWorld().getName() + ":" +
                clickedBlock.getX() + ":" +
                clickedBlock.getY() + ":" +
                clickedBlock.getZ();
        long now = System.currentTimeMillis();
        String key = playerId + ":" + blockKey;
        Long last = lastInteraction.get(key);
        if (last != null && now - last < 200) {
            return;
        }
        lastInteraction.put(key, now);

        if (!plugin.getJailedPlayers().containsKey(player.getName()))
            return;

        // ================ БИБЛИОТЕКА  ================
        if (isLibraryEnabled()) {
            if (clickedBlock.getType() == Material.BARREL) {
                Barrel barrel = (Barrel) clickedBlock.getState();
                String barrelName = barrel.getCustomName();
                if (barrelName != null && barrelName.equals(BOOK_DISPENSER_NAME)) {
                    event.setCancelled(true);
                    handleBookTake(player);
                    return;
                }
            }

            if (clickedBlock.getType() == Material.CHISELED_BOOKSHELF) {
                event.setCancelled(true);
                handleBookShelfInteract(player, clickedBlock);
                return;
            }
        }

        // ================ КИРПИЧИ ================
        if (clickedBlock.getType() == Material.CHEST) {
            Chest chest = (Chest) clickedBlock.getState();
            String chestName = chest.getCustomName();
            if (chestName == null)
                return;

            if ("§6Взять кирпичи".equals(chestName)) {
                event.setCancelled(true);
                handleBrickTake(player);
                return;
            }
            if ("§6Положить кирпичи".equals(chestName)) {
                event.setCancelled(true);
                handleBrickPut(player);
                return;
            }
        }

        // ================ ПРАЧЕЧНАЯ ================
        if (clickedBlock.getType() == Material.BARREL) {
            Barrel barrel = (Barrel) clickedBlock.getState();
            String barrelName = barrel.getCustomName();
            if (barrelName == null)
                return;

            if ("§6Грязная одежда".equals(barrelName)) {
                event.setCancelled(true);
                handleDirtyClothesTake(player);
                return;
            }
            if ("§6Чистая одежда".equals(barrelName)) {
                event.setCancelled(true);
                handleCleanClothesPut(player);
                return;
            }
        }

        // ================ КУХНЯ ================
        if (clickedBlock.getType() == Material.BARREL) {
            Barrel barrel = (Barrel) clickedBlock.getState();
            String barrelName = barrel.getCustomName();
            if (barrelName == null)
                return;

            if ("§6Взять ингредиенты".equals(barrelName)) {
                event.setCancelled(true);
                handleIngredientsTake(player);
                return;
            }
            if ("§6Положить готовое блюдо".equals(barrelName)) {
                event.setCancelled(true);
                handleDishPut(player);
                return;
            }
        }

        // ================ СТИРАЛЬНАЯ МАШИНКА ================
        if (clickedBlock.getType() == Material.DROPPER) {
            Dropper dropper = (Dropper) clickedBlock.getState();
            if ("§6Стиральная машинка".equals(dropper.getCustomName())) {
                // Обрабатывается в onInventoryClose
            }
        }
    }

    // ================ МЕТОДЫ ДЛЯ БИБЛИОТЕКИ ================

    private void handleBookTake(Player player) {
        if (!plugin.getJailedPlayers().containsKey(player.getName())) return;

        if (playerLibraryTask.containsKey(player.getName())) {
            player.sendMessage(plugin.getLang().getMessage("work.library.already-has-book"));
            return;
        }

        ConfigurationSection categories = plugin.getConfig().getConfigurationSection("library.categories");
        if (categories == null || categories.getKeys(false).isEmpty()) {
            player.sendMessage(plugin.getLang().getMessage("work.library.no-categories"));
            return;
        }

        List<String> allBookNames = new ArrayList<>();
        Map<String, String> bookToCategory = new HashMap<>();
        List<String> categoryNames = new ArrayList<>();
        
        for (String catKey : categories.getKeys(false)) {
            String catName = categories.getString(catKey + ".name");
            if (catName != null) {
                categoryNames.add(catName);
            }
            List<String> names = categories.getStringList(catKey + ".book-names");
            for (String name : names) {
                String colored = ChatColor.translateAlternateColorCodes('&', name);
                allBookNames.add(colored);
                bookToCategory.put(colored, catName);
            }
        }

        if (allBookNames.isEmpty()) {
            player.sendMessage(plugin.getLang().getMessage("work.library.no-books"));
            return;
        }

        String selectedBook = allBookNames.get(new Random().nextInt(allBookNames.size()));
        String category = bookToCategory.get(selectedBook);

        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName(selectedBook);
        book.setItemMeta(meta);

        player.getInventory().addItem(book);
        player.sendMessage(plugin.getLang().getMessage("work.library.book-taken", "book", selectedBook));

        playerLibraryTask.put(player.getName(), category);

        boolean glowEnabled = plugin.getConfig().getBoolean("library.glow-enabled", true);
        if (glowEnabled) {
            startGlowForPlayer(player, category);
        } else {
            List<Location> shelves = getShelvesByCategory(category);
            if (shelves.isEmpty()) {
                player.sendMessage(plugin.getLang().getMessage("work.library.no-shelves"));
            } else {
                String namesList = String.join(", ", categoryNames);
                player.sendMessage(plugin.getLang().getMessage("work.library.shelf-instruction", "categories", namesList));
            }
        }
    }

    private void startGlowForPlayer(Player player, String category) {
        LibraryGlowTask oldTask = activeGlowTasks.remove(player);
        if (oldTask != null) oldTask.cancel();

        List<Location> shelves = getShelvesByCategory(category);
        if (shelves == null || shelves.isEmpty()) {
            player.sendMessage(plugin.getLang().getMessage("work.library.no-shelves-for-category", "category", category));
            return;
        }

        List<Location> shelvesCopy = new ArrayList<>(shelves);
        Location target = shelvesCopy.get(new Random().nextInt(shelvesCopy.size()));
        Block block = target.getBlock();
        if (!block.getType().equals(Material.CHISELED_BOOKSHELF)) {
            shelves.remove(target);
            if (!shelves.isEmpty()) {
                target = new ArrayList<>(shelves).get(new Random().nextInt(shelves.size()));
            } else {
                return;
            }
        }

        BlockFace face = getBlockFace(block);
        Location particleLoc = target.clone().add(0.5, 0.5, 0.5);
        switch (face) {
            case NORTH: particleLoc.add(0, 0, -0.6); break;
            case SOUTH: particleLoc.add(0, 0, 0.6); break;
            case WEST: particleLoc.add(-0.6, 0, 0); break;
            case EAST: particleLoc.add(0.6, 0, 0); break;
            default: particleLoc.add(0, 0, -0.6);
        }

        Particle particleType;
        try {
            particleType = Particle.valueOf(plugin.getConfig().getString("library.particle-type", "END_ROD"));
        } catch (IllegalArgumentException e) {
            particleType = Particle.END_ROD;
        }

        final Location finalTarget = target.clone();
        final Block finalBlock = finalTarget.getBlock();
        final Particle finalParticle = particleType;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !finalBlock.getType().equals(Material.CHISELED_BOOKSHELF)) {
                    this.cancel();
                    activeGlowTasks.remove(player);
                    return;
                }
                player.spawnParticle(finalParticle, particleLoc, 1, 0, 0, 0, 0);
            }
        };
        task.runTaskTimer(plugin, 0L, 5L);
        activeGlowTasks.put(player, new LibraryGlowTask(category, finalTarget, task));
    }

    private BlockFace getBlockFace(Block block) {
        if (block.getBlockData() instanceof Directional) {
            return ((Directional) block.getBlockData()).getFacing();
        }
        return BlockFace.NORTH;
    }

    private void handleBookShelfInteract(Player player, Block block) {
        Location loc = block.getLocation();

        if (!playerLibraryTask.containsKey(player.getName())) {
            player.sendMessage(plugin.getLang().getMessage("work.library.no-task"));
            return;
        }

        String expectedCategory = playerLibraryTask.get(player.getName());
        
        String shelfCategory = getShelfCategory(loc);
        
        if (shelfCategory == null) {
            player.sendMessage(plugin.getLang().getMessage("work.library.wrong-shelf"));
            return;
        }

        if (!shelfCategory.equals(expectedCategory)) {
            player.sendMessage(plugin.getLang().getMessage("work.library.wrong-shelf"));
            return;
        }

        boolean glowEnabled = plugin.getConfig().getBoolean("library.glow-enabled", true);
        if (glowEnabled) {
            LibraryGlowTask activeTask = activeGlowTasks.get(player);
            if (activeTask == null || !loc.equals(activeTask.shelfLocation)) {
                player.sendMessage(plugin.getLang().getMessage("work.library.not-glow-shelf"));
                return;
            }
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (!isLibraryBook(handItem)) {
            player.sendMessage(plugin.getLang().getMessage("work.library.no-book-in-hand"));
            return;
        }

        String bookCategory = getBookCategory(handItem);
        if (!expectedCategory.equals(bookCategory)) {
            playerLibraryTask.remove(player.getName());
            cancelGlowTask(player);
            player.sendMessage(plugin.getLang().getMessage("work.library.wrong-book"));
            return;
        }

        ChiseledBookshelf shelf = (ChiseledBookshelf) block.getState();
        for (int i = 0; i < 6; i++) {
            ItemStack slotItem = shelf.getInventory().getItem(i);
            if (slotItem == null || slotItem.getType().isAir()) {
                shelf.getInventory().setItem(i, handItem.clone());
                shelf.update();
                handItem.setAmount(handItem.getAmount() - 1);

                int reduction = plugin.getConfig().getInt("library.time-reduction", 15);
                if (reduction > 0) {
                    plugin.reduceJailTime(player.getName(), reduction);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(plugin.getLang().getMessage("work.brick.reward-action-bar", "seconds", reduction)));
                }

                double chance = plugin.getConfig().getDouble("library.fragment-chance", 0.1);
                if (Math.random() < chance) {
                    giveMapFragment(player);
                }

                playerLibraryTask.remove(player.getName());
                cancelGlowTask(player);

                player.sendMessage(plugin.getLang().getMessage("work.library.success"));
                return;
            }
        }
        player.sendMessage(plugin.getLang().getMessage("work.library.shelf-full"));
    }
    
    private void cancelGlowTask(Player player) {
        LibraryGlowTask task = activeGlowTasks.remove(player);
        if (task != null) {
            task.cancel();
        }
    }

    // ================ КИРПИЧИ ================

    private void handleBrickTake(Player player) {
        if (!hasBrickPiece(player)) {
            ItemStack brickPiece = new ItemStack(Material.STONE_BRICKS);
            ItemMeta meta = brickPiece.getItemMeta();
            meta.setDisplayName(plugin.getLang().getMessage("items.brick-piece"));
            brickPiece.setItemMeta(meta);

            ItemStack currentItem = player.getInventory().getItem(player.getInventory().getHeldItemSlot());
            if (currentItem == null || currentItem.getType() == Material.AIR) {
                player.getInventory().setItem(player.getInventory().getHeldItemSlot(), brickPiece);
            } else {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(brickPiece);
                if (!leftover.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), brickPiece);
                }
            }
            player.sendMessage(plugin.getLang().getMessage("work.brick.take"));
        } else {
            player.sendMessage(plugin.getLang().getMessage("work.brick.already-has"));
        }
    }

    private void handleBrickPut(Player player) {
        if (hasBrickPiece(player)) {
            removeBrickPiece(player);
            int reduction = plugin.getBrickTimeReduction();
            if (reduction > 0) {
                plugin.reduceJailTime(player.getName(), reduction);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(plugin.getLang().getMessage("work.brick.reward-action-bar", "seconds", reduction)));
            }
            increaseWorkCount(player);

            if (plugin.isBrickRewardEnabled() &&
                    plugin.getBrickSwcoinWorkCount() > 0 &&
                    playerWorkCount.getOrDefault(player.getName(), 0) % plugin.getBrickSwcoinWorkCount() == 0) {
                giveSWcoin(player);
            }

            if (plugin.getBrickLockpickChance() > 0 &&
                    Math.random() * 100 <= plugin.getBrickLockpickChance()) {
                player.getInventory().addItem(createLockPick());
                player.sendMessage(plugin.getLang().getMessage("work.brick.lockpick-found"));
            }
        } else {
            player.sendMessage(plugin.getLang().getMessage("work.brick.put"));
        }
    }

    // ================ ПРАЧЕЧНАЯ ================

    private void handleDirtyClothesTake(Player player) {
        if (!hasCompleteDirtyOutfit(player)) {
            ItemStack dirtyChest = createLeatherArmorPiece(plugin.getLang().getMessage("items.dirty-clothes"),
                    Color.fromRGB(139, 69, 19), Material.LEATHER_CHESTPLATE);
            ItemStack dirtyLegs = createLeatherArmorPiece(plugin.getLang().getMessage("items.dirty-clothes"),
                    Color.fromRGB(139, 69, 19), Material.LEATHER_LEGGINGS);
            ItemStack dirtyBoots = createLeatherArmorPiece(plugin.getLang().getMessage("items.dirty-clothes"),
                    Color.fromRGB(139, 69, 19), Material.LEATHER_BOOTS);

            player.getInventory().addItem(dirtyChest, dirtyLegs, dirtyBoots);
            player.sendMessage(plugin.getLang().getMessage("work.laundry.take"));

            double chance = plugin.getLaundryGuardArmorChance();
            if (chance > 0 && Math.random() * 100 <= chance) {
                giveRandomGuardArmor(player);
            }
        } else {
            player.sendMessage(plugin.getLang().getMessage("work.laundry.already-has"));
        }
    }

    private void handleCleanClothesPut(Player player) {
        if (hasCleanOutfit(player)) {
            removeCleanOutfit(player);
            int reduction = plugin.getLaundryTimeReduction();
            if (reduction > 0) {
                plugin.reduceJailTime(player.getName(), reduction);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(plugin.getLang().getMessage("work.brick.reward-action-bar", "seconds", reduction)));
            }
            increaseWorkCount(player);
            if (plugin.isLaundryRewardEnabled() &&
                    plugin.getLaundrySwcoinWorkCount() > 0 &&
                    playerWorkCount.getOrDefault(player.getName(), 0) % plugin.getLaundrySwcoinWorkCount() == 0) {
                giveSWcoin(player);
            }
        } else {
            player.sendMessage(plugin.getLang().getMessage("work.laundry.put"));
        }
    }

    // ================ КУХНЯ ================

    private void handleIngredientsTake(Player player) {
        if (!playerCurrentTask.containsKey(player.getName())) {
            Map<String, RecipeData> recipes = plugin.getKitchenRecipes();
            if (recipes == null || recipes.isEmpty()) {
                player.sendMessage(plugin.getLang().getMessage("work.kitchen.no-recipes"));
                return;
            }

            List<String> recipeKeys = new ArrayList<>(recipes.keySet());
            String recipeKey = recipeKeys.get(new Random().nextInt(recipeKeys.size()));
            RecipeData recipe = recipes.get(recipeKey);
            if (recipe == null)
                return;

            for (Material ingredient : recipe.getIngredients()) {
                ItemStack item = new ItemStack(ingredient);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§7" + recipe.getDisplayName());
                item.setItemMeta(meta);
                player.getInventory().addItem(item);
            }

            playerCurrentTask.put(player.getName(), recipeKey);
            player.sendMessage(plugin.getLang().getMessage("work.kitchen.take", 
                    "dish", recipe.getDisplayName(), "seconds", recipe.getMaxTime()));

            startTaskTimer(player, recipeKey);
        } else {
            player.sendMessage(plugin.getLang().getMessage("work.kitchen.already-task"));
        }
    }

    private void handleDishPut(Player player) {
        String currentRecipeKey = playerCurrentTask.get(player.getName());
        if (currentRecipeKey == null) {
            player.sendMessage(plugin.getLang().getMessage("work.kitchen.put-no-task"));
            return;
        }

        Map<String, RecipeData> recipes = plugin.getKitchenRecipes();
        RecipeData recipe = recipes.get(currentRecipeKey);
        if (recipe == null) {
            player.sendMessage(plugin.getLang().getMessage("work.kitchen.recipe-error"));
            playerCurrentTask.remove(player.getName());
            return;
        }

        if (player.getInventory().contains(recipe.getResult())) {
            player.getInventory().removeItem(new ItemStack(recipe.getResult()));
            int reward = recipe.getReward();
            if (reward > 0) {
                plugin.reduceJailTime(player.getName(), reward);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(plugin.getLang().getMessage("work.brick.reward-action-bar", "seconds", reward)));
            }
            playerCurrentTask.remove(player.getName());
            cancelTaskTimer(player);
            increaseWorkCount(player);

            if (plugin.isKitchenRewardEnabled() &&
                    plugin.getKitchenSwcoinWorkCount() > 0 &&
                    playerWorkCount.getOrDefault(player.getName(), 0) % plugin.getKitchenSwcoinWorkCount() == 0) {
                giveSWcoin(player);
            }
        } else {
            player.sendMessage(plugin.getLang().getMessage("work.kitchen.put-no-dish", "dish", recipe.getDisplayName()));
        }
    }

    // ================ ЛОГИКА СТИРКИ ================

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof Dropper))
            return;
        Dropper dropper = (Dropper) inv.getHolder();
        if (!"§6Стиральная машинка".equals(dropper.getCustomName()))
            return;

        Block block = dropper.getBlock();

        if (washingInProgress.getOrDefault(block, false))
            return;

        if (hasAllDirtyClothes(dropper)) {
            startWashingProcess(dropper, player, block);
        }
    }

    private boolean hasAllDirtyClothes(Dropper dropper) {
        boolean chest = false, legs = false, boots = false;
        for (ItemStack item : dropper.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()
                    && plugin.getLang().getMessage("items.dirty-clothes").equals(item.getItemMeta().getDisplayName())) {
                if (item.getType() == Material.LEATHER_CHESTPLATE)
                    chest = true;
                else if (item.getType() == Material.LEATHER_LEGGINGS)
                    legs = true;
                else if (item.getType() == Material.LEATHER_BOOTS)
                    boots = true;
            }
        }
        return chest && legs && boots;
    }

    private void startWashingProcess(Dropper dropper, Player player, Block block) {
        washingInProgress.put(block, true);
        Location standLoc = block.getLocation().clone().add(0.5, 0.85, 0.5);
        ArmorStand stand = (ArmorStand) block.getWorld().spawnEntity(standLoc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setMarker(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName("§e⏳ " + (plugin.getLaundryWashDuration() / 20) + "с");
        stand.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));
        washingStands.put(block, stand);

        new BukkitRunnable() {
            int ticksLeft = plugin.getLaundryWashDuration();

            @Override
            public void run() {
                if (block.getType() != Material.DROPPER) {
                    removeWashingStand(block);
                    washingInProgress.remove(block);
                    cancel();
                    return;
                }

                ticksLeft -= 20;
                if (ticksLeft <= 0) {
                    finishWashing(block);
                    cancel();
                    return;
                }

                ArmorStand s = washingStands.get(block);
                if (s != null && !s.isDead()) {
                    s.setCustomName("§e⏳ " + (ticksLeft / 20) + "с");
                }

                moveItemsInCircle(dropper);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void finishWashing(Block block) {
        if (block.getType() != Material.DROPPER)
            return;

        Dropper dropper = (Dropper) block.getState();
        replaceDirtyWithCleanClothes(dropper);

        removeWashingStand(block);
        washingInProgress.remove(block);

        if (!dropper.getWorld().getPlayers().isEmpty()) {
            dropper.getWorld().playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
        }
    }

    private void removeWashingStand(Block block) {
        ArmorStand stand = washingStands.remove(block);
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }

    private void moveItemsInCircle(Dropper dropper) {
        ItemStack[] contents = dropper.getInventory().getContents();
        ItemStack temp = contents[0];
        contents[0] = contents[3];
        contents[3] = contents[6];
        contents[6] = contents[7];
        contents[7] = contents[8];
        contents[8] = contents[5];
        contents[5] = contents[2];
        contents[2] = contents[1];
        contents[1] = temp;
        dropper.getInventory().setContents(contents);
    }

    private void replaceDirtyWithCleanClothes(Dropper dropper) {
        for (int i = 0; i < dropper.getInventory().getSize(); i++) {
            ItemStack item = dropper.getInventory().getItem(i);
            if (item != null && item.hasItemMeta()
                    && plugin.getLang().getMessage("items.dirty-clothes").equals(item.getItemMeta().getDisplayName())) {
                ItemStack clean = new ItemStack(item.getType());
                LeatherArmorMeta meta = (LeatherArmorMeta) clean.getItemMeta();
                meta.setDisplayName(plugin.getLang().getMessage("items.clean-clothes"));
                meta.setColor(Color.fromRGB(213, 91, 29));
                clean.setItemMeta(meta);
                dropper.getInventory().setItem(i, clean);
            }
        }
    }

    // ================ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ================

    private void giveRandomGuardArmor(Player player) {
        double r = Math.random();
        if (r < 0.25) {
            ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
            LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
            meta.setColor(Color.fromRGB(0, 0, 70));
            meta.setDisplayName(plugin.getLang().getMessage("items.guard-armor.helmet"));
            helmet.setItemMeta(meta);
            player.getInventory().addItem(helmet);
            player.sendMessage(plugin.getLang().getMessage("work.laundry.guard-armor-found", "piece", plugin.getLang().getMessage("items.guard-armor.helmet")));
        } else if (r < 0.5) {
            ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
            LeatherArmorMeta meta = (LeatherArmorMeta) chest.getItemMeta();
            meta.setColor(Color.fromRGB(0, 0, 70));
            meta.setDisplayName(plugin.getLang().getMessage("items.guard-armor.chestplate"));
            chest.setItemMeta(meta);
            player.getInventory().addItem(chest);
            player.sendMessage(plugin.getLang().getMessage("work.laundry.guard-armor-found", "piece", plugin.getLang().getMessage("items.guard-armor.chestplate")));
        } else if (r < 0.75) {
            ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS);
            LeatherArmorMeta meta = (LeatherArmorMeta) legs.getItemMeta();
            meta.setColor(Color.fromRGB(0, 0, 70));
            meta.setDisplayName(plugin.getLang().getMessage("items.guard-armor.leggings"));
            legs.setItemMeta(meta);
            player.getInventory().addItem(legs);
            player.sendMessage(plugin.getLang().getMessage("work.laundry.guard-armor-found", "piece", plugin.getLang().getMessage("items.guard-armor.leggings")));
        } else {
            ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
            LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
            meta.setColor(Color.fromRGB(71, 74, 81));
            meta.setDisplayName(plugin.getLang().getMessage("items.guard-armor.boots"));
            boots.setItemMeta(meta);
            player.getInventory().addItem(boots);
            player.sendMessage(plugin.getLang().getMessage("work.laundry.guard-armor-found", "piece", plugin.getLang().getMessage("items.guard-armor.boots")));
        }
    }

    private ItemStack createLockPick() {
        ItemStack lockPick = plugin.getItemChecker().createItem("items.lockpick");
        if (lockPick == null) {
            lockPick = new ItemStack(Material.WOODEN_HOE);
            ItemMeta meta = lockPick.getItemMeta();
            meta.setDisplayName(plugin.getLang().getMessage("items.lockpick"));
            meta.setLore(Collections.singletonList(plugin.getLang().getMessage("items.lockpick-lore")));
            lockPick.setItemMeta(meta);
        }
        return lockPick;
    }

    private void giveSWcoin(Player player) {
        ItemStack coin = new ItemStack(plugin.getSwcoinMaterial());
        ItemMeta meta = coin.getItemMeta();
        meta.setDisplayName(plugin.getSwcoinName());
        meta.setLore(Collections.singletonList(plugin.getSwcoinLore()));
        coin.setItemMeta(meta);
        player.getInventory().addItem(coin);
        player.sendMessage(plugin.getLang().getMessage("misc.swcoin-reward", "swcoin", plugin.getSwcoinName()));
    }

    private boolean hasBrickPiece(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.STONE_BRICKS && item.hasItemMeta() &&
                    plugin.getLang().getMessage("items.brick-piece").equals(item.getItemMeta().getDisplayName())) {
                return true;
            }
        }
        return false;
    }

    private void removeBrickPiece(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.STONE_BRICKS && item.hasItemMeta() &&
                    plugin.getLang().getMessage("items.brick-piece").equals(item.getItemMeta().getDisplayName())) {
                player.getInventory().removeItem(item);
                break;
            }
        }
    }

    private boolean hasCompleteDirtyOutfit(Player player) {
        boolean chest = false, legs = false, boots = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()
                    && plugin.getLang().getMessage("items.dirty-clothes").equals(item.getItemMeta().getDisplayName())) {
                if (item.getType() == Material.LEATHER_CHESTPLATE)
                    chest = true;
                else if (item.getType() == Material.LEATHER_LEGGINGS)
                    legs = true;
                else if (item.getType() == Material.LEATHER_BOOTS)
                    boots = true;
            }
        }
        return chest && legs && boots;
    }

    private boolean hasCleanOutfit(Player player) {
        boolean chest = false, legs = false, boots = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()
                    && plugin.getLang().getMessage("items.clean-clothes").equals(item.getItemMeta().getDisplayName())) {
                if (item.getType() == Material.LEATHER_CHESTPLATE)
                    chest = true;
                else if (item.getType() == Material.LEATHER_LEGGINGS)
                    legs = true;
                else if (item.getType() == Material.LEATHER_BOOTS)
                    boots = true;
            }
        }
        return chest && legs && boots;
    }

    private void removeCleanOutfit(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()
                    && plugin.getLang().getMessage("items.clean-clothes").equals(item.getItemMeta().getDisplayName())) {
                player.getInventory().removeItem(item);
            }
        }
    }

    private ItemStack createLeatherArmorPiece(String name, Color color, Material type) {
        ItemStack piece = new ItemStack(type);
        LeatherArmorMeta meta = (LeatherArmorMeta) piece.getItemMeta();
        meta.setDisplayName(name);
        meta.setColor(color);
        piece.setItemMeta(meta);
        return piece;
    }

    private void increaseWorkCount(Player player) {
        playerWorkCount.put(player.getName(), playerWorkCount.getOrDefault(player.getName(), 0) + 1);
    }

    // ================ ТАЙМЕРЫ ДЛЯ КУХНИ ================

    private void startTaskTimer(Player player, String recipeKey) {
        cancelTaskTimer(player);

        Map<String, RecipeData> recipes = plugin.getKitchenRecipes();
        RecipeData recipe = recipes.get(recipeKey);
        if (recipe == null)
            return;

        int taskTime = getRandomTime(recipe.getMinTime(), recipe.getMaxTime());

        BukkitRunnable taskTimer = new BukkitRunnable() {
            int timeLeft = taskTime;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    player.sendMessage(plugin.getLang().getMessage("work.kitchen.task-failed", "dish", recipe.getDisplayName()));

                    for (Material ingredient : recipe.getIngredients()) {
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null && item.getType() == ingredient &&
                                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                                    item.getItemMeta().getDisplayName().equals("§7" + recipe.getDisplayName())) {
                                player.getInventory().removeItem(item);
                                break;
                            }
                        }
                    }

                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() == recipe.getResult() &&
                                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                                item.getItemMeta().getDisplayName().equals("§7" + recipe.getDisplayName())) {
                            player.getInventory().removeItem(item);
                            break;
                        }
                    }

                    playerCurrentTask.remove(player.getName());
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(plugin.getLang().getMessage("work.kitchen.task-failed-action-bar")));
                    cancel();
                    playerTaskTimers.remove(player.getName());
                    return;
                }

                String timeLeftString = String.format(plugin.getLang().getMessage("work.kitchen.action-bar-time"), timeLeft);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(timeLeftString));
                timeLeft--;
            }
        };

        taskTimer.runTaskTimer(plugin, 0L, 20L);
        playerTaskTimers.put(player.getName(), taskTimer);
    }

    private void cancelTaskTimer(Player player) {
        BukkitRunnable task = playerTaskTimers.remove(player.getName());
        if (task != null) {
            task.cancel();
        }
    }

    private int getRandomTime(int min, int max) {
        if (min <= 0)
            min = 10;
        if (max <= min)
            max = min + 10;
        return new Random().nextInt(max - min + 1) + min;
    }

    // ================ ЗАДАЧА ЗАМЕДЛЕНИЯ ================

    private void startSlownessTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.getJailedPlayers().containsKey(player.getName()) && hasBrickPiece(player)) {
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS,
                                plugin.getBrickSlownessDuration(),
                                plugin.getBrickSlownessAmplifier(),
                                true, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, plugin.getBrickSlownessInterval());
    }

    // ================ ЗАПРЕТ НА ВЫБРАСЫВАНИЕ ================

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getJailedPlayers().containsKey(player.getName()))
            return;
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped.hasItemMeta()) {
            String display = dropped.getItemMeta().getDisplayName();
            if (plugin.getLang().getMessage("items.brick-piece").equals(display) ||
                plugin.getLang().getMessage("items.dirty-clothes").equals(display) ||
                plugin.getLang().getMessage("items.clean-clothes").equals(display)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        if (event.getInventory().getHolder() instanceof Dropper) {
            Dropper dropper = (Dropper) event.getInventory().getHolder();
            if ("§6Стиральная машинка".equals(dropper.getCustomName())) {
                Block block = dropper.getBlock();

                if (washingInProgress.getOrDefault(block, false)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getLang().getMessage("work.general.washing-in-progress"));
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.05f, 1.0f);
                }
                return;
            }
        }

        if (!plugin.getJailedPlayers().containsKey(player.getName()))
            return;

        ItemStack current = event.getCurrentItem();
        if (current != null && current.hasItemMeta()) {
            String display = current.getItemMeta().getDisplayName();
            if (plugin.getLang().getMessage("items.brick-piece").equals(display) ||
                plugin.getLang().getMessage("items.dirty-clothes").equals(display) ||
                plugin.getLang().getMessage("items.clean-clothes").equals(display)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getJailedPlayers().containsKey(player.getName()))
            return;
        ItemStack previous = player.getInventory().getItem(event.getPreviousSlot());
        if (previous != null && previous.getType() == Material.STONE_BRICKS && previous.hasItemMeta() &&
                plugin.getLang().getMessage("items.brick-piece").equals(previous.getItemMeta().getDisplayName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (plugin.getJailedPlayers().containsKey(player.getName()) && hasBrickPiece(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.DROPPER) {
            removeWashingStand(block);
            washingInProgress.remove(block);
        }

        if (block.getState() instanceof Container) {
            Container container = (Container) block.getState();
            String customName = container.getCustomName();
            if (customName == null) return;

            if (customName.startsWith("§6Книжная полка:")) {
                removeShelf(block.getLocation());
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerLibraryTask.remove(player.getName());
        LibraryGlowTask task = activeGlowTasks.remove(player);
        if (task != null) task.cancel();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        playerLibraryTask.remove(player.getName());
        LibraryGlowTask task = activeGlowTasks.remove(player);
        if (task != null) task.cancel();
    }
}