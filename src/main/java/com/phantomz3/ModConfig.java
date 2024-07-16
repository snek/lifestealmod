package com.phantomz3;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = LifestealMod.MOD_ID)
public class ModConfig implements ConfigData {
    public int maxHeartCap = 40; // Default to 20 hearts (40 health)
}
