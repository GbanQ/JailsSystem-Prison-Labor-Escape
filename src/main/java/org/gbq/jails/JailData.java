package org.gbq.jails;

import org.bukkit.Location;

public class JailData {
    private Location cellLocation;
    private int time;

    private boolean isEscaped;

    public JailData(Location cellLocation, int time) {
        this.cellLocation = cellLocation;
        this.time = time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    private boolean escaped;

    public boolean isEscaped() {
        return escaped;
    }

    public void setEscaped(boolean escaped) {
        this.escaped = escaped;
    }

    public Location getCellLocation() {
        return cellLocation;
    }

    public int getTime() {
        return time;
    }

    public void decrementTime() {
        if (time > 0) {
            time--;
        }
    }
}
