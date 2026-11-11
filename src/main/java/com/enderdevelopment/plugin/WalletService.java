package com.enderdevelopment.plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class WalletService {
    private final Main plugin;
    private final File file;
    private YamlConfiguration data;
    public WalletService(Main plugin) {
        this.plugin = plugin; this.file = new File(plugin.getDataFolder(), "wallets.yml");
        load();
    }
    private void load() { data = YamlConfiguration.loadConfiguration(file); }
    public synchronized int balance(UUID uuid) { return Math.max(0, data.getInt(uuid.toString(), 0)); }
    public synchronized boolean add(UUID uuid, int amount) {
        if (amount <= 0) return false;
        data.set(uuid.toString(), balance(uuid) + amount); save(); return true;
    }
    public synchronized boolean remove(UUID uuid, int amount) {
        if (amount <= 0 || balance(uuid) < amount) return false;
        data.set(uuid.toString(), balance(uuid) - amount); save(); return true;
    }
    public synchronized void set(UUID uuid, int amount) { data.set(uuid.toString(), Math.max(0, amount)); save(); }
    private void save() { try { data.save(file); } catch (IOException e) { plugin.getLogger().severe("Could not save wallet: " + e.getMessage()); } }
    public void notify(Player player, String message) { player.sendMessage(plugin.message(message.replace("{balance}", String.valueOf(balance(player.getUniqueId()))))); }
}
