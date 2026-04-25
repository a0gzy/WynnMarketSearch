package me.a0g.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "wms")
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    public static final String CONFIG_CATEGORY = "general";

    @Comment("Enable market search when chat prompt appears")
    @ConfigEntry.Gui.TransitiveObject
    public boolean marketSearch = true;

    @Comment("Auto-focus search input when GUI opens")
    @ConfigEntry.Gui.TransitiveObject
    public boolean autoFocus = true;

    @Comment("URL of the WynnMarketSearch item-database backend (override to self-host)")
    @ConfigEntry.Gui.Tooltip
    public String apiUrl = "https://wms-site-api.vercel.app/api/items";

    public static ModConfig get() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }
}
