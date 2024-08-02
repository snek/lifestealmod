package com.phantomz3;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import java.util.Collection;

public class LifestealMod implements ModInitializer {
	public static final String MOD_ID = "lifestealmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

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
				LivingEntity attacker = (LivingEntity) source.getAttacker();

				if (!(attacker instanceof PlayerEntity) || (int) attacker.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH) > 40) {
					ItemStack heartStack = createCustomNetherStar("Heart", "Right-click to redeem");
					player.dropItem(heartStack, true);
				}

				// Decrease the player's max health
				double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
				player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(playerMaxHealth - 2.0);
				player.sendMessage(Text.translatable("message.lifestealmod.lost_health").formatted(Formatting.RED), true);

				// Update the player's max health after decreasing it
				playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);

				// If the player reaches 0 hearts, set the player to spectator mode
				if (playerMaxHealth <= 1.0) {
					((ServerPlayerEntity) player).changeGameMode(GameMode.SPECTATOR);
					player.sendMessage(Text.translatable("message.lifestealmod.spectator_mode").formatted(Formatting.GRAY), true);

					player.setHealth(1.0f);
					player.getInventory().dropAll();
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
					increasePlayerHealth(player);
					player.sendMessage(Text.translatable("message.lifestealmod.gained_health").formatted(Formatting.GRAY), true);
				}
			}
		});

		ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

		// Making the totem of undying disappear when the player holds it
		if (config.disableTotem) {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				server.getPlayerManager().getPlayerList().forEach(player -> {
					ItemStack mainHandStack = player.getMainHandStack();
					ItemStack offHandStack = player.getOffHandStack();

					if (mainHandStack.getItem() == Items.TOTEM_OF_UNDYING || offHandStack.getItem() == Items.TOTEM_OF_UNDYING) {
						player.getMainHandStack().decrement(1);
						player.getOffHandStack().decrement(1);
					}
				});
			});
		}

		// Making crystals disappear when the player holds it
		if (config.disableCPVP) {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				server.getPlayerManager().getPlayerList().forEach(player -> {
					ItemStack mainHandStack = player.getMainHandStack();
					ItemStack offHandStack = player.getOffHandStack();

					if (mainHandStack.getItem() == Items.END_CRYSTAL || offHandStack.getItem() == Items.END_CRYSTAL) {
						player.getMainHandStack().decrement(1);
						player.getOffHandStack().decrement(1);
					}
				});
			});
		}

		if (config.disableEnderPearl) {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				server.getPlayerManager().getPlayerList().forEach(player -> {
					ItemStack mainHandStack = player.getMainHandStack();
					ItemStack offHandStack = player.getOffHandStack();

					if (mainHandStack.getItem() == Items.ENDER_PEARL || offHandStack.getItem() == Items.ENDER_PEARL) {
						player.getMainHandStack().decrement(1);
						player.getOffHandStack().decrement(1);
					}
				});
			});
		}

		// Prevent players from using respawn anchors
		if (config.disableCPVP) {
			UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
				if (world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof RespawnAnchorBlock) {
					return ActionResult.FAIL;
				}
				return ActionResult.PASS;
			});
		}

		// disable netherite armor + sword
		if (config.disableNetherite) {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				server.getPlayerManager().getPlayerList().forEach(player -> {
					ItemStack mainHandStack = player.getMainHandStack();
					ItemStack offHandStack = player.getOffHandStack();

					if (mainHandStack.getItem() == Items.NETHERITE_SWORD || offHandStack.getItem() == Items.NETHERITE_SWORD) {
						player.getMainHandStack().decrement(1);
						player.getOffHandStack().decrement(1);
					}

					if (mainHandStack.getItem() == Items.NETHERITE_AXE || offHandStack.getItem() == Items.NETHERITE_AXE) {
						player.getMainHandStack().decrement(1);
						player.getOffHandStack().decrement(1);
					}

					if (player.getEquippedStack(EquipmentSlot.HEAD).getItem() == Items.NETHERITE_HELMET) {
						player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
					}

					if (player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.NETHERITE_CHESTPLATE) {
						player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
					}

					if (player.getEquippedStack(EquipmentSlot.LEGS).getItem() == Items.NETHERITE_LEGGINGS) {
						player.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
					}

					if (player.getEquippedStack(EquipmentSlot.FEET).getItem() == Items.NETHERITE_BOOTS) {
						player.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
					}
				});
			});
		}

		if (config.noDragonEggEnderChest) {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				server.getPlayerManager().getPlayerList().forEach(player -> {
					EnderChestInventory enderChestInventory = player.getEnderChestInventory();

					for (int i = 0; i < enderChestInventory.size(); i++) {
						if (enderChestInventory.getStack(i).getItem() == Items.DRAGON_EGG) {
							enderChestInventory.setStack(i, ItemStack.EMPTY);
						}
					}
				});
			});
		}

		// if the player has more golden apples than the configured cap, remove the excess
		if (config.goldenAppleCap < 64) {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				server.getPlayerManager().getPlayerList().forEach(player -> {
					Inventory inventory = player.getInventory();
					int goldenAppleCount = 0;

					for (int i = 0; i < inventory.size(); i++) {
						ItemStack stack = inventory.getStack(i);
						if (stack.getItem() == Items.GOLDEN_APPLE) {
							goldenAppleCount += stack.getCount();
							if (goldenAppleCount > config.goldenAppleCap) {
								stack.setCount(0);
							}
						}
					}
				});
			});
		}

		// Right-click heart to gain health
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack itemStack = player.getStackInHand(hand);

			if (itemStack.getItem() == Items.NETHER_STAR && !(itemStack.hasGlint())
					&& itemStack.getName().getString().equals("Heart")) {

				if (player instanceof ServerPlayerEntity) {
					ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

					// health cap
					ModConfig modConfig = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
					double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
					if (playerMaxHealth >= modConfig.maxHeartCap) {
						player.sendMessage(Text.translatable("message.lifestealmod.max_health").formatted(Formatting.RED), true);
						return TypedActionResult.fail(itemStack);
					}

					player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(playerMaxHealth + 2.0);

					player.sendMessage(Text.translatable("message.lifestealmod.gained_health").formatted(Formatting.GREEN), true);

					// Decrease the item stack size
					itemStack.decrement(1);

					return TypedActionResult.success(itemStack);
				}
			}

			return TypedActionResult.pass(itemStack);

		});
	}

	private ItemStack createCustomNetherStar(String name, String lore) {
		ItemStack heartStack = new ItemStack(Items.NETHER_STAR);
		heartStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
		heartStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false);
		return heartStack;
	}

	private void increasePlayerHealth(PlayerEntity player) {
		double playerMaxHealth = player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
		player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(playerMaxHealth + 2.0);
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("lifesteal")
					.then(CommandManager.literal("withdraw")
							.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
									.executes(context -> {
										ServerCommandSource source = context.getSource();
										ServerPlayerEntity player = source.getPlayer();
										int amount = IntegerArgumentType.getInteger(context, "amount");

										double currentMaxHealth = player.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
										double newMaxHealth = Math.max(2.0, currentMaxHealth - amount * 2.0); // 1 heart = 2.0 health

										player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(newMaxHealth);
										player.setHealth((float) newMaxHealth); // Set player's health to the new max health

										// give the player the heart items
										for (int i = 0; i < amount; i++) {
											ItemStack heartStack = createCustomNetherStar("Heart", "Right-click to redeem");
											player.getInventory().insertStack(heartStack);
										}

										return 1;
									})))
					.then(CommandManager.literal("give")
							.requires(source -> source.hasPermissionLevel(2))
							.then(CommandManager.argument("targets", EntityArgumentType.players())
									.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
											.executes(context -> {
												Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");
												int amount = IntegerArgumentType.getInteger(context, "amount");

												for (ServerPlayerEntity target : targets) {
													double currentMaxHealth = target.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
													double newMaxHealth = Math.max(2.0, currentMaxHealth + amount * 2.0); // 1 heart = 2.0 health

													target.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(newMaxHealth);
													target.setHealth((float) newMaxHealth); // Set player's health to the new max health
												}

												return targets.size();
											})))));
		});
	}

	private void registerReviveCommand() {
		// making the revive command requiring the player to have 4 heart items and 4 netherite ingots
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("revive")
					.then(CommandManager.argument("player", EntityArgumentType.player())
							.executes(context -> {
								ServerCommandSource source = context.getSource();
								ServerPlayerEntity player = source.getPlayer();
								ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

								Inventory inventory = player.getInventory();
								int heartCount = 0;
								int netheriteIngotCount = 0;

								for (int i = 0; i < inventory.size(); i++) {
									ItemStack stack = inventory.getStack(i);
									if (stack.getItem() == Items.NETHER_STAR && !(stack.hasGlint())) {
										heartCount += stack.getCount();
									} else if (stack.getItem() == Items.NETHERITE_INGOT) {
										netheriteIngotCount += stack.getCount();
									}
								}

								if (heartCount >= 4 && netheriteIngotCount >= 4) {
									double currentMaxHealth = target.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
									double newMaxHealth = Math.max(2.0, currentMaxHealth + 40.0); // 20 hearts = 40 health

									target.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(newMaxHealth);
									target.setHealth((float) newMaxHealth); // Set player's health to the new max health

									// remove the heart items and netherite ingots from the player's inventory
									for (int i = 0; i < inventory.size(); i++) {
										ItemStack stack = inventory.getStack(i);
										if (stack.getItem() == Items.NETHER_STAR) {
											stack.setCount(0);
										} else if (stack.getItem() == Items.NETHERITE_INGOT) {
											stack.setCount(0);
										}
									}

									player.sendMessage(Text.translatable("message.lifestealmod.revive_success").formatted(Formatting.GREEN), true);
								} else {
									player.sendMessage(Text.translatable("message.lifestealmod.revive_fail").formatted(Formatting.RED), true);
								}

								return 1;
							})));
		});
	}
}
