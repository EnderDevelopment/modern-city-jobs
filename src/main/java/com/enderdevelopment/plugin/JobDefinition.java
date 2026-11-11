package com.enderdevelopment.plugin;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public record JobDefinition(String id, String display, Material icon, String color, String task,
                            String validation, List<Material> materials) {
    public static JobDefinition from(ConfigurationSection section, String id) {
        Material icon = Material.matchMaterial(section.getString("icon", "PAPER"));
        if (icon == null) icon = Material.PAPER;
        List<Material> materials = section.getStringList("valid-materials").stream()
                .map(Material::matchMaterial).filter(java.util.Objects::nonNull).toList();
        return new JobDefinition(id, section.getString("display", id), icon,
                section.getString("color", "WHITE"), section.getString("task", "Complete tasks"),
                section.getString("validation", "RIGHT_CLICK_BLOCK").toUpperCase(java.util.Locale.ROOT), materials);
    }
}
