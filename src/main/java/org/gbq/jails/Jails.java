package org.gbq.jails;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Jails extends JavaPlugin implements Listener {
    private final HashMap<UUID, Long> warningPlayers = new HashMap<>();
    private final HashMap<UUID, Boolean> killedByPVO = new HashMap<>();
    private final HashMap<UUID, BukkitRunnable> missileTasks = new HashMap<>();
    private final Map<Player, Integer> escapeCountdown = new HashMap<>();
    private final Map<Player, Long> lastLocationMessageTime = new HashMap<>();
    private JailWorkListener jailWorkListener;
    private JailBoardManager boardManager;
    private EscapeManager escapeManager;
    private BlackMarketManager blackMarketManager;
    private ItemChecker itemChecker;
    private LanguageManager lang;
    private File historyFile;
    private FileConfiguration historyConfig;
    private Location areaCorner1;
    private Location areaCorner2;
    private File cellsFile;
    private FileConfiguration cellsConfig;
    private File jailsFile;
    private FileConfiguration jailsConfig;
    private JailLogger jailLogger;
    private HashMap<String, JailData> jailedPlayers = new HashMap<>();
    private static Jails instance;
    private Map<Player, Long> lastActiveTime = new HashMap<>();
    private final Map<String, Long> lastMovementTime = new HashMap<>();
    private final Map<String, Boolean> afkStatus = new HashMap<>();

    // ================ ПОЛЯ КОНФИГУРАЦИИ ================
    // Флаги систем
    private boolean enablePvo;
    private boolean enableEscapes;
    private boolean enableWorkSystem;
    private boolean enableLockpicking;

    // Общие настройки
    private long afkTimeout; // в миллисекундах
    private int afkTimeMultiplier;
    private int escapeTimeLimit;
    private double escapeFailMultiplier;
    private Location spawnPoint;

    // ПВО
    private int pvoWarningTime; // в секундах
    private double pvoMissileStartSpeed;
    private double pvoMissileSpeedIncrement;
    private int pvoMissileSpeedIncrementInterval; // тики
    private double pvoMissileMaxSpeed;
    private float pvoExplosionPower;

    // Побег через табличку
    private int escapeShipEffectDuration; // тики
    private int escapeShipTeleportDelay; // тики
    private int escapeShipFinalMessageDelay;// тики
    private Location escapeShipDestination;

    // Работы (кирпичи)
    private int brickTimeReduction;
    private double brickLockpickChance;
    private int brickSlownessAmplifier;
    private int brickSlownessDuration;
    private int brickSlownessInterval;

    // Прачечная
    private int laundryTimeReduction;
    private double laundryGuardArmorChance;
    private int laundryWashDuration; // тики

    // Кухня
    private int kitchenTimeReduction;
    private int brickSwcoinWorkCount;
    private int kitchenSwcoinWorkCount;
    private int laundrySwcoinWorkCount;
    private Map<String, RecipeData> kitchenRecipes = new HashMap<>();

    // Награда за поимку
    private int captureRewardDivider;

    // Взлом
    private int lockpickCooldown; // секунды
    private double lockpickSuccessChance;
    private int lockpickAttemptDelay; // тики
    private int lockpickDoorOpenTime; // тики
    private double lockpickDurabilityLoss;
    private LockpickListener lockpickListener;

    // SWcoin
    private Material swcoinMaterial;
    private String swcoinName;
    private String swcoinLore;
    private boolean brickRewardEnabled;
    private boolean laundryRewardEnabled;
    private boolean kitchenRewardEnabled;
    // AFK сообщения
    private String afkMessageOn;
    private String afkMessageOff;

    // ================ ГЕТТЕРЫ ================
    public boolean isEnablePvo() {
        return enablePvo;
    }

    public boolean isEnableEscapes() {
        return enableEscapes;
    }

    public boolean isEnableWorkSystem() {
        return enableWorkSystem;
    }

    public boolean isEnableLockpicking() {
        return enableLockpicking;
    }

    public long getAfkTimeout() {
        return afkTimeout;
    }

    public int getAfkTimeMultiplier() {
        return afkTimeMultiplier;
    }

    public int getEscapeTimeLimit() {
        return escapeTimeLimit;
    }

    public double getEscapeFailMultiplier() {
        return escapeFailMultiplier;
    }

    public Location getSpawnPoint() {
        return spawnPoint.clone();
    }

    public Location getPvoCorner1() {
        return areaCorner1.clone();
    }

    public Location getPvoCorner2() {
        return areaCorner2.clone();
    }

    public int getPvoWarningTime() {
        return pvoWarningTime;
    }

    public double getPvoMissileStartSpeed() {
        return pvoMissileStartSpeed;
    }

    public double getPvoMissileSpeedIncrement() {
        return pvoMissileSpeedIncrement;
    }

    public int getPvoMissileSpeedIncrementInterval() {
        return pvoMissileSpeedIncrementInterval;
    }

    public double getPvoMissileMaxSpeed() {
        return pvoMissileMaxSpeed;
    }

    public float getPvoExplosionPower() {
        return pvoExplosionPower;
    }

    public int getEscapeShipEffectDuration() {
        return escapeShipEffectDuration;
    }

    public int getEscapeShipTeleportDelay() {
        return escapeShipTeleportDelay;
    }

    public int getEscapeShipFinalMessageDelay() {
        return escapeShipFinalMessageDelay;
    }

    public Location getEscapeShipDestination() {
        return escapeShipDestination.clone();
    }

    public int getBrickTimeReduction() {
        return brickTimeReduction;
    }

    public double getBrickLockpickChance() {
        return brickLockpickChance;
    }

    public int getBrickSlownessAmplifier() {
        return brickSlownessAmplifier;
    }

    public int getBrickSlownessDuration() {
        return brickSlownessDuration;
    }

    public int getBrickSlownessInterval() {
        return brickSlownessInterval;
    }

    public int getLaundrySwcoinWorkCount() {
        return laundrySwcoinWorkCount;
    }

    public int getLaundryTimeReduction() {
        return laundryTimeReduction;
    }

    public double getLaundryGuardArmorChance() {
        return laundryGuardArmorChance;
    }

    public int getLaundryWashDuration() {
        return laundryWashDuration;
    }

    public int getKitchenTimeReduction() {
        return kitchenTimeReduction;
    }

    public int getBrickSwcoinWorkCount() {
        return brickSwcoinWorkCount;
    }

    public int getKitchenSwcoinWorkCount() {
        return kitchenSwcoinWorkCount;
    }

    public Map<String, RecipeData> getKitchenRecipes() {
        return kitchenRecipes;
    }

    public int getCaptureRewardDivider() {
        return captureRewardDivider;
    }

    public int getLockpickCooldown() {
        return lockpickCooldown;
    }

    public double getLockpickSuccessChance() {
        return lockpickSuccessChance;
    }

    public int getLockpickAttemptDelay() {
        return lockpickAttemptDelay;
    }

    public int getLockpickDoorOpenTime() {
        return lockpickDoorOpenTime;
    }

    public double getLockpickDurabilityLoss() {
        return lockpickDurabilityLoss;
    }

    public Material getSwcoinMaterial() {
        return swcoinMaterial;
    }

    public String getSwcoinName() {
        return swcoinName;
    }

    public String getSwcoinLore() {
        return swcoinLore;
    }

    public String getAfkMessageOn() {
        return afkMessageOn;
    }

    public String getAfkMessageOff() {
        return afkMessageOff;
    }

    public boolean isBrickRewardEnabled() {
        return brickRewardEnabled;
    }

    public boolean isLaundryRewardEnabled() {
        return laundryRewardEnabled;
    }

    public boolean isKitchenRewardEnabled() {
        return kitchenRewardEnabled;
    }

    public EscapeManager getEscapeManager() {
        return escapeManager;
    }

    public BlackMarketManager getBlackMarketManager() {
        return blackMarketManager;
    }

    public ItemChecker getItemChecker() {
        return itemChecker;
    }

    public boolean isEscaping(Player player) {
        return escapeCountdown.containsKey(player);
    }

    // ================ ЗАГРУЗКА КОНФИГА ================
    private void loadConfig() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();

        // Флаги
        enablePvo = cfg.getBoolean("enable-pvo", false);
        enableEscapes = cfg.getBoolean("enable-escapes", true);
        enableWorkSystem = cfg.getBoolean("enable-work-system", true);
        enableLockpicking = cfg.getBoolean("enable-lockpicking", true);

        // Общие
        int afkTimeoutSeconds = cfg.getInt("general.afk-timeout", 60);
        if (afkTimeoutSeconds <= 0) {
            afkTimeout = 0;
            afkTimeMultiplier = 1;
        } else {
            afkTimeout = afkTimeoutSeconds * 1000L;
            afkTimeMultiplier = cfg.getInt("general.afk-time-multiplier", 3);
        }
        afkTimeMultiplier = cfg.getInt("general.afk-time-multiplier", 2);
        escapeTimeLimit = cfg.getInt("general.escape-time-limit", 900);
        escapeFailMultiplier = cfg.getDouble("general.escape-fail-multiplier", 2.0);
        
        ConfigurationSection spawnSec = getConfig().getConfigurationSection("general.spawn-point");
        if (spawnSec == null) {
            World defaultWorld = Bukkit.getWorld("world");
            if (defaultWorld != null) {
                spawnPoint = defaultWorld.getSpawnLocation();
            } else {
                getLogger().warning("Мир 'world' не найден, используется запасная точка освобождения.");
                spawnPoint = new Location(null, 10, 76, 10);
            }
        } else {
            String worldName = spawnSec.getString("world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                world = Bukkit.getWorld("world");
                getLogger().warning("Указанный мир '" + worldName + "' не найден, используется мир 'world'.");
            }
            double x = spawnSec.getDouble("x", 10);
            double y = spawnSec.getDouble("y", 76);
            double z = spawnSec.getDouble("z", 10);
            spawnPoint = new Location(world, x, y, z);
        }

        // ПВО
        areaCorner1 = loadLocation("pvo.area-corner1", Bukkit.getWorld("world"), 1258, 0, -1109);
        areaCorner2 = loadLocation("pvo.area-corner2", Bukkit.getWorld("world"), 1373, 256, -976);
        pvoWarningTime = cfg.getInt("pvo.warning-time", 1);
        pvoMissileStartSpeed = cfg.getDouble("pvo.missile-start-speed", 0.3);
        pvoMissileSpeedIncrement = cfg.getDouble("pvo.missile-speed-increment", 0.5);
        pvoMissileSpeedIncrementInterval = cfg.getInt("pvo.missile-speed-increment-interval", 50);
        pvoMissileMaxSpeed = cfg.getDouble("pvo.missile-max-speed", 3.0);
        pvoExplosionPower = (float) cfg.getDouble("pvo.explosion-power", 4.0);

        // Побег через табличку (для обратной совместимости)
        escapeShipEffectDuration = (int) (cfg.getDouble("escape-ship.effect-duration", 12.5) * 20);
        escapeShipTeleportDelay = cfg.getInt("escape-ship.teleport-delay", 10) * 20;
        escapeShipFinalMessageDelay = cfg.getInt("escape-ship.final-message-delay", 1) * 20;
        escapeShipDestination = loadLocation("escape-ship.destination", Bukkit.getWorld("world"), -1023, 66, -3180);

        // Работы (кирпичи)
        brickTimeReduction = cfg.getInt("work-brick.time-reduction", 20);
        brickLockpickChance = cfg.getDouble("work-brick.lockpick-chance", 5.0);
        brickSlownessAmplifier = cfg.getInt("work-brick.slowness-amplifier", 2);
        brickSlownessDuration = cfg.getInt("work-brick.slowness-duration", 4);
        brickSlownessInterval = cfg.getInt("work-brick.slowness-interval", 2);

        // Прачечная
        laundryTimeReduction = cfg.getInt("work-laundry.time-reduction", 20);
        laundryGuardArmorChance = cfg.getDouble("work-laundry.guard-armor-chance", 15.0);
        laundryWashDuration = cfg.getInt("work-laundry.wash-duration", 15) * 20;

        // Кухня
        kitchenTimeReduction = cfg.getInt("work-kitchen.time-reduction", 10);
        loadKitchenRecipes();

        // Награда за поимку
        captureRewardDivider = cfg.getInt("capture-reward.divider", 300);

        // Взлом
        lockpickCooldown = cfg.getInt("lockpick.cooldown", 5);
        lockpickSuccessChance = cfg.getDouble("lockpick.success-chance", 20.0);
        lockpickAttemptDelay = cfg.getInt("lockpick.attempt-delay", 3) * 20;
        lockpickDoorOpenTime = cfg.getInt("lockpick.door-open-time", 30) * 20;
        lockpickDurabilityLoss = cfg.getDouble("lockpick.durability-loss", 34.0);

        // SWcoin
        String matStr = cfg.getString("swcoin.material", "GOLD_NUGGET");
        swcoinMaterial = Material.getMaterial(matStr.toUpperCase());
        if (swcoinMaterial == null)
            swcoinMaterial = Material.GOLD_NUGGET;
        swcoinName = ChatColor.translateAlternateColorCodes('&', cfg.getString("swcoin.name", "&6SWcoin"));
        swcoinLore = ChatColor.translateAlternateColorCodes('&', cfg.getString("swcoin.lore", "&dТюремная валюта"));
        brickSwcoinWorkCount = cfg.getInt("swcoin.rewards.brick.work-count", 5);
        kitchenSwcoinWorkCount = cfg.getInt("swcoin.rewards.kitchen.work-count", 10);
        laundrySwcoinWorkCount = cfg.getInt("swcoin.rewards.laundry.work-count", 8);
        
        // AFK сообщения
        afkMessageOn = getLang().getMessage("afk.on");
        afkMessageOff = getLang().getMessage("afk.off");
        brickRewardEnabled = cfg.getBoolean("swcoin.rewards.brick.enabled", true);
        laundryRewardEnabled = cfg.getBoolean("swcoin.rewards.laundry.enabled", true);
        kitchenRewardEnabled = cfg.getBoolean("swcoin.rewards.kitchen.enabled", true);
    }

    private Location loadLocation(String path, World defaultWorld, double defX, double defY, double defZ) {
        ConfigurationSection sec = getConfig().getConfigurationSection(path);
        if (sec == null) {
            return new Location(defaultWorld, defX, defY, defZ);
        }
        String worldName = sec.getString("world", defaultWorld.getName());
        World world = Bukkit.getWorld(worldName);
        if (world == null)
            world = defaultWorld;
        double x = sec.getDouble("x", defX);
        double y = sec.getDouble("y", defY);
        double z = sec.getDouble("z", defZ);
        return new Location(world, x, y, z);
    }

    private void loadKitchenRecipes() {
        ConfigurationSection recipesSec = getConfig().getConfigurationSection("work-kitchen.recipes");
        if (recipesSec == null) {
            getLogger().warning("Раздел рецептов на кухне не найден, кухонные работы будут недоступны.");
            return;
        }
        for (String key : recipesSec.getKeys(false)) {
            ConfigurationSection sec = recipesSec.getConfigurationSection(key);
            String name = ChatColor.translateAlternateColorCodes('&', sec.getString("name", key));
            List<String> ingStr = sec.getStringList("ingredients");
            List<Material> ingredients = new ArrayList<>();
            for (String s : ingStr) {
                Material m = Material.getMaterial(s.toUpperCase());
                if (m != null)
                    ingredients.add(m);
                else
                    getLogger().warning("Неизвестный материал в рецепте " + key + ": " + s);
            }
            String resultStr = sec.getString("result");
            Material result = Material.getMaterial(resultStr.toUpperCase());
            if (result == null) {
                getLogger().warning("Неверный результат в рецепте " + key + ", рецепт пропущен.");
                continue;
            }
            List<Integer> timeRange = sec.getIntegerList("time");
            if (timeRange.size() < 2) {
                getLogger().warning("Рецепт " + key + " не содержит корректного диапазона времени, пропущен.");
                continue;
            }
            int minTime = timeRange.get(0);
            int maxTime = timeRange.get(1);
            int reward = sec.getInt("reward", kitchenTimeReduction);
            kitchenRecipes.put(key, new RecipeData(key, name, ingredients, result, minTime, maxTime, reward));
        }
        getLogger().info("Загружено рецептов на кухне: " + kitchenRecipes.size());
    }

    // ================ КОД ПЛАГИНА ================

    @Override
    public void onEnable() {
        instance = this;
        lang = new LanguageManager(this);
        loadConfig();
        escapeManager = new EscapeManager(this);
        blackMarketManager = new BlackMarketManager(this);
        itemChecker = new ItemChecker(this);

        getCommand("cell").setExecutor(this);
        getCommand("jailstatus").setExecutor(this);
        getCommand("jail").setExecutor(this);
        getCommand("unjail").setExecutor(this);
        getCommand("jtime").setExecutor(this);
        getCommand("buypassport").setExecutor(this);
        
        WorkBlockGUI workBlockGUI = new WorkBlockGUI(this);
        getCommand("jailtools").setExecutor(new WorkBlocksCommand(this, workBlockGUI));
        getServer().getPluginManager().registerEvents(workBlockGUI, this);
        getCommand("givemarketmap").setExecutor(this);
        getCommand("cell").setTabCompleter(new cellTabComplete(this));
        getCommand("jail").setTabCompleter(new jailTabComplete(this));
        getCommand("jtime").setTabCompleter(new jtimeTabComplete(this));
        getCommand("jailboard").setTabCompleter(new JailBoardTabComplete());
        getCommand("jailstatus").setTabCompleter(new jailstatusTabComplete());
        getCommand("jails").setTabCompleter(new JailsTabComplete());
        getCommand("jailtools").setTabCompleter(new JailToolsTabCompleter());
        jailLogger = new JailLogger(getDataFolder());
        createCellsConfig();
        createJailsConfig();
        loadJailData();
        startJailCheckTask();
        
        lockpickListener = new LockpickListener(this);
        getServer().getPluginManager().registerEvents(lockpickListener, this);
        
        historyFile = new File(getDataFolder(), "history.yml");
        if (!historyFile.exists()) {
            try {
                historyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        boardManager = new JailBoardManager(this);
        getServer().getPluginManager().registerEvents(new JailBoardListener(this), this);
        getServer().getPluginManager().registerEvents(new EscapeListener(this), this);

        getServer().getScheduler().runTaskLater(this, () -> {
            if (lockpickListener != null) {
                lockpickListener.cleanupAllPlayers();
            }
        }, 20L);
        
        historyConfig = YamlConfiguration.loadConfiguration(historyFile);
        getServer().getPluginManager().registerEvents(this, this);
        jailWorkListener = new JailWorkListener(this);
        getServer().getPluginManager().registerEvents(jailWorkListener, this);
        registerMapRecipe();
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onInteract(PlayerInteractEntityEvent e) {
                blackMarketManager.onVillagerInteract(e);
            }
        }, this);

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerInteract(PlayerInteractEvent e) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    if (blackMarketManager.onSpawnEggUse(e.getPlayer(), e.getClickedBlock())) {
                        e.setCancelled(true);
                    }
                }
            }
        }, this);
    }

