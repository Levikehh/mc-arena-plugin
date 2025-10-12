package me.levikehh.arena.models;

import org.bukkit.Location;

public class Arena {
    private final String name;
    private final Location spawn1;
    private final Location spawn2;
    private boolean occupied;

    public Arena(String name, Location spawn1, Location spawn2) {
        this.name = name;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.occupied = false;
    }

    public String getName() {
        return this.name;
    }

    public Location getSpawn1() {
        return this.spawn1;
    }

    public Location getSpawn2() {
        return this.spawn2;
    }

    public boolean isOccupied() {
        return this.occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    @Override
    public String toString() {
        return String.format("Arena{name='%s', occupied=%s}", this.name, this.occupied);
    }
}
