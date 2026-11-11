package com.enderdevelopment.plugin;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventExecutor;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class CitizensHook {
    private final Main plugin;
    public CitizensHook(Main plugin) { this.plugin = plugin; }
    @SuppressWarnings("unchecked")
    public void enable() {
        try {
            Class<?> eventClass = Class.forName("net.citizensnpcs.api.event.NPCRightClickEvent");
            EventExecutor executor = (listener, event) -> {
                try {
                    Method clicker = event.getClass().getMethod("getClicker");
                    Object value = clicker.invoke(event);
                    if (value instanceof Player player) {
                        player.performCommand("jobs");
                    }
                } catch (ReflectiveOperationException exception) {
                    plugin.getLogger().warning("Citizens NPC interaction could not be read: " + exception.getMessage());
                }
            };
            Bukkit.getPluginManager().registerEvent((Class<? extends Event>) eventClass, new Listener() {}, EventPriority.NORMAL, executor, plugin);
            plugin.getLogger().info("Citizens detected: NPC right clicks open the city job center.");
        } catch (ClassNotFoundException ignored) {
            plugin.getLogger().info("Citizens not installed; /jobs remains available as the job center.");
        }
    }
}
