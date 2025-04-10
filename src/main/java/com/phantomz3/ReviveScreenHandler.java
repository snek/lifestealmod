package com.phantomz3;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

public class ReviveScreenHandler extends GenericContainerScreenHandler {

    public ReviveScreenHandler(int syncId, PlayerInventory playerInventory, SimpleInventory inventory) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, 3);
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

        // Check if the clicked item is a player head with a glint
        if (clickedStack.getItem() == Items.PLAYER_HEAD) {
            String playerName = clickedStack.get(DataComponentTypes.ITEM_NAME).getString();
            ServerPlayerEntity target = ((ServerPlayerEntity) player).getServer().getPlayerManager()
                    .getPlayer(playerName);

            if (target != null) {
                // Revive the player
                executeRevive_((ServerPlayerEntity) player, target, false);

                // Remove the player head from the GUI after revival
                this.slots.get(slotId).setStack(ItemStack.EMPTY);
                this.sendContentUpdates(); // Notify the client to refresh the GUI

                // Closing the GUI after revival
                ((ServerPlayerEntity) player).closeHandledScreen();
            }
        }

        if (clickedStack.getItem() == Items.GRAY_STAINED_GLASS_PANE) {
            // Closing the GUI when the player clicks on the gray stained glass pane
            ((ServerPlayerEntity) player).closeHandledScreen();
        }

        // Call the parent class method to ensure normal behavior for other slots
        super.onSlotClick(slotId, button, actionType, player);
    }

    private int executeRevive_(ServerPlayerEntity player, ServerPlayerEntity target, boolean isOpRevive) {
        // If the player is reviving themselves or a player who is not dead, return 0
        if (player == target || target.getHealth() > 2.0f) {
            player.sendMessage(Text.literal("Reviving the player failed!").formatted(Formatting.RED), true);
            return 0;
        }

        double currentMaxHealth = target.getAttributeBaseValue(EntityAttributes.MAX_HEALTH);
        double newMaxHealth = Math.max(2.0, currentMaxHealth + 8.0); // 20 hearts = 40 health

        target.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(newMaxHealth);
        target.setHealth((float) newMaxHealth); // Set player's health to the new max health

        // changing the player's gamemode to survival
        target.changeGameMode(GameMode.SURVIVAL);
        player.sendMessage(Text.literal("Succesfully revived the player!").formatted(Formatting.GREEN),
                true);

        // loop through player items and remove revive beacon
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack itemStack = player.getInventory().getStack(i);
            if (itemStack.getItem() == Items.BEACON && itemStack.hasGlint()
                    && itemStack.getName().getString().equals("Revive Beacon")) {
                itemStack.decrement(1);
                break;
            }
        }

        return 1;
    }
}
