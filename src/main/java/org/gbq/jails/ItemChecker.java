package org.gbq.jails;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemChecker {
    private final Jails plugin;
    
    public ItemChecker(Jails plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Проверяет, является ли предмет билетом на паром (заменяет старый паспорт)
     */
    public boolean isEscapeTicket(ItemStack item) {
        if (item == null) return false;
        
        ConfigurationSection ticketSec = plugin.getConfig().getConfigurationSection("blackmarket.trader.offers.ticket.item");
        if (ticketSec == null) return false;
        
        
        Material expectedMaterial = Material.getMaterial(ticketSec.getString("material", "PAPER"));
        if (item.getType() != expectedMaterial) return false;
        
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        
        String expectedName = ChatColor.translateAlternateColorCodes('&', 
            plugin.getLang().getMessage("blackmarket.trader.offers.ticket.item.name"));
        if (!meta.hasDisplayName() || !meta.getDisplayName().equals(expectedName)) return false;
        
        return true;
    }
    
    /**
     * Проверяет, является ли предмет яйцом призыва торговца
     */
    public boolean isTraderSpawnEgg(ItemStack item) {
        if (item == null) return false;
        
        ConfigurationSection spawnSec = plugin.getConfig().getConfigurationSection("blackmarket.trader.spawn-item");
        if (spawnSec == null) return false;
        
        Material expectedMaterial = Material.getMaterial(spawnSec.getString("material", "VILLAGER_SPAWN_EGG"));
        if (item.getType() != expectedMaterial) return false;
        
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        
        String expectedName = ChatColor.translateAlternateColorCodes('&', 
            spawnSec.getString("name", "&6Яйцо призыва торговца"));
        if (!meta.hasDisplayName() || !meta.getDisplayName().equals(expectedName)) return false;
        
        return true;
    }
    
    /**
     * Проверяет, является ли предмет глушителем слежения
     */
    public boolean isTrackingJammer(ItemStack item) {
        if (item == null) return false;
        
        ConfigurationSection jammerSec = plugin.getConfig().getConfigurationSection("blackmarket.items.jammer");
        if (jammerSec == null || !jammerSec.getBoolean("enabled", false)) return false;
        
        Material expectedMaterial = Material.getMaterial(jammerSec.getString("material", "CLOCK"));
        if (item.getType() != expectedMaterial) return false;
        
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        
        
        String expectedName = ChatColor.translateAlternateColorCodes('&', 
            jammerSec.getString("name", "&6Глушитель слежения"));
        if (!meta.hasDisplayName() || !meta.getDisplayName().equals(expectedName)) return false;
        
        return true;
    }
    
    /**
     * Проверяет, является ли предмет паспортом из конфига (оставлено для совместимости)
     * @deprecated Используйте isEscapeTicket()
     */
    @Deprecated
    public boolean isPassport(ItemStack item) {
        return isEscapeTicket(item);
    }
    
    /**
     * Проверяет, является ли предмет отмычкой из конфига
     */
    public boolean isLockpick(ItemStack item) {
        if (item == null) return false;
        
        ConfigurationSection lockpickSec = plugin.getConfig().getConfigurationSection("items.lockpick");
        if (lockpickSec == null) return false;
        
        
        Material expectedMaterial = Material.getMaterial(lockpickSec.getString("material", "WOODEN_HOE"));
        if (item.getType() != expectedMaterial) return false;
        
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        
        
        String expectedName = ChatColor.translateAlternateColorCodes('&', 
            lockpickSec.getString("name", "&6Деревянная отмычка"));
        if (!meta.hasDisplayName() || !meta.getDisplayName().equals(expectedName)) return false;
        
        List<String> expectedLore = lockpickSec.getStringList("lore");
        if (!expectedLore.isEmpty()) {
            if (!meta.hasLore()) return false;
            
            List<String> actualLore = meta.getLore();
            if (actualLore.size() != expectedLore.size()) return false;
            
            for (int i = 0; i < expectedLore.size(); i++) {
                String expectedLine = ChatColor.translateAlternateColorCodes('&', expectedLore.get(i));
                if (!expectedLine.equals(actualLore.get(i))) return false;
            }
        }
        
        return true;
    }
    
    /**
     * Проверяет, является ли предмет частью брони надзирателя
     */
    public boolean isGuardArmorPiece(ItemStack item, String pieceType) {
        if (item == null) return false;
        
        ConfigurationSection armorSec = plugin.getConfig().getConfigurationSection("items.guard-armor");
        if (armorSec == null) return false;
        
        ConfigurationSection pieceSec = armorSec.getConfigurationSection(pieceType);
        if (pieceSec == null) return false;
        
        
        String materialStr = pieceSec.getString("material");
        if (materialStr == null) return false;
        
        Material expectedMaterial = Material.getMaterial(materialStr);
        if (item.getType() != expectedMaterial) return false;
        
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        
        
        String expectedName = ChatColor.translateAlternateColorCodes('&', 
            pieceSec.getString("name", ""));
        if (!meta.hasDisplayName() || !meta.getDisplayName().equals(expectedName)) return false;
        
        
        if (meta instanceof LeatherArmorMeta && pieceSec.contains("color")) {
            LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
            Color expectedColor = parseColor(pieceSec.getString("color", "0,0,70"));
            if (!expectedColor.equals(leatherMeta.getColor())) return false;
        }
        
        return true;
    }
    
    /**
     * Проверяет полный комплект брони надзирателя
     */
    public boolean hasFullGuardArmor(Player player) {
        return isGuardArmorPiece(player.getInventory().getHelmet(), "helmet") &&
               isGuardArmorPiece(player.getInventory().getChestplate(), "chestplate") &&
               isGuardArmorPiece(player.getInventory().getLeggings(), "leggings") &&
               isGuardArmorPiece(player.getInventory().getBoots(), "boots");
    }
    
    /**
     * Проверяет, является ли предмет SWcoin из конфига
     */
    public boolean isSwcoin(ItemStack item) {
        if (item == null) return false;
        
        Material expectedMaterial = plugin.getSwcoinMaterial();
        if (item.getType() != expectedMaterial) return false;
        
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        
        String expectedName = plugin.getSwcoinName();
        if (!meta.hasDisplayName() || !meta.getDisplayName().equals(expectedName)) return false;
        
        String expectedLore = plugin.getSwcoinLore();
        if (expectedLore != null && !expectedLore.isEmpty()) {
            if (!meta.hasLore()) return false;
            List<String> lore = meta.getLore();
            boolean found = false;
            for (String line : lore) {
                if (line.equals(expectedLore)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        
        return true;
    }
    
    /**
     * Создаёт билет на паром
     */
    public ItemStack createEscapeTicket() {
        return createTraderItem("blackmarket.trader.offers.ticket.item");
    }
    
    /**
     * Создаёт яйцо призыва торговца
     */
    public ItemStack createTraderSpawnEgg() {
        return createTraderItem("blackmarket.trader.spawn-item");
    }
    
    /**
     * Создаёт предмет из секции trader в конфиге
     */
    private ItemStack createTraderItem(String path) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return null;
        
        String materialStr = sec.getString("material", "PAPER");
        Material material = Material.getMaterial(materialStr);
        if (material == null) material = Material.PAPER;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
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
    
    /**
     * Создаёт предмет из конфига по указанному пути
     */
    public ItemStack createItem(String path) {
        ConfigurationSection itemSec = plugin.getConfig().getConfigurationSection(path);
        if (itemSec == null) return null;
        
        String materialStr = itemSec.getString("material", "STONE");
        Material material = Material.getMaterial(materialStr);
        if (material == null) material = Material.STONE;
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        if (itemSec.contains("name")) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                itemSec.getString("name")));
        }
        
        if (itemSec.contains("lore")) {
            List<String> lore = new ArrayList<>();
            for (String line : itemSec.getStringList("lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
        }
        
        if (material == Material.WRITTEN_BOOK && meta instanceof BookMeta) {
            BookMeta bookMeta = (BookMeta) meta;
            if (itemSec.contains("title")) {
                bookMeta.setTitle(ChatColor.translateAlternateColorCodes('&', 
                    itemSec.getString("title")));
            }
            if (itemSec.contains("author")) {
                bookMeta.setAuthor(ChatColor.translateAlternateColorCodes('&', 
                    itemSec.getString("author")));
            }
            if (itemSec.contains("pages")) {
                bookMeta.setPages(itemSec.getStringList("pages"));
            }
            item.setItemMeta(bookMeta);
            return item;
        }
        
        if (meta instanceof LeatherArmorMeta && itemSec.contains("color")) {
            LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
            leatherMeta.setColor(parseColor(itemSec.getString("color")));
            item.setItemMeta(leatherMeta);
            return item;
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Парсит цвет из строки "R,G,B"
     */
    private Color parseColor(String rgbString) {
        if (rgbString == null) return Color.fromRGB(0, 0, 70);
        
        String[] parts = rgbString.split(",");
        if (parts.length >= 3) {
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return Color.fromRGB(r, g, b);
            } catch (NumberFormatException e) {
                return Color.fromRGB(0, 0, 70);
            }
        }
        return Color.fromRGB(0, 0, 70);
    }
}