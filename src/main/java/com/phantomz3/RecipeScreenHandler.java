package com.phantomz3;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

public class RecipeScreenHandler extends GenericContainerScreenHandler{

    public RecipeScreenHandler(int syncId, PlayerInventory playerInventory, SimpleInventory inventory) {
        super(ScreenHandlerType.GENERIC_9X5, syncId, playerInventory, inventory, 5);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true; // Allow players to open the GUI
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public void onSlotClick(int slotId, int button, SlotActionType actionType, PlayerEntity player) {
        ItemStack clickedStack = this.slots.get(slotId).getStack();
        // Closing the gui as soon as any item is clicked
        if (clickedStack != ItemStack.EMPTY) {
            ((ServerPlayerEntity) player).closeHandledScreen();
        }
    }
}