@EventHandler
public void onCraftItem(CraftItemEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    Player player = (Player) event.getWhoClicked();

    if (event.getInventory().getType() != InventoryType.WORKBENCH) return;

    ItemStack result = event.getCurrentItem();
    if (result == null || result.getType() != Material.FILLED_MAP) return;

    String mapName = getConfig().getString("blackmarket.map-settings.name");
    if (mapName == null) return;
    String coloredName = ChatColor.translateAlternateColorCodes('&', mapName);

    if (!result.hasItemMeta() || !result.getItemMeta().hasDisplayName() ||
            !result.getItemMeta().getDisplayName().equals(coloredName)) {
        return;
    }

    ItemStack[] matrix = event.getInventory().getMatrix();
    for (ItemStack item : matrix) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta() ||
                !item.getItemMeta().hasDisplayName() ||
                !item.getItemMeta().getDisplayName().equals("§7Обрывок карты")) {
            return; 
        }
    }

    event.setCancelled(true);

    for (int i = 0; i < matrix.length; i++) {
        ItemStack item = matrix[i];
        if (item != null) {
            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() <= 0) {
                event.getInventory().setItem(i, null);
            }
        }
    }

    player.updateInventory();

    blackMarketManager.giveRealTreasureMap(player);
}
    @Override
    public void onDisable() {
        if (boardManager != null) {
            boardManager.stopAutoUpdate();
        }
    }

    public static Jails getInstance() {
        return instance;
    }

    private void saveHistory() {
        try {
            historyConfig.save(historyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addHistoryEntry(String player, String initiator, int minutes, String cell, String reason,
            String status) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) historyConfig.getList("history." + player,
                new ArrayList<>());
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("date", new Date().toString());
        entry.put("timestamp", System.currentTimeMillis());
        entry.put("initiator", initiator);
        entry.put("player", player);
        entry.put("minutes", minutes);
        entry.put("cell", cell);
        entry.put("reason", reason);
        entry.put("status", status);
        list.add(entry);
        historyConfig.set("history." + player, list);
        saveHistory();
    }

    public void reduceJailTime(String playerName, int seconds) {
        JailData jailData = jailedPlayers.get(playerName);
        if (jailData != null) {
            int newTime = Math.max(0, jailData.getTime() - seconds);
            jailData.setTime(newTime);
            jailsConfig.set("jails." + playerName + ".time", newTime);
            saveJailsConfig();
        }
    }

public ItemStack getBoardConfigurator() {
    ItemStack item = new ItemStack(Material.STICK);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(getLang().getMessage("items.configurator-stick"));
    meta.setLore(Arrays.asList(
            getLang().getMessage("items.configurator-lore.1"),
            getLang().getMessage("items.configurator-lore.2")));
    item.setItemMeta(meta);
    return item;
}

    public JailBoardManager getBoardManager() {
        return boardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // ================ КОМАНДЫ, ДОСТУПНЫЕ ВСЕМ ================
        if (command.getName().equalsIgnoreCase("jtime") || command.getName().equalsIgnoreCase("jt")) {
            if (args.length == 0) {
                showJailTime(sender);
                return true;
            }

            boolean isAdmin = sender.isOp() || sender.hasPermission("jails.admin");
            String first = args[0].toLowerCase();

            if (first.equals("add") || first.equals("remove") || first.equals("set")) {
                if (!isAdmin) {
                    sender.sendMessage(Jails.getInstance().getLang().getMessage("general.no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(getLang().getMessage("jtime.admin.usage-" + first));
                    return true;
                }
                String playerName = args[1];
                int seconds;
                try {
                    seconds = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(getLang().getMessage("jtime.admin.seconds-number"));
                    return true;
                }
                switch (first) {
                    case "add":
                        addJailTime(sender, playerName, seconds);
                        break;
                    case "remove":
                        removeJailTime(sender, playerName, seconds);
                        break;
                    case "set":
                        setJailTime(sender, playerName, seconds);
                        break;
                }
                return true;
            }

            if (isAdmin) {
                showOtherJailTime(sender, args[0]);
            } else {
                sender.sendMessage(getLang().getMessage("general.unknown-command"));
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("jailstatus")) {
            showJailStatus(sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("jails")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.isOp() && !sender.hasPermission("jails.admin")) {
                    sender.sendMessage(Jails.getInstance().getLang().getMessage("general.no-permission"));
                    return true;
                }
                reloadConfig();
                loadConfig();
                if (lang != null) lang.reload();
                if (lockpickListener != null) {
                    lockpickListener.reloadConfig();
                }
                if (escapeManager != null) {
                    escapeManager.loadZones();
                }
                if (blackMarketManager != null) {
                    blackMarketManager.loadConfig();
                }
                sender.sendMessage(getLang().getMessage("general.config-reloaded"));
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(getLang().getMessage("general.command-only-player"));
                return true;
            }
            Player player = (Player) sender;
            if (args.length == 0) {
                sendJailInfo(player);
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "workinfo":
                case "work":
                case "works":
                    sendWorkDetails(player);
                    break;
                case "escapeinfo":
                case "escape":
                    sendEscapeDetails(player);
                    break;
                case "rules":
                    sendRulesDetails(player);
                    break;
                default:
                    sendJailInfo(player);
            }
            return true;
        }

        // Команда /buypassport
        if (command.getName().equalsIgnoreCase("buypassport")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getLang().getMessage("general.command-only-player"));
                return true;
            }
            
            Player player = (Player) sender;
            if (!blackMarketManager.buyPassport(player)) {
            
            }
            return true;
        }

        // Команда /jailhistory
        if (command.getName().equalsIgnoreCase("jailhistory")) {
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(
                            "§cДля просмотра своей истории из консоли укажите ник игрока: /jailhistory <игрок>");
                    return true;
                }
                showJailHistory((Player) sender, sender.getName(), 1);
                return true;
            }
            String target = args[0];
            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(getLang().getMessage("jailhistory.page-number"));
                    return true;
                }
            }
            boolean canViewOthers = sender.isOp() || sender.hasPermission("jails.history.others");
            if (!target.equals(sender.getName()) && !canViewOthers) {
                sender.sendMessage(getLang().getMessage("general.no-permission"));
                return true;
            }
            if (!(sender instanceof Player)) {
                Player targetPlayer = Bukkit.getPlayer(target);
                if (targetPlayer == null) {
                    sender.sendMessage(getLang().getMessage("general.player-not-found"));
                    return true;
                }
                showJailHistory(targetPlayer, target, page);
            } else {
                showJailHistory((Player) sender, target, page);
            }
            return true;
        }

        // Команда /jail
        if (command.getName().equalsIgnoreCase("jail")) {
            if (!sender.isOp() && !sender.hasPermission("jails.jail")) {
                sender.sendMessage(Jails.getInstance().getLang().getMessage("general.no-permission"));
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage(getLang().getMessage("jail.usage"));
                return true;
            }
            String playerName = args[0];
            int minutes;
            try {
                minutes = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(getLang().getMessage("jail.minutes-number"));
                return true;
            }
            String cellName = args[2];
            StringBuilder reason = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                if (i > 3)
                    reason.append(" ");
                reason.append(args[i]);
            }
            jailPlayer(sender, playerName, minutes, cellName, reason.toString());
            return true;
        }

        // Команда /unjail
        if (command.getName().equalsIgnoreCase("unjail")) {
            if (!sender.isOp() && !sender.hasPermission("jails.unjail")) {
                sender.sendMessage(Jails.getInstance().getLang().getMessage("general.no-permission"));
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(getLang().getMessage("unjail.usage"));
                return true;
            }
            unjailPlayer(sender, args[0], false);
            return true;
        }

        // Команды управления камерами
        if (command.getName().equalsIgnoreCase("cell") || command.getName().equalsIgnoreCase("delcell")) {
            if (!sender.isOp() && !sender.hasPermission("jails.admin")) {
                sender.sendMessage(Jails.getInstance().getLang().getMessage("general.no-permission"));
                return true;
            }
            if (command.getName().equalsIgnoreCase("delcell")) {
                if (args.length != 1) {
                    sender.sendMessage(getLang().getMessage("cell.delete.usage"));
                    return true;
                }
                deleteCell(sender, args[0]);
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(getLang().getMessage("cell.help.0"));
                sender.sendMessage(getLang().getMessage("cell.help.1"));
                sender.sendMessage(getLang().getMessage("cell.help.2"));
                sender.sendMessage(getLang().getMessage("cell.help.3"));
                return true;
            }
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "create":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(getLang().getMessage("general.command-only-player"));
                        return true;
                    }
                    if (args.length != 2) {
                        sender.sendMessage("§cИспользование: /cell create <name>");
                        return true;
                    }
                    createCell((Player) sender, args[1]);
                    break;
                case "delete":
                    if (args.length != 2) {
                        sender.sendMessage(getLang().getMessage("cell.delete.usage"));
                        return true;
                    }
                    deleteCell(sender, args[1]);
                    break;
                case "rename":
                    if (args.length != 3) {
                        sender.sendMessage(getLang().getMessage("cell.rename.usage"));
                        return true;
                    }
                    renameCell(sender, args[1], args[2]);
                    break;
                default:
                    sender.sendMessage(getLang().getMessage("general.unknown-command"));
            }
            return true;
        }

        // Команда /givepick
        if (command.getName().equalsIgnoreCase("givepick")) {
            if (!sender.isOp() && !sender.hasPermission("jails.admin")) {
                sender.sendMessage(Jails.getInstance().getLang().getMessage("general.no-permission"));
                return true;
            }
            Player target;
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(getLang().getMessage("givepick.usage"));
                    return true;
                }
                target = (Player) sender;
            } else {
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(getLang().getMessage("general.player-not-found"));
                    return true;
                }
            }
            ItemStack lockpick = itemChecker.createItem("items.lockpick");
            if (lockpick != null) {
                target.getInventory().addItem(lockpick);
                sender.sendMessage(getLang().getMessage("givepick.success", "player", target.getName()));
            } else {
                sender.sendMessage(getLang().getMessage("givepick.fail"));
            }
            return true;
        }

        // Команда /givearmour
        if (command.getName().equalsIgnoreCase("givearmour")) {
            if (!sender.isOp() && !sender.hasPermission("jails.admin")) {
                sender.sendMessage(Jails.getInstance().getLang().getMessage("general.no-permission"));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(getLang().getMessage("givearmour.usage"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(getLang().getMessage("general.player-not-found"));
                return true;
            }
            giveGuardArmor(target);
            sender.sendMessage(getLang().getMessage("givearmour.success", "player", target.getName()));
            return true;
        }


if (command.getName().equalsIgnoreCase("givemarketmap")) {
    if (!sender.isOp() && !sender.hasPermission("jails.admin")) {
        sender.sendMessage(Jails.getInstance().getLang().getMessage("general.no-permission"));
        return true;
    }

    Player target;
    if (args.length == 0) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getLang().getMessage("givemarketmap.usage"));
            return true;
        }
        target = (Player) sender;
    } else {
        target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getLang().getMessage("general.player-not-found"));
            return true;
        }
    }

    blackMarketManager.giveRealTreasureMap(target);
    return true;
}
        // Команда /givetable
        if (command.getName().equalsIgnoreCase("givetable")) {
            if (!sender.isOp() && !sender.hasPermission("jails.admin")) {
                sender.sendMessage(Jails.getInstance().getLang().getMessage("general.no-permission"));
                return true;
            }
            Player target;
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(getLang().getMessage("givetable.usage"));
                    return true;
                }
                target = (Player) sender;
            } else {
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(getLang().getMessage("general.player-not-found"));
                    return true;
                }
            }
            
            ItemStack table = null;
            Map<String, EscapeManager.EscapeZone> zones = escapeManager.getEscapeZones();
            if (!zones.isEmpty()) {
                table = zones.values().iterator().next().getInteractItem();
            }
            
            if (table == null) {
                table = new ItemStack(Material.OAK_SIGN);
                ItemMeta meta = table.getItemMeta();
                meta.setDisplayName("§6Уплыть");
                table.setItemMeta(meta);
            }
            
            target.getInventory().addItem(table);
            	sender.sendMessage(getLang().getMessage("givetable.success", "player", target.getName()));
            return true;
        }

        // Команда /jailboard
        if (command.getName().equalsIgnoreCase("jailboard")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getLang().getMessage("general.command-only-player"));
                return false;
            }
            Player player = (Player) sender;
            
            if (!player.isOp()) {
                player.sendMessage(getLang().getMessage("general.no-permission"));
                return true;
            }
            if (args.length == 0) {
                player.sendMessage(getLang().getMessage("jailboard.help.0"));
                player.sendMessage(getLang().getMessage("jailboard.help.1"));
                player.sendMessage(getLang().getMessage("jailboard.help.2"));
                player.sendMessage(getLang().getMessage("jailboard.help.3"));
                	player.sendMessage(getLang().getMessage("jailboard.help.4"));
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "create":
                    boardManager.enableSelectionMode(player);
                    break;
                case "confirm":
                    boardManager.confirmBoard(player);
                    break;
                case "cancel":
                    boardManager.cancelBoard(player);
                    break;
                case "clear":
                    boardManager.clearSelection(player);
                    break;
                default:
                    player.sendMessage(getLang().getMessage("general.unknown-command"));
            }
            return true;
        }

        return false;
    }

    // ================ МЕТОДЫ ДЛЯ ИНФОРМАЦИОННОГО МЕНЮ ================
    private void sendCenteredMessage(Player player, String message) {
        int maxLength = 60;
        String stripped = ChatColor.stripColor(message);
        int padding = (maxLength - stripped.length()) / 2;
        player.sendMessage(" ".repeat(Math.max(0, padding)) + message);
    }

    private void sendClickableSection(Player player, String title, String command, String hoverText) {
        TextComponent component = new TextComponent("§7[ §6" + title + " §7] ");
        component.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, command));
        component.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.TextComponent[] {
                        new net.md_5.bungee.api.chat.TextComponent(hoverText) }));
        player.spigot().sendMessage(component);
    }

    private void sendCommandWithHover(Player player, String prefix, String command,
            String hoverText, String suffix, boolean showBrackets) {
        TextComponent message = new TextComponent(prefix);
        String cmdDisplay = showBrackets ? "§6/" + command : "§c/" + command;
        TextComponent cmdComponent = new TextComponent(cmdDisplay);
        cmdComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, "/" + command));
        cmdComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.TextComponent[] {
                        new net.md_5.bungee.api.chat.TextComponent(hoverText) }));
        message.addExtra(cmdComponent);
        if (!suffix.isEmpty()) {
            message.addExtra(new TextComponent(suffix));
        }
        player.spigot().sendMessage(message);
    }

    private void sendJailInfo(Player player) {
    JailData jailData = jailedPlayers.get(player.getName());
    int timeInSeconds = jailData != null ? jailData.getTime() : 0;
    int minutes = timeInSeconds / 60;
    int seconds = timeInSeconds % 60;
    String timeStr = minutes + " мин " + seconds + " сек";

    for (int i = 0; i < 3; i++)
        player.sendMessage("");
    sendCenteredMessage(player, getLang().getMessage("jails.help.header"));
    player.sendMessage("");
    sendCenteredMessage(player, getLang().getMessage("jails.help.main-info-title"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.help.you-are-jailed"));
    sendCommandWithHover(player,
            getLang().getMessage("jails.help.time-line-prefix", "time", timeStr) + " (",
            "jtime",
            getLang().getMessage("jails.help.check-time-hover"),
            ")",
            true);
    player.sendMessage(getLang().getMessage("jails.help.reduce-time-ways"));
    player.sendMessage(getLang().getMessage("jails.help.escape-risky"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.help.reduce-ways-title"));
    player.sendMessage(getLang().getMessage("jails.help.work-reward-template", "seconds", brickTimeReduction));
    sendCommandWithHover(player,
            getLang().getMessage("jails.help.afk-slower-text-prefix"),
            "jails rules",
            getLang().getMessage("jails.help.afk-hover"),
            ")",
            true);
    player.sendMessage("");
    sendCommandWithHover(player,
            getLang().getMessage("jails.help.check-time-click"),
            "jtime",
            getLang().getMessage("jails.help.check-time-hover-long"),
            "",
            false);
    sendCommandWithHover(player,
            getLang().getMessage("jails.help.afk-multiplier-template", "multiplier", afkTimeMultiplier) + " (",
            "jails rules",
            getLang().getMessage("jails.help.rules-hover"),
            ")",
            true);
    sendClickableSection(player, 
            getLang().getMessage("jails.help.clickable.works"),
            "/jails workinfo",
            getLang().getMessage("jails.help.works-hover", "seconds", brickTimeReduction));
    sendClickableSection(player, 
            getLang().getMessage("jails.help.clickable.escape"),
            "/jails escapeinfo",
            getLang().getMessage("jails.help.escape-hover", "multiplier", (int) escapeFailMultiplier));
    sendClickableSection(player, 
            getLang().getMessage("jails.help.clickable.rules"),
            "/jails rules",
            getLang().getMessage("jails.help.rules-hover"));
    player.sendMessage("");
    sendCenteredMessage(player, getLang().getMessage("jails.help.footer"));
}

    private void sendWorkDetails(Player player) {
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    for (int i = 0; i < 3; i++)
        player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.work-details.title"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.work-details.bricks.name"));
    player.sendMessage(getLang().getMessage("jails.work-details.bricks.desc"));
    player.sendMessage(getLang().getMessage("jails.work-details.bricks.reward", "seconds", brickTimeReduction));
    player.sendMessage(getLang().getMessage("jails.work-details.bricks.extra"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.work-details.laundry.name"));
    player.sendMessage(getLang().getMessage("jails.work-details.laundry.desc"));
    player.sendMessage(getLang().getMessage("jails.work-details.laundry.reward", "seconds", laundryTimeReduction));
    player.sendMessage(getLang().getMessage("jails.work-details.laundry.extra"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.work-details.kitchen.name"));
    player.sendMessage(getLang().getMessage("jails.work-details.kitchen.desc"));
    player.sendMessage(getLang().getMessage("jails.work-details.kitchen.reward", "seconds", kitchenTimeReduction));
    player.sendMessage(getLang().getMessage("jails.work-details.kitchen.extra", "swcoin", ChatColor.stripColor(swcoinName)));
    player.sendMessage("");
    sendClickableBackButton(player);
}

    private void renameCell(CommandSender sender, String oldName, String newName) {
    if (!cellsConfig.contains("cells." + oldName)) {
        sender.sendMessage(getLang().getMessage("cell.rename.not-found", "old", oldName));
        return;
    }
    if (cellsConfig.contains("cells." + newName)) {
        sender.sendMessage(getLang().getMessage("cell.rename.already-exists", "new", newName));
        return;
    }

    for (String jailed : jailedPlayers.keySet()) {
        String cell = jailsConfig.getString("jails." + jailed + ".cell");
        if (oldName.equals(cell)) {
            sender.sendMessage(getLang().getMessage("cell.rename.has-prisoners"));
            return;
        }
    }

    ConfigurationSection oldSection = cellsConfig.getConfigurationSection("cells." + oldName);
    cellsConfig.set("cells." + newName, oldSection);
    cellsConfig.set("cells." + oldName, null);
    saveCellsConfig();

    sender.sendMessage(getLang().getMessage("cell.rename.success", "old", oldName, "new", newName));
}

    private void addJailTime(CommandSender sender, String playerName, int seconds) {
    if (!jailedPlayers.containsKey(playerName)) {
        sender.sendMessage(getLang().getMessage("jtime.other-not-jailed", "player", playerName));
        return;
    }
    JailData data = jailedPlayers.get(playerName);
    int newTime = data.getTime() + seconds;
    data.setTime(newTime);
    jailsConfig.set("jails." + playerName + ".time", newTime);
    saveJailsConfig();
    sender.sendMessage(getLang().getMessage("jtime.admin.add-success", 
            "seconds", seconds, "player", playerName, "time", formatTime(newTime)));

    Player target = Bukkit.getPlayer(playerName);
    if (target != null) {
        target.sendMessage(getLang().getMessage("jtime.target-notified-add", 
                "seconds", seconds, "time", formatTime(newTime)));
    }
}

    private void removeJailTime(CommandSender sender, String playerName, int seconds) {
    if (!jailedPlayers.containsKey(playerName)) {
        sender.sendMessage(getLang().getMessage("jtime.other-not-jailed", "player", playerName));
        return;
    }
    JailData data = jailedPlayers.get(playerName);
    int newTime = Math.max(0, data.getTime() - seconds);
    data.setTime(newTime);
    jailsConfig.set("jails." + playerName + ".time", newTime);
    saveJailsConfig();
    sender.sendMessage(getLang().getMessage("jtime.admin.remove-success", 
            "seconds", seconds, "player", playerName, "time", formatTime(newTime)));

    Player target = Bukkit.getPlayer(playerName);
    if (target != null) {
        target.sendMessage(getLang().getMessage("jtime.target-notified-remove", 
                "seconds", seconds, "time", formatTime(newTime)));
    }
}

    private void setJailTime(CommandSender sender, String playerName, int seconds) {
    if (!jailedPlayers.containsKey(playerName)) {
        sender.sendMessage(getLang().getMessage("jtime.other-not-jailed", "player", playerName));
        return;
    }
    JailData data = jailedPlayers.get(playerName);
    data.setTime(seconds);
    jailsConfig.set("jails." + playerName + ".time", seconds);
    saveJailsConfig();
    sender.sendMessage(getLang().getMessage("jtime.admin.set-success", 
            "time", formatTime(seconds), "player", playerName));

    Player target = Bukkit.getPlayer(playerName);
    if (target != null) {
        target.sendMessage(getLang().getMessage("jtime.target-notified-set", "time", formatTime(seconds)));
    }
}

    private void showOtherJailTime(CommandSender sender, String playerName) {
    if (!jailedPlayers.containsKey(playerName)) {
        sender.sendMessage(getLang().getMessage("jtime.other-not-jailed", "player", playerName));
        return;
    }
    int time = jailedPlayers.get(playerName).getTime();
    int min = time / 60;
    int sec = time % 60;
    sender.sendMessage(getLang().getMessage("jtime.other", "player", playerName, "minutes", min, "seconds", sec));
}

    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return min + " мин " + sec + " сек";
    }

    private void sendEscapeDetails(Player player) {
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    for (int i = 0; i < 3; i++)
        player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.escape-details.title"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.escape-details.requirements"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.escape-details.lockpick"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.escape-details.armor"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.escape-details.ticket", "swcoin", ChatColor.stripColor(swcoinName)));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.escape-details.destination"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.escape-details.warning-title"));
    player.sendMessage(getLang().getMessage("jails.escape-details.warning-time", "minutes", (escapeTimeLimit / 60)));
    player.sendMessage(getLang().getMessage("jails.escape-details.warning-caught", "multiplier", (int) escapeFailMultiplier));
    player.sendMessage(getLang().getMessage("jails.escape-details.warning-reward", "swcoin", ChatColor.stripColor(swcoinName)));
    player.sendMessage("");
    sendClickableBackButton(player);
}

    private void sendRulesDetails(Player player) {
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    for (int i = 0; i < 3; i++)
        player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.rules-details.title"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.rules-details.basic.header"));
    player.sendMessage(getLang().getMessage("jails.rules-details.basic.gamemode"));
    player.sendMessage(getLang().getMessage("jails.rules-details.basic.teleport"));
    player.sendMessage(getLang().getMessage("jails.rules-details.basic.portal"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.rules-details.time.header"));
    player.sendMessage(getLang().getMessage("jails.rules-details.time.normal"));

    if (afkTimeout > 0) {
        player.sendMessage(getLang().getMessage("jails.rules-details.time.afk", "multiplier", afkTimeMultiplier));
    } else {
        player.sendMessage(getLang().getMessage("jails.rules-details.time.afk-disabled"));
    }

    player.sendMessage(getLang().getMessage("jails.rules-details.time.check"));
    player.sendMessage("");
    player.sendMessage(getLang().getMessage("jails.rules-details.penalties.header"));
    player.sendMessage(getLang().getMessage("jails.rules-details.penalties.work", "seconds", brickTimeReduction));
    player.sendMessage(getLang().getMessage("jails.rules-details.penalties.escape-fail", "multiplier", (int) escapeFailMultiplier));
    player.sendMessage(getLang().getMessage("jails.rules-details.penalties.capture", "swcoin", ChatColor.stripColor(swcoinName)));
    player.sendMessage("");

    if (afkTimeout > 0) {
        player.sendMessage(getLang().getMessage("jails.rules-details.afk-info.header"));
        player.sendMessage(getLang().getMessage("jails.rules-details.afk-info.desc"));
        player.sendMessage(getLang().getMessage("jails.rules-details.afk-info.timeout", "seconds", (afkTimeout / 1000)));
        player.sendMessage(getLang().getMessage("jails.rules-details.afk-info.auto"));
        player.sendMessage("");
    }

    sendClickableBackButton(player);
}

private void sendClickableBackButton(Player player) {
    TextComponent back = new TextComponent(getLang().getMessage("jails.rules-details.back-button"));
    back.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
            net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/jails help"));
    back.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
            net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
            new net.md_5.bungee.api.chat.TextComponent[] {
                    new net.md_5.bungee.api.chat.TextComponent(getLang().getMessage("jails.rules-details.back-hover")) }));
    player.spigot().sendMessage(back);
}

    private void showJailHistory(Player player, String target, int page) {
    List<Map<String, Object>> history = (List<Map<String, Object>>) historyConfig.getList("history." + target,
            new ArrayList<>());

    if (history.isEmpty()) {
        player.sendMessage(getLang().getMessage("jailhistory.no-history", "player", target));
        return;
    }

    history.sort((a, b) -> {
        if (a.containsKey("timestamp") && b.containsKey("timestamp")) {
            long timeA = ((Number) a.get("timestamp")).longValue();
            long timeB = ((Number) b.get("timestamp")).longValue();
            return Long.compare(timeB, timeA);
        }

        try {
            String dateA = a.getOrDefault("date", "").toString();
            String dateB = b.getOrDefault("date", "").toString();

            SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
            Date dA = format.parse(dateA);
            Date dB = format.parse(dateB);
            return dB.compareTo(dA);
        } catch (Exception e) {
            String dateA = a.getOrDefault("date", "").toString();
            String dateB = b.getOrDefault("date", "").toString();
            return dateB.compareTo(dateA);
        }
    });

    int itemsPerPage = 5;
    int totalPages = (int) Math.ceil(history.size() / (double) itemsPerPage);
    int totalEntries = history.size();

    if (page < 1)
        page = 1;
    if (page > totalPages)
        page = totalPages;

    int startIndex = (page - 1) * itemsPerPage;
    int endIndex = Math.min(startIndex + itemsPerPage, totalEntries);

    player.sendMessage(getLang().getMessage("jailhistory.header", "player", target));
    player.sendMessage(getLang().getMessage("jailhistory.page-info", "page", page, "total", totalPages, "totalEntries", totalEntries));

    for (int i = startIndex; i < endIndex; i++) {
        Map<String, Object> entry = history.get(i);
        String date = entry.getOrDefault("date", "?").toString();

        String playerName = entry.containsKey("player") ? entry.get("player").toString()
                : entry.getOrDefault("admin", "?").toString();

        String initiator = entry.getOrDefault("initiator", "?").toString();
        int minutes = (int) entry.getOrDefault("minutes", 0);
        String cell = entry.getOrDefault("cell", "?").toString();
        String reason = entry.getOrDefault("reason", "").toString();

        String formattedDate = formatDate(date);
        int recordNumber = i + 1;

        player.sendMessage(getLang().getMessage("jailhistory.entry-line",
                "number", recordNumber, "date", formattedDate, "initiator", initiator,
                "player", playerName, "minutes", minutes, "cell", cell));
        player.sendMessage(getLang().getMessage("jailhistory.reason-line", "reason", reason));
    }

    TextComponent navigation = new TextComponent("");

    if (page > 1) {
        TextComponent prev = new TextComponent(getLang().getMessage("jailhistory.navigation-prev"));
        prev.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                "/jailhistory " + target + " " + (page - 1)));
        prev.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.TextComponent[] {
                        new net.md_5.bungee.api.chat.TextComponent(getLang().getMessage("jailhistory.navigation-prev-hover")) }));
        navigation.addExtra(prev);
    }

    navigation.addExtra(getLang().getMessage("jailhistory.navigation-page", "page", page, "total", totalPages));

    if (page < totalPages) {
        TextComponent next = new TextComponent(getLang().getMessage("jailhistory.navigation-next"));
        next.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                "/jailhistory " + target + " " + (page + 1)));
        next.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.TextComponent[] {
                        new net.md_5.bungee.api.chat.TextComponent(getLang().getMessage("jailhistory.navigation-next-hover")) }));
        navigation.addExtra(next);
    }

    player.spigot().sendMessage(navigation);
}

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.equals("?"))
            return "?";
        try {
            String[] parts = dateStr.split(" ");
            if (parts.length >= 5) {
                String engMonth = parts[1];
                String day = parts[2];
                String time = parts[3].substring(0, 5);
                String rusMonth = convertMonthToRussian(engMonth);

                return rusMonth + " " + day + " " + time;
            }
            return dateStr.length() > 16 ? dateStr.substring(0, 16) : dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String convertMonthToRussian(String engMonth) {
        switch (engMonth.toLowerCase()) {
            case "jan":
                return "янв";
            case "feb":
                return "фев";
            case "mar":
                return "мар";
            case "apr":
                return "апр";
            case "may":
                return "май";
            case "jun":
                return "июн";
            case "jul":
                return "июл";
            case "aug":
                return "авг";
            case "sep":
                return "сен";
            case "oct":
                return "окт";
            case "nov":
                return "ноя";
            case "dec":
                return "дек";
            default:
                return engMonth;
        }
    }

    // ================ МЕТОДЫ ДЛЯ БРОНИ НАДЗИРАТЕЛЯ ================
    private void giveGuardArmor(Player player) {
        ItemStack helmet = itemChecker.createItem("items.guard-armor.helmet");
        ItemStack chest = itemChecker.createItem("items.guard-armor.chestplate");
        ItemStack legs = itemChecker.createItem("items.guard-armor.leggings");
        ItemStack boots = itemChecker.createItem("items.guard-armor.boots");
        
        if (helmet != null) player.getInventory().addItem(helmet);
        if (chest != null) player.getInventory().addItem(chest);
        if (legs != null) player.getInventory().addItem(legs);
        if (boots != null) player.getInventory().addItem(boots);
    }

    // ================ СИСТЕМА ПВО ================
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enablePvo)
            return;
        Player player = event.getPlayer();
        Location playerLocation = player.getLocation();

        if (!player.isGliding()) {
            cancelMissileTask(player);
            return;
        }

        if (isWithinProtectedArea(playerLocation)) {
            if (!missileTasks.containsKey(player.getUniqueId())) {
                launchMissile(player);
            }
        } else {
            cancelMissileTask(player);
        }
    }

    private void launchMissile(Player player) {
        if (!enablePvo)
            return;
        UUID playerId = player.getUniqueId();
        player.sendMessage("§eПВО: Вы вошли в охраняемую территорию. покиньте территорию иначе через " + pvoWarningTime
                + " секунды будут запущены ракеты!");
        player.sendTitle("§cТревога!", "§eПокиньте территорию!", 10, 70, 20);

        BukkitRunnable missileTask = new BukkitRunnable() {
            Location missileLocation = player.getLocation().clone().add(0, -20, 0);
            int ticks = 0;
            double speed = pvoMissileStartSpeed;
            boolean chasingPlayer = false;

            @Override
            public void run() {
                if (ticks < pvoWarningTime * 20) {
                    missileLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, missileLocation, 10, 0.5, 0.5, 0.5,
                            0.05);
                    missileLocation.getWorld().playSound(missileLocation, Sound.BLOCK_PISTON_EXTEND, 1.0F, 1.0F);
                    missileLocation.add(0, 0.5, 0);
                } else if (ticks == pvoWarningTime * 20) {
                    missileLocation.getWorld().playSound(missileLocation, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0F,
                            1.0F);
                    chasingPlayer = true;
                }

                if (chasingPlayer) {
                    Location playerLoc = player.getLocation();
                    Location predicted = playerLoc.clone().add(player.getVelocity().multiply(5));
                    double dx = predicted.getX() - missileLocation.getX();
                    double dy = predicted.getY() - missileLocation.getY();
                    double dz = predicted.getZ() - missileLocation.getZ();
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance > 0) {
                        if (ticks % pvoMissileSpeedIncrementInterval == 0) {
                            speed = Math.min(pvoMissileMaxSpeed, speed + pvoMissileSpeedIncrement);
                        }
                        missileLocation.add(
                                dx / distance * speed,
                                dy / distance * speed,
                                dz / distance * speed);
                    }
                    spawnMissileParticles(missileLocation, 15);
                    if (missileLocation.distance(playerLoc) < 1.5) {
                        createExplosion(player);
                        cancel();
                        missileTasks.remove(playerId);
                    }
                }
                ticks++;
            }
        };
        missileTask.runTaskTimer(this, 20L, 1L);
        missileTasks.put(playerId, missileTask);
    }

    private void cancelMissileTask(Player player) {
        BukkitRunnable task = missileTasks.remove(player.getUniqueId());
        if (task != null)
            task.cancel();
    }

    private void createExplosion(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world != null) {
            world.createExplosion(loc, pvoExplosionPower, false, false);
        }
        killedByPVO.put(player.getUniqueId(), true);
    }

    private void spawnMissileParticles(Location loc, int count) {
        World world = loc.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.FLAME, loc, count, 0.1, 0.1, 0.1, 0.02);
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, count / 2, 0.1, 0.1, 0.1, 0.02);
            world.spawnParticle(Particle.EXPLOSION, loc, count / 5, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private boolean isWithinProtectedArea(Location loc) {
        double x1 = Math.min(areaCorner1.getX(), areaCorner2.getX());
        double y1 = Math.min(areaCorner1.getY(), areaCorner2.getY());
        double z1 = Math.min(areaCorner1.getZ(), areaCorner2.getZ());
        double x2 = Math.max(areaCorner1.getX(), areaCorner2.getX());
        double y2 = Math.max(areaCorner1.getY(), areaCorner2.getY());
        double z2 = Math.max(areaCorner1.getZ(), areaCorner2.getZ());

        return loc.getX() >= x1 && loc.getX() <= x2 &&
                loc.getY() >= y1 && loc.getY() <= y2 &&
                loc.getZ() >= z1 && loc.getZ() <= z2;
    }

    @EventHandler
    public void onPlayerDeathByPVO(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (killedByPVO.getOrDefault(player.getUniqueId(), false)) {
            event.setDeathMessage("§cИгрок " + player.getName() + " был уничтожен системой ПВО!");
            killedByPVO.remove(player.getUniqueId());
        }
    }

    // ================ ТАБЛИЧКА ПОБЕГА (для обратной совместимости) ================
@EventHandler
public void onSignChange(SignChangeEvent event) {
    Player player = event.getPlayer();
    if (player.getInventory().getItemInMainHand().hasItemMeta()) {
        ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
        if (meta != null && meta.hasDisplayName()
                && meta.getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', "&6Уплыть"))) {
            event.setLine(0, ChatColor.translateAlternateColorCodes('&', getLang().getMessage("misc.sign-escape-line1")));
            event.setLine(1, ChatColor.translateAlternateColorCodes('&', getLang().getMessage("misc.sign-escape-line2")));
            event.setLine(2, ChatColor.translateAlternateColorCodes('&', getLang().getMessage("misc.sign-escape-line3")));
            event.setLine(3, "");
            Sign sign = (Sign) event.getBlock().getState();
            sign.update();
        }
    }
}
@EventHandler
public void onPlayerInteractSign(PlayerInteractEvent event) {
    if (!enableEscapes)
        return;
    Player player = event.getPlayer();
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
        Block block = event.getClickedBlock();
        if (block != null && (block.getType() == Material.OAK_SIGN || block.getType() == Material.OAK_WALL_SIGN)) {
            Sign sign = (Sign) block.getState();
            if (sign.getLine(0).equals(ChatColor.translateAlternateColorCodes('&', getLang().getMessage("misc.sign-escape-line1")))) {
                event.setCancelled(true);
                
                EscapeManager.EscapeZone zone = escapeManager.getZoneAt(block.getLocation());
                if (zone != null) {
                    return;
                }
                
                if (!jailedPlayers.containsKey(player.getName())) {
                    player.sendMessage(getLang().getMessage("misc.not-prisoner"));
                    return;
                }
                
                if (!escapeManager.hasPassport(player)) {
                    player.sendMessage(getLang().getMessage("misc.escape-no-ticket"));
                    return;
                }
                
                Location dest = null;
                if (!escapeManager.getEscapeZones().isEmpty()) {
                    EscapeManager.EscapeZone firstZone = escapeManager.getEscapeZones().values().iterator().next();
                    dest = firstZone.getTeleportDestination();
                }
                if (dest == null) {
                    dest = escapeShipDestination;
                }
                
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, escapeShipEffectDuration, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, escapeShipEffectDuration, 1));
                player.sendTitle(getLang().getMessage("escape.escape-ship-title"), "", 10, 70, 20);

                final Location finalDest = dest;
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (player.isOnline()) {
                        player.sendTitle("", getLang().getMessage("escape.escape-ship-mid"));
                        Bukkit.getScheduler().runTaskLater(this, () -> {
                            if (player.isOnline()) {
                                player.sendTitle("", getLang().getMessage("escape.escape-ship-final"));
                                player.teleport(finalDest);
                                unjailPlayer(Bukkit.getConsoleSender(), player.getName(), true);
                            }
                        }, escapeShipFinalMessageDelay);
                    }
                }, escapeShipTeleportDelay);
            }
        }
    }
}

    // ================ УПРАВЛЕНИЕ ИНВЕНТАРЁМ ЗАКЛЮЧЁННЫХ ================
    private void savePlayerInventory(Player player) {
        UUID playerId = player.getUniqueId();
        File file = new File(getDataFolder(), "inventoryData.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(playerId.toString() + ".items", player.getInventory().getContents());
        config.set(playerId.toString() + ".armor", player.getInventory().getArmorContents());
        config.set(playerId.toString() + ".offHand", player.getInventory().getItemInOffHand());
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void givePrisonerRobe(Player player) {
    ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
    ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS);
    ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);

    LeatherArmorMeta chestMeta = (LeatherArmorMeta) chest.getItemMeta();
    LeatherArmorMeta legsMeta = (LeatherArmorMeta) legs.getItemMeta();
    LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();

    Color orange = Color.fromRGB(213, 91, 29);
    chestMeta.setColor(orange);
    chestMeta.setDisplayName(getLang().getMessage("items.prisoner-chestplate"));
    legsMeta.setColor(orange);
    legsMeta.setDisplayName(getLang().getMessage("items.prisoner-leggings"));
    bootsMeta.setColor(orange);
    bootsMeta.setDisplayName(getLang().getMessage("items.prisoner-boots"));

    chest.setItemMeta(chestMeta);
    legs.setItemMeta(legsMeta);
    boots.setItemMeta(bootsMeta);

    player.getInventory().clear();
    player.getInventory().setArmorContents(new ItemStack[] { boots, legs, chest, null });
}

    private void loadPlayerInventory(Player player) {
        UUID playerId = player.getUniqueId();
        File file = new File(getDataFolder(), "inventoryData.yml");
        if (!file.exists())
            return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> items = (List<ItemStack>) config.getList(playerId.toString() + ".items");
        List<ItemStack> armor = (List<ItemStack>) config.getList(playerId.toString() + ".armor");
        ItemStack offHand = (ItemStack) config.get(playerId.toString() + ".offHand");
        if (items != null)
            player.getInventory().setContents(items.toArray(new ItemStack[0]));
        if (armor != null)
            player.getInventory().setArmorContents(armor.toArray(new ItemStack[0]));
        if (offHand != null)
            player.getInventory().setItemInOffHand(offHand);
    }

    // ================ ПОКАЗ ВРЕМЕНИ И СТАТУСА ================
    private void showJailTime(CommandSender sender) {
    if (!(sender instanceof Player)) {
        sender.sendMessage(getLang().getMessage("general.command-only-player"));
        return;
    }
    Player player = (Player) sender;
    JailData data = jailedPlayers.get(player.getName());
    if (data == null) {
        sender.sendMessage(getLang().getMessage("jtime.not-jailed"));
        return;
    }
    int sec = data.getTime();
    int min = sec / 60;
    int s = sec % 60;
    sender.sendMessage(getLang().getMessage("jtime.self", "minutes", min, "seconds", s));
}

