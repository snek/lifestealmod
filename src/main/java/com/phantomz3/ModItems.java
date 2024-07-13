package com.phantomz3;

import com.phantomz3.Items.HeartItem;
import com.phantomz3.Items.ReviveStarItem;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static Item register(Item item, String id) {
        Identifier itemID = Identifier.of(LifestealMod.MOD_ID, id);

        Item registeredItem = Registry.register(Registries.ITEM, itemID, item);

        return registeredItem;
    }

    public static final Item HEART = register(
            new HeartItem(new Item.Settings()), "heart"
    );

    public static final Item REVIVE_STAR = register(
        new ReviveStarItem(new Item.Settings()), "revive_star"
    );

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register((itemGroup) -> {
                    itemGroup.add(ModItems.HEART);
                    itemGroup.add(ModItems.REVIVE_STAR);
                });

    }
}
