package com.phantomz3.Items;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;

import com.phantomz3.ModConfig;

import me.shedaniel.autoconfig.AutoConfig;

public class HeartItem extends Item {
    public HeartItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.lifestealmod.heart.tooltip").formatted(Formatting.RED));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        // config
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        double maxHealth = player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
        double healthCap = config.maxHeartCap;

        if (!world.isClient) {
            if (maxHealth < healthCap) {
                // Increase the player's max health by one heart (2 health points)
                player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                        .setBaseValue(MathHelper.clamp(maxHealth + 2.0, 0.0, healthCap));

                // Play a sound effect
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 1.0F, 1.0F);

                // Consume the item
                if (!player.isCreative()) {
                    itemStack.decrement(1);
                }
            } else {
                // Send a message to the player's action bar
                player.sendMessage(Text.translatable("message.lifestealmod.max_health_reached").formatted(Formatting.RED), true);
            }
        }

        return TypedActionResult.success(itemStack, world.isClient());
    }



}
