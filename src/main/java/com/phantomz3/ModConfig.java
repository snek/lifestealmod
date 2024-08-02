package com.phantomz3;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = LifestealMod.MOD_ID)
public class ModConfig implements ConfigData {
    public int maxHeartCap = 40; // Default to 20 hearts (40 health)
    public int goldenAppleCap = 16; // Default to 64
    public boolean disableEnderPearl = true; // Default to false
    public boolean disableCPVP = true; // Default to false
    public boolean disableTotem = true; // Default to false
    public boolean disableNetherite = true; // Default to false
    public boolean noDragonEggEnderChest = true; // Default to false
}
