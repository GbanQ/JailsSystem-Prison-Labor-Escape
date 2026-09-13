package org.gbq.jails;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EscapeManager {
    private final Jails plugin;
    private final Map<String, EscapeZone> escapeZones = new HashMap<>();
    private final Map<UUID, Long> lastLocationBroadcast = new HashMap<>();
    
    public EscapeManager(Jails plugin) {
        this.plugin = plugin;
        loadZones();
    }
    
    public void loadZones() {
        escapeZones.clear();
        ConfigurationSection zonesSec = plugin.getConfig().getConfigurationSection("escape.escape-zones");
        if (zonesSec == null) return;
        
        for (String key : zonesSec.getKeys(false)) {
            ConfigurationSection zoneSec = zonesSec.getConfigurationSection(key);
            if (zoneSec == null || !zoneSec.getBoolean("enabled", false)) continue;
            
            EscapeZone zone = new EscapeZone(key, zoneSec);
            escapeZones.put(key, zone);
            plugin.getLogger().info("Загружена зона побега: " + key); 
        }
    }
    
    public EscapeZone getZoneAt(Location loc) {
        for (EscapeZone zone : escapeZones.values()) {
            if (zone.isInside(loc)) return zone;
        }
        return null;
    }
    
    public boolean isInAnyEscapeZone(Player player) {
        return getZoneAt(player.getLocation()) != null;
    }
    
    public Map<String, EscapeZone> getEscapeZones() {
        return Collections.unmodifiableMap(escapeZones);
    }
    
    public void checkLocationBroadcast(Player player) {
        if (!plugin.getConfig().getBoolean("escape.location-broadcast.enabled", true)) return;
        
        if (hasTrackingJammer(player)) return;
        
        long now = System.currentTimeMillis();
        long last = lastLocationBroadcast.getOrDefault(player.getUniqueId(), 0L);
        int interval = plugin.getConfig().getInt("escape.location-broadcast.interval", 60) * 1000;
        
        if (now - last >= interval) {
            Location loc = player.getLocation();
            String message = plugin.getLang().getMessage("escape.broadcast-location",
                    "player", player.getName(),
                    "x", loc.getBlockX(),
                    "y", loc.getBlockY(),
                    "z", loc.getBlockZ());
            
            plugin.getServer().broadcastMessage(message);
            lastLocationBroadcast.put(player.getUniqueId(), now);
        }
    }
    
    /**
     * Проверяет наличие билета на паром
     */
    public boolean hasEscapeTicket(Player player) {
        ItemChecker itemChecker = plugin.getItemChecker();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && itemChecker.isEscapeTicket(item)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @deprecated Используйте hasEscapeTicket()
     */
    @Deprecated
    public boolean hasPassport(Player player) {
        return hasEscapeTicket(player);
    }
    
    /**
     * Проверяет наличие глушителя слежения (опционально)
     */
    public boolean hasTrackingJammer(Player player) {
      
        ConfigurationSection jammerSec = plugin.getConfig().getConfigurationSection("blackmarket.items.jammer");
        if (jammerSec == null || !jammerSec.getBoolean("enabled", false)) return false;
        
        ItemChecker itemChecker = plugin.getItemChecker();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && itemChecker.isTrackingJammer(item)) {
                return true;
            }
        }
        return false;
    }
    
    
    public boolean hasFullGuardArmor(Player player) {
        ItemChecker itemChecker = plugin.getItemChecker();
        return itemChecker.hasFullGuardArmor(player);
    }
    
    /**
     * Создаёт билет на паром (заменяет старый паспорт)
     */
    public ItemStack createEscapeTicket() {
        return plugin.getItemChecker().createEscapeTicket();
    }
    
    /**
     * @deprecated Используйте createEscapeTicket()
     */
    @Deprecated
    public ItemStack createPassport() {
        return createEscapeTicket();
    }
    
    public ItemStack createLockpick() {
        return plugin.getItemChecker().createItem("items.lockpick");
    }
    
    public static class EscapeZone {
        private final String key;
        private final String name;
        private final World world;
        private final Location corner1;
        private final Location corner2;
        private final ItemStack interactItem;
        private final Location teleportDestination;
        private final List<String> playerCommands;
        private final List<String> consoleCommands;
        
        public EscapeZone(String key, ConfigurationSection sec) {
            this.key = key;
            this.name = org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                sec.getString("name", key));
            
            String worldName = sec.getString("world", "world");
            this.world = org.bukkit.Bukkit.getWorld(worldName);
            
            ConfigurationSection c1 = sec.getConfigurationSection("corner1");
            ConfigurationSection c2 = sec.getConfigurationSection("corner2");
            
            if (c1 != null && c2 != null && world != null) {
                this.corner1 = new Location(world, c1.getDouble("x"), c1.getDouble("y"), c1.getDouble("z"));
                this.corner2 = new Location(world, c2.getDouble("x"), c2.getDouble("y"), c2.getDouble("z"));
            } else {
                this.corner1 = null;
                this.corner2 = null;
            }
            
            ConfigurationSection itemSec = sec.getConfigurationSection("interact-item");
            if (itemSec != null) {
                Material mat = Material.getMaterial(itemSec.getString("material", "OAK_SIGN"));
                ItemStack item = new ItemStack(mat != null ? mat : Material.OAK_SIGN);
                ItemMeta meta = item.getItemMeta();
                
                if (itemSec.contains("name")) {
                    meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                        itemSec.getString("name")));
                }
                
                if (itemSec.contains("lore")) {
                    List<String> lore = new ArrayList<>();
                    for (String line : itemSec.getStringList("lore")) {
                        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                    }
                    meta.setLore(lore);
                }
                
                item.setItemMeta(meta);
                this.interactItem = item;
            } else {
                this.interactItem = null;
            }
            
            ConfigurationSection successSec = sec.getConfigurationSection("on-success");
            if (successSec != null) {
                ConfigurationSection tpSec = successSec.getConfigurationSection("teleport");
                if (tpSec != null && tpSec.getBoolean("enabled", false)) {
                    World tpWorld = org.bukkit.Bukkit.getWorld(tpSec.getString("world", "world"));
                    if (tpWorld != null) {
                        this.teleportDestination = new Location(tpWorld, 
                            tpSec.getDouble("x"), tpSec.getDouble("y"), tpSec.getDouble("z"));
                    } else {
                        this.teleportDestination = null;
                    }
                } else {
                    this.teleportDestination = null;
                }
                
                this.playerCommands = successSec.getStringList("player-commands");
                this.consoleCommands = successSec.getStringList("console-commands");
            } else {
                this.teleportDestination = null;
                this.playerCommands = new ArrayList<>();
                this.consoleCommands = new ArrayList<>();
            }
        }
        
        public boolean isInside(Location loc) {
            if (world == null || corner1 == null || corner2 == null || !loc.getWorld().equals(world)) 
                return false;
            
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
        
        public String getKey() { return key; }
        public String getName() { return name; }
        public World getWorld() { return world; }
        public Location getCorner1() { return corner1; }
        public Location getCorner2() { return corner2; }
        public ItemStack getInteractItem() { return interactItem != null ? interactItem.clone() : null; }
        public Location getTeleportDestination() { return teleportDestination != null ? teleportDestination.clone() : null; }
        public List<String> getPlayerCommands() { return Collections.unmodifiableList(playerCommands); }
        public List<String> getConsoleCommands() { return Collections.unmodifiableList(consoleCommands); }
    }
}