private void deleteCell(CommandSender sender, String cellName) {
    if (!cellsConfig.contains("cells." + cellName)) {
        sender.sendMessage(getLang().getMessage("cell.delete.not-found"));
        return;
    }
    cellsConfig.set("cells." + cellName, null);
    saveCellsConfig();
    sender.sendMessage(getLang().getMessage("cell.delete.success", "name", cellName));
}

    public HashMap<String, JailData> getJailedPlayers() {
        return jailedPlayers;
    }

    private void createCellsConfig() {
        cellsFile = new File(getDataFolder(), "cells.yml");
        if (!cellsFile.exists()) {
            try {
                cellsFile.createNewFile();
                cellsConfig = YamlConfiguration.loadConfiguration(cellsFile);
                cellsConfig.set("cells", new HashMap<>());
                saveCellsConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            cellsConfig = YamlConfiguration.loadConfiguration(cellsFile);
        }
    }

    private void createJailsConfig() {
        jailsFile = new File(getDataFolder(), "jails.yml");
        if (!jailsFile.exists()) {
            try {
                jailsFile.createNewFile();
                jailsConfig = YamlConfiguration.loadConfiguration(jailsFile);
                jailsConfig.set("jails", new HashMap<>());
                saveJailsConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            jailsConfig = YamlConfiguration.loadConfiguration(jailsFile);
        }
    }

private void createCell(Player player, String name) {
    Location loc = player.getLocation();
    cellsConfig.set("cells." + name + ".world", loc.getWorld().getName());
    cellsConfig.set("cells." + name + ".x", loc.getX());
    cellsConfig.set("cells." + name + ".y", loc.getY());
    cellsConfig.set("cells." + name + ".z", loc.getZ());
    saveCellsConfig();
    player.sendMessage(getLang().getMessage("cell.create.success", "name", name));
}

private void showJailStatus(CommandSender sender) {
    if (!jailsConfig.contains("jails")) {
        sender.sendMessage(getLang().getMessage("jailstatus.empty"));
        return;
    }
    sender.sendMessage(getLang().getMessage("jailstatus.header"));
    for (String name : jailsConfig.getConfigurationSection("jails").getKeys(false)) {
        String cell = jailsConfig.getString("jails." + name + ".cell");
        int time = jailsConfig.getInt("jails." + name + ".time");
        int min = time / 60;
        int sec = time % 60;
        sender.sendMessage(getLang().getMessage("jailstatus.entry", 
                "player", name, "cell", cell, "minutes", min, "seconds", sec));
    }
}

    private void jailPlayer(CommandSender sender, String playerName, int minutes, String cellName, String reason) {
    if (minutes <= 0) {
        sender.sendMessage(getLang().getMessage("jail.positive-time"));
        return;
    }
    if (jailedPlayers.containsKey(playerName)) {
        sender.sendMessage(getLang().getMessage("jail.already-jailed"));
        return;
    }
    Player player = Bukkit.getPlayer(playerName);
    if (player == null) {
        sender.sendMessage(getLang().getMessage("general.player-not-found"));
        return;
    }
    
    PlayerInventoryData invData = new PlayerInventoryData(
            player.getUniqueId(),
            player.getInventory().getContents(),
            player.getInventory().getArmorContents(),
            player.getInventory().getItemInOffHand());
    savePlayerInventory(player.getUniqueId(), invData);

    if (!cellsConfig.contains("cells." + cellName)) {
        sender.sendMessage(getLang().getMessage("jail.cell-not-found"));
        return;
    }
    double x = cellsConfig.getDouble("cells." + cellName + ".x");
    double y = cellsConfig.getDouble("cells." + cellName + ".y");
    double z = cellsConfig.getDouble("cells." + cellName + ".z");
    World world = Bukkit.getWorld(cellsConfig.getString("cells." + cellName + ".world"));
    if (world == null) {
        sender.sendMessage(getLang().getMessage("jail.world-not-found"));
        return;
    }
    Location cellLoc = new Location(world, x, y, z);
    player.teleport(cellLoc);
    player.setGameMode(GameMode.ADVENTURE);
    player.setBedSpawnLocation(cellLoc, true);

    int timeSec = minutes * 60;
    jailsConfig.set("jails." + playerName + ".cell", cellName);
    jailsConfig.set("jails." + playerName + ".time", timeSec);
    saveJailsConfig();
    addHistoryEntry(playerName, sender.getName(), minutes, cellName, reason, "jailed");

    jailLogger.logEvent("Игрок " + playerName + " заключён в тюрьму на " + minutes + " мин в камеру '" + cellName
            + "' (причина: " + reason + ") (по команде: " + sender.getName() + ").");
    jailedPlayers.put(playerName, new JailData(cellLoc, timeSec));
    givePrisonerRobe(player);

    Bukkit.broadcastMessage(getLang().getMessage("jail.broadcast-jailed", "player", playerName, "minutes", minutes));
    jailLogger.logEvent("Игрок " + playerName + " заключён в тюрьму на " + minutes + " мин в камеру: '" + cellName
            + "' (по команде: " + sender.getName() + ").");
    sendJailInfo(player);
    boardManager.updateAllBoards();
}

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getState() instanceof Sign) {
            boardManager.removeBoard(event.getBlock().getLocation());
        }
    }

    public void unjailPlayer(CommandSender sender, String playerName, boolean isEscape) {
    if (!jailsConfig.contains("jails." + playerName)) {
        sender.sendMessage(getLang().getMessage("unjail.not-jailed"));
        return;
    }
    
    String status = isEscape ? "escaped" : "released";
    int minutes = jailsConfig.getInt("jails." + playerName + ".time", 0) / 60;
    String cell = jailsConfig.getString("jails." + playerName + ".cell", "?");
    addHistoryEntry(playerName, sender.getName(), minutes, cell, "", status);
    
    jailsConfig.set("jails." + playerName, null);
    saveJailsConfig();

    Player target = Bukkit.getPlayer(playerName);
    if (target != null) {
        jailedPlayers.remove(playerName);
        escapeCountdown.remove(target);
        target.setGameMode(GameMode.SURVIVAL);
        PlayerInventoryData invData = loadPlayerInventory(target.getUniqueId());
        if (invData != null) {
            target.getInventory().setContents(invData.getItems());
            target.getInventory().setArmorContents(invData.getArmor());
            target.getInventory().setItemInOffHand(invData.getOffHand());
        } else {
            sender.sendMessage(getLang().getMessage("unjail.inventory-not-found"));
        }
        
        if (isEscape) {
            Bukkit.broadcastMessage(getLang().getMessage("escape.broadcast-escape", "player", playerName));
            jailLogger.logEvent("Игрок " + playerName + " сбежал из тюрьмы!");
        } else {
            Bukkit.broadcastMessage(getLang().getMessage("unjail.broadcast-released", "player", playerName));
            jailLogger.logEvent("Игрок " + playerName + " освобождён из тюрьмы игроком: " + sender.getName() + ".");
        }
        
        target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        boardManager.updateAllBoards();
    } else {
        sender.sendMessage(getLang().getMessage("general.player-not-found"));
    }
}

    private void savePlayerInventory(UUID playerId, PlayerInventoryData invData) {
        File file = new File(getDataFolder(), "playerInventories.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set(playerId.toString() + ".items", invData.getItems());
        cfg.set(playerId.toString() + ".armor", invData.getArmor());
        cfg.set(playerId.toString() + ".offHand", invData.getOffHand());
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private PlayerInventoryData loadPlayerInventory(UUID playerId) {
        File file = new File(getDataFolder(), "playerInventories.yml");
        if (!file.exists())
            return null;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> items = (List<ItemStack>) cfg.getList(playerId.toString() + ".items");
        List<ItemStack> armor = (List<ItemStack>) cfg.getList(playerId.toString() + ".armor");
        ItemStack offHand = (ItemStack) cfg.get(playerId.toString() + ".offHand");
        if (items == null || armor == null)
            return null;
        return new PlayerInventoryData(
                playerId,
                items.toArray(new ItemStack[0]),
                armor.toArray(new ItemStack[0]),
                offHand);
    }

    public class PlayerInventoryData {
        private final UUID playerId;
        private final ItemStack[] items;
        private final ItemStack[] armor;
        private final ItemStack offHand;

        public PlayerInventoryData(UUID playerId, ItemStack[] items, ItemStack[] armor, ItemStack offHand) {
            this.playerId = playerId;
            this.items = items;
            this.armor = armor;
            this.offHand = offHand;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public ItemStack[] getItems() {
            return items;
        }

        public ItemStack[] getArmor() {
            return armor;
        }

        public ItemStack getOffHand() {
            return offHand;
        }
    }

    private Location getCellLocation(String cellName) {
        String worldName = cellsConfig.getString("cells." + cellName + ".world");
        double x = cellsConfig.getDouble("cells." + cellName + ".x");
        double y = cellsConfig.getDouble("cells." + cellName + ".y");
        double z = cellsConfig.getDouble("cells." + cellName + ".z");
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    public FileConfiguration getCellsConfig() {
        return cellsConfig;
    }

    private boolean isGuardArmorEquipped(Player player) {
        return itemChecker.hasFullGuardArmor(player);
    }

    // ================ ЗАДАЧА ПРОВЕРКИ ЗАКЛЮЧЁННЫХ ================
    private void startJailCheckTask() {
        new BukkitRunnable() {
            private int tickCounter = 0;

            @Override
            public void run() {
                boolean configUpdated = false;
                tickCounter++;

                for (String name : jailedPlayers.keySet()) {
                    Player player = Bukkit.getPlayer(name);
                    if (player == null)
                        continue;

                    JailData data = jailedPlayers.get(name);
                    Location cellLoc = data.getCellLocation();

                    if (!player.getWorld().equals(cellLoc.getWorld())) {
                        player.teleport(cellLoc);
                        continue;
                    }

                    double dist = player.getLocation().distance(cellLoc);
                    if (player.getGameMode() != GameMode.ADVENTURE) {
                        player.setGameMode(GameMode.ADVENTURE);
                    }

                    if (dist > 100) {
                        if (isGuardArmorEquipped(player) && !escapeCountdown.containsKey(player)) {
                            // Начало побега
                            player.sendMessage(getLang().getMessage("escape.start", "time", escapeTimeLimit / 60));

                            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                                    getConfig().getString("escape.messages.broadcast-escape",
                                            "&7[Тюрьма] &eИгрок {player} сбежал из тюрьмы!")
                                            .replace("{player}", player.getName())));

                            jailLogger.logEvent("Игрок " + player.getName() + " начал побег из тюрьмы!");
                            escapeCountdown.put(player, escapeTimeLimit);
                            lastLocationMessageTime.put(player, System.currentTimeMillis());
                        } else if (!isGuardArmorEquipped(player)) {
                            player.teleport(cellLoc);
                        }
                    }

                    if (escapeCountdown.containsKey(player)) {
                        int timeLeft = escapeCountdown.get(player);
                        if (timeLeft <= 0) {
                            player.sendMessage(getLang().getMessage("escape.fail", "multiplier", (int) escapeFailMultiplier));

                            escapeCountdown.remove(player);
                            int remaining = data.getTime();
                            data.setTime((int) (remaining * escapeFailMultiplier));
                            int total = (int) (remaining * escapeFailMultiplier);

                            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                                    getConfig().getString("escape.messages.broadcast-caught",
                                            "&7[Тюрьма] &cИгрок {player} был пойман при попытке побега!")
                                            .replace("{player}", player.getName())));

                            player.teleport(cellLoc);
                        } else {
                            escapeCountdown.put(player, timeLeft - 1);
                            String timeStr = String.format(getLang().getMessage("escape.escape-time-left"), timeLeft / 60, timeLeft % 60);
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(timeStr));

                            long now = System.currentTimeMillis();
                            if (now - lastLocationMessageTime.getOrDefault(player, 0L) >= 60_000) {
                                String locationMsg = getConfig().getString("escape.messages.broadcast-location",
                                        "&7[Тюрьма] &eСбежавший заключённый {player} был замечен на координатах: X: {x}, Y: {y}, Z: {z}.")
                                        .replace("{player}", player.getName())
                                        .replace("{x}", String.valueOf(player.getLocation().getBlockX()))
                                        .replace("{y}", String.valueOf(player.getLocation().getBlockY()))
                                        .replace("{z}", String.valueOf(player.getLocation().getBlockZ()));
                                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', locationMsg));
                                lastLocationMessageTime.put(player, now);
                            }
                        }
                    }

                    boolean isAfk = false;
                    if (afkTimeout > 0) {
                        long lastMove = lastMovementTime.getOrDefault(name, System.currentTimeMillis());
                        isAfk = (System.currentTimeMillis() - lastMove) >= afkTimeout;

                        if (isAfk && !afkStatus.getOrDefault(name, false)) {
                            afkStatus.put(name, true);
                            player.sendMessage(afkMessageOn);
                        } else if (!isAfk && afkStatus.getOrDefault(name, false)) {
                            afkStatus.put(name, false);
                            player.sendMessage(afkMessageOff);
                        }
                    } else {
                        if (afkStatus.getOrDefault(name, false)) {
                            afkStatus.put(name, false);
                        }
                    }

                    if (data.getTime() > 0) {
                        if (afkTimeout > 0 && isAfk) {
                            if (tickCounter % afkTimeMultiplier == 0) {
                                data.decrementTime();
                            }
                        } else {
                            data.decrementTime();
                        }
                        jailsConfig.set("jails." + name + ".time", data.getTime());
                        configUpdated = true;
                    } else {
                        unjailPlayer(Bukkit.getConsoleSender(), name, false);
                        player.sendMessage("§aВы успешно отсидели свой срок и теперь свободны!");
                        player.teleport(spawnPoint);
                    }
                }

                if (configUpdated)
                    saveJailsConfig();
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    public void setPlayerAfkStatus(Player player, boolean isAfk) {
        afkStatus.put(player.getName(), isAfk);
    }

    // ================ ОБРАБОТЧИКИ СОБЫТИЙ ================
    @EventHandler
    public void onPlayerMove1(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        if (afkTimeout > 0) {
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                lastMovementTime.put(name, System.currentTimeMillis());
                if (afkStatus.getOrDefault(name, false)) {
                    afkStatus.put(name, false);
                    player.sendMessage(afkMessageOff);
                }
            }
        }
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        if (isGuardArmorEquipped(player)) {
            return;
        } else {
            JailData data = jailedPlayers.get(player.getName());
            if (escapeCountdown.containsKey(player)) {
                escapeCountdown.remove(player);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                int remaining = data.getTime();
                data.setTime((int) (remaining * escapeFailMultiplier));
                int total = (int) (remaining * escapeFailMultiplier);
                
                player.sendMessage(getLang().getMessage("escape.caught", "multiplier", (int) escapeFailMultiplier));
                
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                        getConfig().getString("escape.messages.broadcast-caught",
                                "&7[Тюрьма] &cИгрок {player} был пойман при попытке побега!")
                                .replace("{player}", player.getName())));
            }
        }
    }

    @EventHandler
