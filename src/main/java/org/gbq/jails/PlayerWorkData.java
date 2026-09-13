package org.gbq.jails;

import java.util.HashMap;
import java.util.UUID;

public class PlayerWorkData {
    private int washedClothesCount;
    private int deliveredBricksCount;

    public void incrementWashedClothes() {
        washedClothesCount++;
    }

    public void incrementDeliveredBricks() {
        deliveredBricksCount++;
    }

    public int getWashedClothesCount() {
        return washedClothesCount;
    }

    public int getDeliveredBricksCount() {
        return deliveredBricksCount;
    }
}
