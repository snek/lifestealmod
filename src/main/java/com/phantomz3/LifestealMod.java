package com.phantomz3;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.Command;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.item.Items;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class LifestealMod implements ModInitializer {
	public static final String MOD_ID = "lifestealmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		ModItems.initialize();

		registerConfig();
		registerEvents();
		registerCommands();    
		registerReviveCommand();
	}

	private void registerConfig() {
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
	}

	private void registerEvents() {
		// Handle player death and drop heart
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof PlayerEntity) {
				PlayerEntity player = (PlayerEntity) entity;

				// Drop a heart item on death
				if(source.getAttacker() instanceof PlayerEntity) {
					ItemStack heartStack = new ItemStack(ModItems.HEART);
					player.dropItem(heartStack, true);
				}

				// Decrease the player's max health
				double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
				// Send a message to the player's action bar
				player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
				 			.setBaseValue(playerMaxHealth - 2.0);
				player.sendMessage(Text.translatable("message.lifestealmod.lost_health").formatted(Formatting.RED), true);

				// Update the player's max health after decreasing it
				playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);

				// If the player reaches 0 hearts, set the player to spectator mode
				if (playerMaxHealth <= 1.0) {
					((ServerPlayerEntity) player).changeGameMode(GameMode.SPECTATOR);
					player.sendMessage(Text.translatable("message.lifestealmod.spectator_mode").formatted(Formatting.GRAY), true);

					player.setHealth(1.0f);
					// Return false to prevent the player from dying
					return false;
				}
			}

			return true;
		});

		// Handle player killing another entity
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed) -> {
			if (entity instanceof PlayerEntity) {
				PlayerEntity player = (PlayerEntity) entity;
				ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
		
				if (killed instanceof PlayerEntity) {
					// if player already has max hearts, give them heart item
					if (player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH) >= config.maxHeartCap) {
						ItemStack heartStack = new ItemStack(ModItems.HEART);
						player.dropItem(heartStack, true);
						return;
					}
		
					increasePlayerHealth(player);
					player.sendMessage(Text.translatable("message.lifestealmod.gained_health").formatted(Formatting.GRAY), true);
				}
				
			}
		});
		
	}

	private void increasePlayerHealth(PlayerEntity player) {
		ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
		double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
		if (playerMaxHealth < config.maxHeartCap) { // Use the configured max heart cap
			player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
					.setBaseValue(MathHelper.clamp(playerMaxHealth + 2.0, 0.0, config.maxHeartCap));
		}
	}
	
	// creating a /withdraw command: /withdraw <amount> and it should remove the amount from the player's inventory and give them heart items
	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(CommandManager.literal("withdraw")
					.requires(source -> source.hasPermissionLevel(0))
					.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
							.executes(context -> {
								ServerCommandSource source = context.getSource();
								int amount = IntegerArgumentType.getInteger(context, "amount");
								// inventory
								Inventory inventory = source.getPlayer().getInventory();

								// Get the player
								ServerPlayerEntity player = source.getPlayer();

								// Get the player's inventory
								// Inventory inventory = player.getInventory();

								// Remove the specified amount of items from the player's inventory
								// inventory.removeItems(ModItems.HEART, amount);

								// checking if the player has the specified amount of hearts in there total hearts
								int totalHearts = (int) player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH) / 2;
								// (totalHearts - 2) because we don't want the player to have 0 hearts
								if ((totalHearts - 1) < amount) {
									player.sendMessage(Text.translatable("message.lifestealmod.not_enough_hearts").formatted(Formatting.RED), true);
									return Command.SINGLE_SUCCESS;
								}

								// if the player has 0 hearts, do not give them any more hearts
								if (player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH) <= 1.0) {
									player.sendMessage(Text.translatable("message.lifestealmod.spectator_mode").formatted(Formatting.GRAY), true);
									return Command.SINGLE_SUCCESS;
								}

								// Decrease the player's max health by the specified amount
								double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
								player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
										.setBaseValue(playerMaxHealth - (amount * 2.0));

								// Give the player the specified amount of heart items
								for (int i = 0; i < amount; i++) {
									ItemStack heartStack = new ItemStack(ModItems.HEART);
									player.dropItem(heartStack, true);
								}

								return Command.SINGLE_SUCCESS;
							})));
		});
	}

	private void registerReviveCommand() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(CommandManager.literal("revive")
					.requires(source -> source.hasPermissionLevel(0))
					.then(CommandManager.argument("player", EntityArgumentType.player())
							.executes(context -> {
								ServerCommandSource source = context.getSource();
								ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
								ServerPlayerEntity executingPlayer = source.getPlayer();
	
								double executingPlayerMaxHealth = executingPlayer.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
								ItemStack netheriteIngotStack = new ItemStack(Items.NETHERITE_INGOT, 4);

								// cannot revive himself or players with above 1 heart
								if (executingPlayer == targetPlayer || targetPlayer.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH) > 1.0) {
									executingPlayer.sendMessage(Text.translatable("message.lifestealmod.cannot_revive").formatted(Formatting.RED), true);
									return Command.SINGLE_SUCCESS;
								}
	
								if (executingPlayer.getInventory().count(ModItems.HEART) >= 4 && executingPlayer.getInventory().count(Items.NETHERITE_INGOT) >= 4) {
									// Remove 4 heart items from the executing player's inventory
									executingPlayer.getInventory().removeStack(executingPlayer.getInventory().getSlotWithStack(new ItemStack(ModItems.HEART)), 4);
	
									// Remove 4 netherite ingots from the executing player's inventory
									executingPlayer.getInventory().removeStack(executingPlayer.getInventory().getSlotWithStack(netheriteIngotStack), 4);

									// Remove 4 heart items from the e
									executingPlayer.getInventory().removeStack(targetPlayer.getInventory().getSlotWithStack(new ItemStack(ModItems.HEART)), 4);
	
									// Revive the target player
									targetPlayer.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(8.0);
									targetPlayer.changeGameMode(GameMode.SURVIVAL);
									targetPlayer.setHealth(8.0f);
	
									targetPlayer.sendMessage(Text.translatable("message.lifestealmod.revive_success").formatted(Formatting.GREEN), true);
									source.sendFeedback(() -> Text.translatable("message.lifestealmod.revive_success").formatted(Formatting.GREEN), false);
								} else {
									source.sendFeedback(() -> Text.translatable("message.lifestealmod.revive_fail").formatted(Formatting.RED), false);
								}
	
								return Command.SINGLE_SUCCESS;
							})));
		});
	}
		
}