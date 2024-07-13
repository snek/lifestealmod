// ReviveStarItem.java
package com.phantomz3.Items;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class ReviveStarItem extends Item {
    public ReviveStarItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (!world.isClient) {
            // List players with 0 hearts
            List<ServerPlayerEntity> zeroHeartPlayers = world.getServer().getPlayerManager().getPlayerList().stream()
                    .filter(p -> p.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH) <= 1.0)
                    .toList();

            if (zeroHeartPlayers.isEmpty()) {
                player.sendMessage(Text.translatable("message.lifestealmod.no_zero_heart_players").formatted(Formatting.RED), true);
            } else {
                player.sendMessage(Text.translatable("message.lifestealmod.zero_heart_players").formatted(Formatting.YELLOW), false);
                for (ServerPlayerEntity zeroHeartPlayer : zeroHeartPlayers) {
                    player.sendMessage(Text.literal(zeroHeartPlayer.getName().getString()).formatted(Formatting.RED), false);
                }
                player.sendMessage(Text.translatable("message.lifestealmod.revive_instructions").formatted(Formatting.GREEN), false);
            }
        }

        return TypedActionResult.success(itemStack, world.isClient());
    }

}
