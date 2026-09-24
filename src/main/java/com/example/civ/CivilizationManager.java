package com.example.civ;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CivilizationManager {

    private final CivPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** civ name (lowercase) -> Civilization */
    private final Map<String, Civilization> civs = new HashMap<>();
    /** player UUID -> civ name (lowercase) */
    private final Map<UUID, String> playerToCiv = new HashMap<>();

    private final File dataFile;

    public CivilizationManager(CivPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "civilizations.json");
    }

    // ---------- Persistence ----------

    @SuppressWarnings("unchecked")
    public void load() {
        if (!dataFile.exists()) return;
        try (Reader reader = new InputStreamReader(
                new FileInputStream(dataFile), StandardCharsets.UTF_8)) {

            Type type = new TypeToken<SaveData>(){}.getType();
            SaveData data = gson.fromJson(reader, type);
            if (data == null) return;

            civs.clear();
            playerToCiv.clear();

            if (data.civs != null) {
                for (Civilization civ : data.civs) {
                    civs.put(civ.getName().toLowerCase(), civ);
                    for (UUID uuid : civ.getMembers().keySet()) {
                        playerToCiv.put(uuid, civ.getName().toLowerCase());
                    }
                }
            }
            plugin.getLogger().info("Loaded " + civs.size() + " civilizations.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load civilizations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

            SaveData data = new SaveData();
            data.civs = new ArrayList<>(civs.values());

            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
                gson.toJson(data, writer);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save civilizations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Wrapper for Gson serialization. */
    private static class SaveData {
        List<Civilization> civs = new ArrayList<>();
    }

    // ---------- Queries ----------

    public Civilization getCiv(String name) {
        return civs.get(name.toLowerCase());
    }

    public Civilization getCivOf(UUID uuid) {
        String name = playerToCiv.get(uuid);
        return name == null ? null : civs.get(name);
    }

    public Collection<Civilization> getAll() { return civs.values(); }

    // ---------- Mutations ----------

    public boolean create(String name, UUID owner) {
        if (civs.containsKey(name.toLowerCase())) return false;
        Civilization civ = new Civilization(name, owner);
        civs.put(name.toLowerCase(), civ);
        playerToCiv.put(owner, name.toLowerCase());
        save();
        return true;
    }

    public boolean disband(String name) {
        Civilization civ = civs.remove(name.toLowerCase());
        if (civ == null) return false;
        for (UUID u : civ.getMembers().keySet()) playerToCiv.remove(u);
        save();
        return true;
    }

    public void join(UUID uuid, Civilization civ) {
        civ.addMember(uuid);
        playerToCiv.put(uuid, civ.getName().toLowerCase());
        save();
    }

    public void leave(UUID uuid) {
        Civilization civ = getCivOf(uuid);
        if (civ == null) return;
        civ.removeMember(uuid);
        playerToCiv.remove(uuid);
        if (civ.getMemberCount() == 0) civs.remove(civ.getName().toLowerCase());
        save();
    }

    /** Broadcast a message to every online member of the civ. */
    public void broadcast(Civilization civ, net.kyori.adventure.text.Component msg) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (civ.getRank(p.getUniqueId()) != null) {
                p.sendMessage(msg);
            }
        }
    }
}
