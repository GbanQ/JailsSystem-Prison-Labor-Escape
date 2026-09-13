package org.gbq.jails;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BlackMarketManager implements Listener {
    private final Jails plugin;
    private Location corner1;
    private Location corner2;
    private boolean enabled;
    

    private UUID traderUUID;
    private Merchant merchant;
    
    private int ticketPrice;
    
    private String traderName;

    public BlackMarketManager(Jails plugin) {
        this.plugin = plugin;
        loadConfig();
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void loadConfig() {
        ConfigurationSection marketSec = plugin.getConfig().getConfigurationSection("blackmarket");
        if (marketSec == null) {
            this.enabled = false;
            return;
        }

        this.enabled = marketSec.getBoolean("enabled", false);
        
        this.traderName = ChatColor.translateAlternateColorCodes('&',
                plugin.getLang().getMessage("blackmarket.trader.name"));

        ConfigurationSection zoneSec = marketSec.getConfigurationSection("zone");
        if (zoneSec != null) {
            String worldName = zoneSec.getString("world", "world");
            World world = plugin.getServer().getWorld(worldName);

            ConfigurationSection c1 = zoneSec.getConfigurationSection("corner1");
            ConfigurationSection c2 = zoneSec.getConfigurationSection("corner2");

            if (world != null && c1 != null && c2 != null) {
                corner1 = new Location(world, c1.getDouble("x"), c1.getDouble("y"), c1.getDouble("z"));
                corner2 = new Location(world, c2.getDouble("x"), c2.getDouble("y"), c2.getDouble("z"));
            } else {
                corner1 = null;
                corner2 = null;
            }
        }
        
        ConfigurationSection ticketSec = marketSec.getConfigurationSection("trader.offers.ticket");
        this.ticketPrice = ticketSec != null ? ticketSec.getInt("price", 20) : 20;
        
        createMerchantOffers();
    }
    
    private void createMerchantOffers() {
        merchant = Bukkit.createMerchant(traderName);
        List<MerchantRecipe> recipes = new ArrayList<>();
        
        ConfigurationSection offers = plugin.getConfig().getConfigurationSection("blackmarket.trader.offers");
        if (offers != null) {
            for (String key : offers.getKeys(false)) {
                ConfigurationSection offerSec = offers.getConfigurationSection(key);
                if (!offerSec.getBoolean("enabled", true)) continue;
                
                int price = offerSec.getInt("price", 10);
                ItemStack result = createItemFromSection(offerSec.getConfigurationSection("item"));
                if (result == null) continue;
                
                ItemStack currency = new ItemStack(plugin.getSwcoinMaterial(), price);
                ItemMeta meta = currency.getItemMeta();
                meta.setDisplayName(plugin.getSwcoinName());
                String lore = plugin.getSwcoinLore();
                if (lore != null && !lore.isEmpty()) {
                    meta.setLore(java.util.Collections.singletonList(lore));
                }
                currency.setItemMeta(meta);
                
                MerchantRecipe recipe = new MerchantRecipe(result, 999);
                recipe.addIngredient(currency);
                recipes.add(recipe);
            }
        }
        merchant.setRecipes(recipes);
    }
    
    private ItemStack createItemFromSection(ConfigurationSection sec) {
        if (sec == null) return null;
        Material mat = Material.getMaterial(sec.getString("material", "PAPER"));
        ItemStack item = new ItemStack(mat != null ? mat : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (sec.contains("name")) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', sec.getString("name")));
        }
        if (sec.contains("lore")) {
            List<String> lore = new ArrayList<>();
            for (String line : sec.getStringList("lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
    
    private boolean isTraderEntity(Entity entity) {
        if (entity == null || !(entity instanceof Villager)) return false;
        if (!entity.hasMetadata("NPC") && !(entity instanceof LivingEntity)) return false;
        
        String customName = entity.getCustomName();
        if (customName == null) return false;
        
        return customName.equals(traderName);
    }

    private Villager getCurrentTrader() {
        if (traderUUID != null) {
            Entity e = Bukkit.getEntity(traderUUID);
            if (e instanceof Villager && e.isValid()) {
                return (Villager) e;
            }
            traderUUID = null;
        }
        return null;
    }
    

    private Villager findTraderInWorld() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isTraderEntity(entity)) {
                    traderUUID = entity.getUniqueId();
                    return (Villager) entity;
                }
            }
        }
        return null;
    }
    
    public void spawnTrader(Location loc, Player player) {
        if (!enabled) return;
        if (!isInMarketZone(loc)) {
            player.sendMessage(plugin.getLang().getMessage("blackmarket.spawn-trader-not-in-zone"));
            return;
        }
        
        removeTrader();
        
        Location spawnLoc = loc.clone();
        if (player != null) {
            double dx = player.getLocation().getX() - spawnLoc.getX();
            double dz = player.getLocation().getZ() - spawnLoc.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            spawnLoc.setYaw(yaw);
            spawnLoc.setPitch(0);
        }
        
        Villager villager = (Villager) spawnLoc.getWorld().spawnEntity(spawnLoc, org.bukkit.entity.EntityType.VILLAGER);
        
        villager.setAI(false);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setCustomNameVisible(true);
        villager.setCustomName(traderName);
        villager.setProfession(Villager.Profession.NONE);
        villager.setVillagerType(Villager.Type.PLAINS);
        villager.setVillagerExperience(0);
        villager.setAgeLock(true);
        villager.setAdult();
        
        villager.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 10, true, false));
        villager.setRecipes(merchant.getRecipes());
        
        traderUUID = villager.getUniqueId();
        player.sendMessage(plugin.getLang().getMessage("blackmarket.spawn-success"));
    }
    
    public void removeTrader() {
        Villager trader = getCurrentTrader();
        if (trader != null) {
            trader.remove();
        }
        traderUUID = null;
    }
    
    private boolean isInMarketZone(Location loc) {
        if (corner1 == null || corner2 == null) return true;
        double x1 = Math.min(corner1.getX(), corner2.getX());
        double y1 = Math.min(corner1.getY(), corner2.getY());
        double z1 = Math.min(corner1.getZ(), corner2.getZ());
        double x2 = Math.max(corner1.getX(), corner2.getX());
        double y2 = Math.max(corner1.getY(), corner2.getY());
        double z2 = Math.max(corner1.getZ(), corner2.getZ());
        return loc.getX() >= x1 && loc.getX() <= x2 &&
               loc.getY() >= y1 && loc.getY() <= y2 &&
               loc.getZ() >= z1 && loc.getZ() <= z2;
    }
    
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (!enabled) return;
        Entity entity = event.getRightClicked();
        
        if (isTraderEntity(entity)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            
            ItemStack item = player.getInventory().getItemInMainHand();
            if (plugin.getItemChecker().isTraderSpawnEgg(item)) {
                removeTrader();
                player.sendMessage(plugin.getLang().getMessage("blackmarket.remove-success"));
                return;
            }
            
            if (!isInMarket(player)) {
                player.sendMessage(plugin.getLang().getMessage("blackmarket.trader-works-only-in-market"));
                return;
            }
            
            if (traderUUID == null || !traderUUID.equals(entity.getUniqueId())) {
                traderUUID = entity.getUniqueId();
            }
            
            player.openMerchant(merchant, true);
        }
    }
    
    public boolean onSpawnEggUse(Player player, org.bukkit.block.Block clickedBlock) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getItemChecker().isTraderSpawnEgg(item)) return false;
        
        Location spawnLoc;
        if (clickedBlock != null) {
            spawnLoc = clickedBlock.getLocation().add(0.5, 1, 0.5);
        } else {
            spawnLoc = player.getLocation();
        }
        
        spawnTrader(spawnLoc, player);
        
        item.setAmount(item.getAmount() - 1);
        return true;
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (isTraderEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isInMarket(Player player) {
        if (!enabled || corner1 == null || corner2 == null) return false;

        Location loc = player.getLocation();
        if (!loc.getWorld().equals(corner1.getWorld())) return false;

        double x1 = Math.min(corner1.getX(), corner2.getX());
        double y1 = Math.min(corner1.getY(), corner2.getY());
        double z1 = Math.min(corner1.getZ(), corner2.getZ());
        double x2 = Math.max(corner1.getX(), corner2.getX());
        double y2 = Math.max(corner1.getY(), corner2.getY());
        double z2 = Math.max(corner1.getZ(), corner2.getZ());

        return loc.getX() >= x1 && loc.getX() <= x2 &&
               loc.getY() >= y1 && loc.getY() <= y2 &&
               loc.getZ() >= z1 && loc.getZ() <= z2;
    }

    public boolean buyTicket(Player player) {
        if (!enabled) {
            player.sendMessage(plugin.getLang().getMessage("buypassport.disabled"));
            return false;
        }

        if (!isInMarket(player)) {
            player.sendMessage(plugin.getLang().getMessage("buypassport.not-in-market"));
            return false;
        }

        if (plugin.getEscapeManager().hasEscapeTicket(player)) {
            player.sendMessage(plugin.getLang().getMessage("buypassport.already-have"));
            return false;
        }

        if (!hasEnoughSwcoin(player, ticketPrice)) {
            player.sendMessage(plugin.getLang().getMessage("buypassport.not-enough-money", "swcoin", plugin.getSwcoinName()));
            return false;
        }

        removeSwcoin(player, ticketPrice);

        ItemStack ticket = plugin.getItemChecker().createEscapeTicket();
        if (ticket != null) {
            player.getInventory().addItem(ticket);
            player.sendMessage(plugin.getLang().getMessage("buypassport.success", "price", ticketPrice, "swcoin", plugin.getSwcoinName()));
            return true;
        }
        return false;
    }
    
    @Deprecated
    public boolean buyPassport(Player player) {
        return buyTicket(player);
    }

    public ItemStack createMarketMap() {
        if (!enabled || corner1 == null || corner2 == null) {
            plugin.getLogger().warning("Чёрный рынок не настроен, карта не создана."); 
        }

        ConfigurationSection marketSec = plugin.getConfig().getConfigurationSection("blackmarket");
        ConfigurationSection mapSettings = marketSec.getConfigurationSection("map-settings");

        if (mapSettings == null) {
            plugin.getLogger().warning("Настройки карты не найдены в конфиге."); 
            return null;
        }

        String mapName = mapSettings.getString("name");
        List<String> mapLore = mapSettings.getStringList("lore");
        String scaleStr = mapSettings.getString("scale", "FAR");
        String markerType = mapSettings.getString("marker-type", "red_x");
        int markerRotation = mapSettings.getInt("marker-rotation", 180);

        String coloredName = mapName != null ? ChatColor.translateAlternateColorCodes('&', mapName) : null;
        List<String> coloredLore = new ArrayList<>();
        for (String line : mapLore) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        Location targetLoc = new Location(corner1.getWorld(),
                (corner1.getX() + corner2.getX()) / 2,
                (corner1.getY() + corner2.getY()) / 2,
                (corner1.getZ() + corner2.getZ()) / 2);
        int targetX = targetLoc.getBlockX();
        int targetZ = targetLoc.getBlockZ();

        try {
            MapView mapView = Bukkit.createMap(targetLoc.getWorld());
            mapView.setCenterX(targetX);
            mapView.setCenterZ(targetZ);
            mapView.setTrackingPosition(true);

            MapView.Scale scale;
            try {
                scale = MapView.Scale.valueOf(scaleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный масштаб карты: " + scaleStr + ", используется FAR"); 
                scale = MapView.Scale.FAR;
            }
            mapView.setScale(scale);

            ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
            MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
            if (mapMeta != null) {
                mapMeta.setMapView(mapView);
                mapMeta.setDisplayName(coloredName);
                mapMeta.setLore(coloredLore);
                mapItem.setItemMeta(mapMeta);
            }
            return mapItem;

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при создании карты: " + e.getMessage()); 
            e.printStackTrace();
            return null;
        }
    }

    public void giveRealTreasureMap(Player player) {
        if (!enabled || corner1 == null || corner2 == null) {
            player.sendMessage(plugin.getLang().getMessage("givemarketmap.not-configured"));
            return;
        }

        ConfigurationSection marketSec = plugin.getConfig().getConfigurationSection("blackmarket");
        ConfigurationSection mapSettings = marketSec.getConfigurationSection("map-settings");

        if (mapSettings == null) {
            player.sendMessage(plugin.getLang().getMessage("blackmarket.map-settings-not-found"));
            return;
        }

        String mapName = mapSettings.getString("name");
        List<String> mapLore = mapSettings.getStringList("lore");
        String scaleStr = mapSettings.getString("scale", "FAR");
        String markerType = mapSettings.getString("marker-type", "red_x");
        int markerRotation = mapSettings.getInt("marker-rotation", 180);

        String coloredName = mapName != null ? ChatColor.translateAlternateColorCodes('&', mapName) : null;
        List<String> coloredLore = new ArrayList<>();
        for (String line : mapLore) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        Location targetLoc = new Location(corner1.getWorld(),
                (corner1.getX() + corner2.getX()) / 2,
                (corner1.getY() + corner2.getY()) / 2,
                (corner1.getZ() + corner2.getZ()) / 2);
        int targetX = targetLoc.getBlockX();
        int targetZ = targetLoc.getBlockZ();

        try {
            MapView mapView = Bukkit.createMap(targetLoc.getWorld());
            mapView.setCenterX(targetX);
            mapView.setCenterZ(targetZ);
            mapView.setTrackingPosition(true);

            MapView.Scale scale;
            try {
                scale = MapView.Scale.valueOf(scaleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный масштаб карты: " + scaleStr + ", используется FAR"); 
                scale = MapView.Scale.FAR;
            }
            mapView.setScale(scale);

            int mapId = mapView.getId();

            List<String> components = new ArrayList<>();
            components.add("map_id=" + mapId);

            if (coloredName != null && !coloredName.isEmpty()) {
                String escapedName = escapeJson(coloredName);
                components.add("custom_name='[{\"text\":\"" + escapedName + "\"}]'");
            }

            if (!coloredLore.isEmpty()) {
                List<String> loreEntries = new ArrayList<>();
                for (String line : coloredLore) {
                    String escapedLine = escapeJson(line);
                    loreEntries.add("'[{\"text\":\"" + escapedLine + "\",\"italic\":false}]'");
                }
                components.add("lore=[" + String.join(",", loreEntries) + "]");
            }

            components.add("map_decorations={\"market\":{\"type\":\"" + markerType + "\",\"x\":" + targetX + ",\"z\":" + targetZ + ",\"rotation\":" + markerRotation + "}}");

            String command = "give " + player.getName() + " filled_map[" + String.join(",", components) + "] 1";

            plugin.getLogger().info("Выполняется команда: " + command); 
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при создании карты: " + e.getMessage()); 
            e.printStackTrace();
            player.sendMessage(plugin.getLang().getMessage("blackmarket.map-creation-error"));
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean hasEnoughSwcoin(Player player, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getItemChecker().isSwcoin(item)) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeSwcoin(Player player, int amount) {
        int toRemove = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getItemChecker().isSwcoin(item)) {
                if (item.getAmount() > toRemove) {
                    item.setAmount(item.getAmount() - toRemove);
                    break;
                } else {
                    toRemove -= item.getAmount();
                    player.getInventory().removeItem(item);
                }
            }
        }
    }

    public int getTicketPrice() {
        return ticketPrice;
    }
    
    @Deprecated
    public int getPassportPrice() {
        return ticketPrice;
    }

    public Location getCorner1() {
        return corner1 != null ? corner1.clone() : null;
    }

    public Location getCorner2() {
        return corner2 != null ? corner2.clone() : null;
    }
}