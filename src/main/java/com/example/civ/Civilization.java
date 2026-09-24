package com.example.civ;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Civilization {
    private String name;
    private UUID owner;
    private Map<UUID, CivRank> members = new HashMap<>();

    // Base location
    private String baseWorld;
    private int baseX, baseY, baseZ;
    private boolean hasBase = false;

    /** No-arg constructor required by Gson. */
    public Civilization() {}

    public Civilization(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.members.put(owner, CivRank.OWNER);
    }

    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public Map<UUID, CivRank> getMembers() { return members; }

    public boolean hasBase() { return hasBase; }
    public String getBaseWorld() { return baseWorld; }
    public int getBaseX() { return baseX; }
    public int getBaseY() { return baseY; }
    public int getBaseZ() { return baseZ; }

    public void setBase(String world, int x, int y, int z) {
        this.baseWorld = world;
        this.baseX = x;
        this.baseY = y;
        this.baseZ = z;
        this.hasBase = true;
    }

    public int getMemberCount() { return members.size(); }

    public CivRank getRank(UUID uuid) { return members.get(uuid); }

    public void addMember(UUID uuid) { members.putIfAbsent(uuid, CivRank.MEMBER); }

    public void removeMember(UUID uuid) { members.remove(uuid); }

    public void setRank(UUID uuid, CivRank rank) {
        if (members.containsKey(uuid)) members.put(uuid, rank);
    }

    public boolean isOwner(UUID uuid) { return uuid.equals(owner); }

    /**
     * Crown tier based on member count.
     *  0 = no crown benefits yet (below 10)
     *  1 = 10–19 members
     *  2 = 20–29 members
     *  3 = 30+ members
     */
    public int getCrownTier() {
        int count = members.size();
        if (count >= 30) return 3;
        if (count >= 20) return 2;
        if (count >= 10) return 1;
        return 0;
    }
}