public void onPlayerKill(PlayerDeathEvent event) {
    Player killed = event.getEntity();
    Player killer = killed.getKiller();
    if (killer != null && escapeCountdown.containsKey(killed)) {
        JailData data = jailedPlayers.get(killed.getName());
        int remaining = data.getTime();
        int reward = remaining / captureRewardDivider;
        if (reward > 0) {
            ItemStack coin = itemChecker.createItem("swcoin");
            if (coin != null) {
                coin.setAmount(reward);
                killer.getInventory().addItem(coin);
            } else {
                ItemStack paper = new ItemStack(Material.PAPER, reward);
                ItemMeta meta = paper.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "SWnote");
                meta.setLore(Collections.singletonList(ChatColor.LIGHT_PURPLE + "Валюта сервера"));
                paper.setItemMeta(meta);
                killer.getInventory().addItem(paper);
            }
            killer.sendMessage(getLang().getMessage("misc.capture-reward-message", 
                    "amount", reward, "swcoin", ChatColor.stripColor(swcoinName), "player", killed.getName()));
        }
        Bukkit.broadcastMessage(getLang().getMessage("misc.capture-broadcast", "killer", killer.getName(), "victim", killed.getName()));
    }
}

@EventHandler
public void onPlayerPortal(PlayerPortalEvent event) {
    Player player = event.getPlayer();
    if (escapeCountdown.containsKey(player) || jailedPlayers.containsKey(player.getName())) {
        event.setCancelled(true);
        player.sendMessage(getLang().getMessage("misc.cannot-use-portal"));
    }
}

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (escapeCountdown.containsKey(player)) {
            escapeCountdown.remove(player);
            lastLocationMessageTime.remove(player);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
            JailData data = jailedPlayers.get(player.getName());
            if (data != null) {
                int remaining = data.getTime();
                data.setTime((int) (remaining * escapeFailMultiplier));
                int total = (int) (remaining * escapeFailMultiplier);
                
                String caughtMsg = getConfig().getString("escape.messages.escape-caught",
                        "&cВас поймали при попытке побега! Срок увеличен в {multiplier} раз!")
                        .replace("{multiplier}", String.valueOf((int) escapeFailMultiplier));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', caughtMsg));
                
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                        getConfig().getString("escape.messages.broadcast-caught",
                                "&7[Тюрьма] &cИгрок {player} был пойман при попытке побега!")
                                .replace("{player}", player.getName())));
            }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        }
    }

    private void saveCellsConfig() {
        try {
            cellsConfig.save(cellsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadJailData() {
        if (jailsConfig.contains("jails")) {
            for (String name : jailsConfig.getConfigurationSection("jails").getKeys(false)) {
                String cellName = jailsConfig.getString("jails." + name + ".cell");
                int time = jailsConfig.getInt("jails." + name + ".time");
                if (cellsConfig.contains("cells." + cellName)) {
                    double x = cellsConfig.getDouble("cells." + cellName + ".x");
                    double y = cellsConfig.getDouble("cells." + cellName + ".y");
                    double z = cellsConfig.getDouble("cells." + cellName + ".z");
                    String worldName = cellsConfig.getString("cells." + cellName + ".world");
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        jailedPlayers.put(name, new JailData(new Location(world, x, y, z), time));
                    } else {
                        getLogger().warning("Мир не найден для клетки " + cellName + " игрока " + name);
                    }
                }
            }
        }
    }
private void registerMapRecipe() {

    if (blackMarketManager == null) {
        getLogger().warning("BlackMarketManager не инициализирован, рецепт не зарегистрирован."); 
    }

    ItemStack fragment = new ItemStack(Material.PAPER);
    ItemMeta meta = fragment.getItemMeta();
    meta.setDisplayName(getLang().getMessage("items.map-fragment"));
    fragment.setItemMeta(meta);

    ItemStack map = blackMarketManager.createMarketMap();
    if (map == null) {
        getLogger().warning("Не удалось создать карту для рецепта."); 
        return;
    }

    NamespacedKey key = new NamespacedKey(this, "black_market_map");
    ShapedRecipe recipe = new ShapedRecipe(key, map);
    recipe.shape("AAA", "AAA", "AAA");
    recipe.setIngredient('A', new RecipeChoice.ExactChoice(fragment));

    Bukkit.addRecipe(recipe);
    getLogger().info("Зарегистрирован рецепт карты чёрного рынка"); 
}
    private void saveJailsConfig() {
        try {
            jailsConfig.save(jailsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public JailWorkListener getJailWorkListener() {
    return jailWorkListener;
    }
    public LanguageManager getLang() {
    return lang;
}
}
