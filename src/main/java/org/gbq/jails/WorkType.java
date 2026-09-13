package org.gbq.jails;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;
import java.util.List;

public enum WorkType {
    BRICKS("§6Работа с кирпичами", Material.BRICKS, Arrays.asList(
            new WorkBlock(Material.CHEST, "§6Положить кирпичи"),
            new WorkBlock(Material.CHEST, "§6Взять кирпичи"))),
    LAUNDRY("§6Прачечная", Material.WATER_BUCKET, Arrays.asList(
            new WorkBlock(Material.BARREL, "§6Грязная одежда"),
            new WorkBlock(Material.BARREL, "§6Чистая одежда"),
            new WorkBlock(Material.DROPPER, "§6Стиральная машинка"))),
    KITCHEN("§6Кухня", Material.COOKED_COD, Arrays.asList(
            new WorkBlock(Material.BARREL, "§6Взять ингредиенты"),
            new WorkBlock(Material.BARREL, "§6Положить готовое блюдо"))),
    LIBRARY("§6Библиотека", Material.BOOKSHELF, null),
    TRADER("§6Призыв торговца", Material.VILLAGER_SPAWN_EGG, null);

    private final String displayName;
    private final Material iconMaterial;
    private final List<WorkBlock> blocks;

    WorkType(String displayName, Material iconMaterial, List<WorkBlock> blocks) {
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.blocks = blocks;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<WorkBlock> getBlocks() {
        return blocks;
    }

    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(iconMaterial);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(displayName);
        icon.setItemMeta(meta);
        return icon;
    }

    public static String[] names() {
        return Arrays.stream(values()).map(Enum::name).toArray(String[]::new);
    }

    public boolean isLibrary() {
        return this == LIBRARY;
    }
    
    public boolean isTrader() {
        return this == TRADER;
    }
}

// Класс WorkBlock
class WorkBlock {
    private final Material material;
    private final String customName;

    public WorkBlock(Material material, String customName) {
        this.material = material;
        this.customName = customName;
    }

    public Material getMaterial() {
        return material;
    }

    public String getCustomName() {
        return customName;
    }
}