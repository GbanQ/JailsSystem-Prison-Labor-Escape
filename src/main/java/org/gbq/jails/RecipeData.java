package org.gbq.jails;

import org.bukkit.Material;

import java.util.List;

public class RecipeData {
    private final String key;
    private final String displayName;
    private final List<Material> ingredients;
    private final Material result;
    private final int minTime;
    private final int maxTime;
    private final int reward;

    public RecipeData(String key, String displayName, List<Material> ingredients, Material result,
            int minTime, int maxTime, int reward) {
        this.key = key;
        this.displayName = displayName;
        this.ingredients = ingredients;
        this.result = result;
        this.minTime = minTime;
        this.maxTime = maxTime;
        this.reward = reward;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Material> getIngredients() {
        return ingredients;
    }

    public Material getResult() {
        return result;
    }

    public int getMinTime() {
        return minTime;
    }

    public int getMaxTime() {
        return maxTime;
    }

    public int getReward() {
        return reward;
    }
}