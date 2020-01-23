package define.main;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import world.bentobox.bentobox.BentoBox;

public class AbyssListener implements Listener {
	EcoManager eco = new EcoManager();

	private BentoBox bento = (BentoBox) AbyssSkyblock.plugin.getServer().getPluginManager().getPlugin("BentoBox");

	public ItemStack getMenuStar() {
		ItemStack menustar = new ItemStack(Material.NETHER_STAR);
		ItemMeta meta = (ItemMeta) menustar.getItemMeta();
		List<String> lore = new ArrayList<String>();

		lore.add(ChatColor.GREEN + "Opens the main menu");
		meta.setLore(lore);
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.AQUA + "Menu");
		menustar.setItemMeta(meta);

		return menustar;
	}

	// Opens the menu when the player right clicks something
	@EventHandler
	public void onPlayerClick(PlayerInteractEvent event) {
		if (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			if (event.getItem() != null && event.getItem().equals(getMenuStar())) {
				event.setCancelled(true);
				event.getPlayer().chat("/menu");
			}
		}
	}

	@EventHandler
	public void onOffhandEvent(PlayerSwapHandItemsEvent event) {
		if (event.getOffHandItem().equals(getMenuStar())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryMoveItemEvent(InventoryMoveItemEvent event) {
		if (event.getItem().equals(getMenuStar())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClickEvent(InventoryClickEvent event) {

		if (AbyssSkyblock.plugin.inventories.contains(event.getView().getTitle())) {
			if (event.getCurrentItem() != null) {
				// For the buy shop that works by clicking items in the inventory
				try {
					// Null check for first condition
					if (event.getClickedInventory().getItem(event.getSlot()).getItemMeta().hasLore()
							&& event.getClickedInventory().getItem(event.getSlot()).getItemMeta().getLore().get(0)
									.contains("Sell Price")) {
						int amount = event.getClickedInventory().getItem(event.getSlot()).getAmount();
						double sellPrice = eco
								.getSellPrice(event.getClickedInventory().getItem(event.getSlot()).getType()) * amount;
						ItemStack item = event.getClickedInventory().getItem(event.getSlot());

						eco.sellItem(event.getClickedInventory().getItem(event.getSlot()).getType(),
								event.getClickedInventory().getItem(event.getSlot()).getAmount());

						event.getWhoClicked().getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR, 1));

						eco.addAmount(sellPrice, (Player) event.getWhoClicked());

						addSellPricesMeta(event.getWhoClicked().getInventory(), (Player) event.getWhoClicked());

						event.getWhoClicked()
								.sendMessage("You have sold " + item.getAmount() + " "
										+ item.getType().toString().toLowerCase() + ChatColor.RESET + " for "
										+ Math.round(sellPrice * 100) * 0.01 + " Minecoins.");

						// Minecoins is the currency name, since Java Edition needs Minecoins like
						// bedrock.
						// Microsoft needs more money.

						// Update prices on menu by reopening this menu
						updatePrices(event.getInventory(), (Player) event.getWhoClicked());
					}

				} catch (NullPointerException exeption) {
					// Occurs when clicking nothing
				}
			}

			// Everything that involves reading from the config of this is below this line,
			// and is from the upper inventory
			if (event.getWhoClicked().getOpenInventory().getTopInventory().equals(event.getClickedInventory())) {
				for (String key : AbyssSkyblock.plugin.configList.keySet()) {
					if (AbyssSkyblock.plugin.configList.get(key).getString("name")
							.equalsIgnoreCase(event.getWhoClicked().getOpenInventory().getTitle())) {

						FileConfiguration returnedConfig = AbyssSkyblock.plugin.configList.get(key);
						// Number of items in the slot clicked
						int amount = returnedConfig.getInt(event.getSlot() + ".amount");

						if (returnedConfig.getString(event.getSlot() + ".item") != null) {

							String action = returnedConfig.getString(event.getSlot() + ".action");

							if (returnedConfig.getString(event.getSlot() + ".showpricesininventory") != null
									&& returnedConfig.getString(event.getSlot() + ".showpricesininventory")
											.equalsIgnoreCase("true")) {
								addSellPricesMeta(event.getWhoClicked().getInventory(), (Player) event.getWhoClicked());

							}

							// Opens another menu
							if (action.equals("menu")) {
								// For first island expansions bought
								if (returnedConfig.getString(event.getSlot() + ".permissionmenu") != null) {
									if (event.getWhoClicked()
											.hasPermission(returnedConfig.getString(event.getSlot() + ".permission"))) {
										AbyssSkyblock.plugin.openInventoryFromConfig((Player) event.getWhoClicked(),
												returnedConfig.getString(event.getSlot() + ".permissionmenu"));
									}
								} else {
									AbyssSkyblock.plugin.openInventoryFromConfig((Player) event.getWhoClicked(),
											returnedConfig.getString(event.getSlot() + ".menu"));
								}
							}
							// Makes the player run something though chat, the player needs permission
							if (action.equals("command")) {
								Bukkit.getPlayer(event.getWhoClicked().getUniqueId())
										.chat(returnedConfig.getString(event.getSlot() + ".command"));
							}

							// Opens buy confirm menu
							if (action.equals("buyconfirm")) {
								if (returnedConfig.getString(event.getSlot() + ".buy").equalsIgnoreCase("item")) {
									String itemName = returnedConfig.getString(event.getSlot() + ".itemName");

									// Null does not work for translateAlternativeChatCodes
									if (itemName != null) {
										itemName = ChatColor.translateAlternateColorCodes('&', itemName);
									}

									Double discount = returnedConfig.getDouble(event.getSlot() + ".discount");
									
									Bukkit.broadcastMessage(discount.toString());

									if (discount == 0.0) {
										discount = 1.0;
									}

									// Opens buy confirm menu
									AbyssSkyblock.plugin.openInventoryFromConfig((Player) event.getWhoClicked(),
											returnedConfig.getString(event.getSlot() + ".menu"),
											new ItemStack(Material.matchMaterial(
													returnedConfig.getString(event.getSlot() + ".buyitem")), 1),
											returnedConfig.getInt(event.getSlot() + ".maxBuyAmount"),
											returnedConfig.getStringList(event.getSlot() + ".lore"), itemName,
											discount);
								}
							}

							// Final Buy
							if (action.equals("buy")) {
								Material clicked = event.getInventory().getItem(event.getInventory().getSize() / 2)
										.getType();
								ItemMeta meta = event.getInventory().getItem(event.getInventory().getSize() / 2)
										.getItemMeta();

								// Null means clicking nothing
								if (clicked != null) {

									// -1 means there is no free slot
									if (event.getWhoClicked().getInventory().firstEmpty() != -1) {
										// Returns true if player has enough money and has withdrawn the money
										if (eco.subtractAmount(eco.getBuyPrice(clicked) * amount,
												(Player) event.getWhoClicked())) {

											event.getWhoClicked().sendMessage("You have bought " + amount + " "
													+ clicked.toString().toLowerCase() + ChatColor.RESET + " for "
													+ Math.round(eco.getBuyPrice(clicked) * amount * 100) * 0.01
													+ " Minecoins.");

											// Changes market price
											eco.buyItem(clicked, amount);

											ItemStack give = new ItemStack(clicked, amount);
											give.setItemMeta(meta);

											// So the items will stack
											removeSellPricesMeta(event.getWhoClicked().getInventory(),
													(Player) event.getWhoClicked());

											event.getWhoClicked().getInventory().addItem(give);

											// TODO: Make this toggable with a config
											// Adds sell prices to the lore
											addSellPricesMeta(event.getWhoClicked().getInventory(),
													(Player) event.getWhoClicked());

											// Update prices on menu by reopening this menu
											updatePrices(event.getInventory(), (Player) event.getWhoClicked());

										} else {
											event.getWhoClicked().sendMessage(ChatColor.RED + "You need more money");
										}
									} else {
										event.getWhoClicked().sendMessage(ChatColor.RED + "Your inventory is full");
									}
								}
							}

							// Final Sell
							if (action.equals("sell")) {
								Material clicked = event.getInventory().getItem(event.getInventory().getSize() / 2)
										.getType();

								if (clicked != null) {

									// The one is needed to specify a - this comment is incomplete?
									if (event.getWhoClicked().getInventory().contains(clicked, amount)) {
										// Must remove sell item meta for removeitem to work
										removeSellPricesMeta(event.getWhoClicked().getInventory(),
												(Player) event.getWhoClicked());

										// Removes the item in the inventory..
										event.getWhoClicked().getInventory().removeItem(new ItemStack(clicked, amount));

										eco.addAmount((eco.getSellPrice(clicked) * amount),
												(Player) event.getWhoClicked());

										event.getWhoClicked()
												.sendMessage("You have sold " + amount + " "
														+ clicked.toString().toLowerCase() + ChatColor.RESET + " for "
														+ Math.round(eco.getSellPrice(clicked) * amount * 100) * 0.01
														+ " Minecoins.");

										eco.sellItem(clicked, amount);

										addSellPricesMeta(event.getWhoClicked().getInventory(),
												(Player) event.getWhoClicked());

										// Updates sell prices
										updatePrices(event.getInventory(), (Player) event.getWhoClicked());
									}
									// For next time
									addSellPricesMeta(event.getWhoClicked().getInventory(),
											(Player) event.getWhoClicked());
								}
							}
						}
					}
				}
			}
			// So players can't take the items
			event.setCancelled(true);
		}
		// Or take the nether star
		if (event.getCurrentItem() != null && event.getCurrentItem().equals(getMenuStar())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		// Delay is needed for inventory close to process
		Bukkit.getScheduler().scheduleSyncDelayedTask(AbyssSkyblock.plugin, new Runnable() {
			public void run() {
				if (event.getPlayer().getOpenInventory().getTopInventory().toString()
						.contains("CraftInventoryCrafting")) {
					Player player = (Player) event.getPlayer();
					removeSellPricesMeta(event.getPlayer().getInventory(), player);
				}
			}
		}, 1);
	}

	// No taking the nether star
	@EventHandler
	public void onPlayerDropItemEvent(PlayerDropItemEvent event) {
		if (event.getItemDrop().getItemStack().equals(getMenuStar())) {
			event.setCancelled(true);
		}
	}

	// No crafting the nether star
	@EventHandler
	public void onCraft(PrepareItemCraftEvent event) {
		ItemStack air = new ItemStack(Material.AIR);

		if (event.getInventory().contains(getMenuStar())) {
			event.getInventory().setResult(air);
		}
	}

	// You cannot escape the nether star
	@EventHandler
	public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
		if (event.getPlayer().getInventory().contains(getMenuStar()) == false) {
			event.getPlayer().getInventory().setItem(8, getMenuStar());
		}
	}

	// No dropping the nether star on death
	@EventHandler
	public void onPlayerDeathEvent(PlayerDeathEvent event) {
		event.getDrops().remove(getMenuStar());

	}

	// Gives a player a nether star on join to open the menu
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (event.getPlayer().getInventory().contains(getMenuStar()) == false) {
			event.getPlayer().getInventory().setItem(8, getMenuStar());
		}

		// Gives player an island automatically if first join
		// if (!event.getPlayer().hasPlayedBefore()) {
		// event.getPlayer().chat("/island create default");
		// }
	}

	// For spawning islands by right clicking with a spawn egg
	@EventHandler(priority = EventPriority.HIGHEST)
	public void rightClickEvent(PlayerInteractEvent event) {

		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {

			// WJASWOIFDUHNASIUFHASIFLOUAHSWFIOUKJASHNFIULASHF
			try {
				Player player = (Player) event.getPlayer();
				// Both main hand and off hand are here
				// TODO: Find a way to not repeat this code
				// After awhile I couldn't think of a way
				if (event.getHand().equals(EquipmentSlot.HAND)
						&& player.getInventory().getItemInMainHand().getType() != Material.AIR
						&& player.getInventory().getItemInMainHand().getItemMeta().hasLore()) {
					for (String lore : player.getInventory().getItemInMainHand().getItemMeta().getLore()) {
						if (lore.contains("Island: ")) {
							// Only the island owner can paste islands
							if (bento.getIslands().getIslandAt(event.getClickedBlock().getLocation()).get().getOwner()
									.equals(event.getPlayer().getUniqueId())) {

								int x = 0;
								int z = 0;

								int overwrittenBlocks = 0;

								String biome = null;
								int biomeRange = 0;

								// Second iteration
								// Will not fail since I am the only user of my code
								// If it does fail, then everything will break
								for (String lore2 : player.getInventory().getItemInMainHand().getItemMeta().getLore()) {
									if (lore2.contains("Size: ")) {
										x = Integer.parseInt(
												lore2.substring(lore2.lastIndexOf(": ") + 2, lore2.lastIndexOf(" x")));
										z = Integer.parseInt(lore2.substring(lore2.lastIndexOf("x ") + 2));
									}

									if (lore2.contains("Biome: ")) {
										biome = lore2.substring(lore2.lastIndexOf(": ") + 2);
									}

									if (lore2.contains("Biome Range: ")) {
										biomeRange = Integer.parseInt(lore2.substring(lore2.lastIndexOf(": ") + 2));
									}
								}

								x = x / 2;
								z = z / 2;

								// x2, y2, and z2 are iterators. x and z are lengths
								for (int x2 = event.getClickedBlock().getX() - x; x2 < event.getClickedBlock().getX()
										+ x; x2++) {
									for (int z2 = event.getClickedBlock().getZ() - z; z2 < event.getClickedBlock()
											.getZ() + z; z2++) {
										for (int y2 = event.getClickedBlock().getY() - 8; y2 < event.getClickedBlock()
												.getY() + 8; y2++) {
											Block overwriteBlock = event.getClickedBlock().getWorld().getBlockAt(x2, y2,
													z2);
											if (overwriteBlock.getType().equals(Material.AIR) == false) {
												// Non-air block, adding to the safety counter
												overwrittenBlocks++;
												;
												// Cancels the event
												if (overwriteBlock.getType().equals(Material.CHEST)) {
													// Random value that won't overflow but will also stop the paste
													// Chest is always on default island
													overwrittenBlocks = 10000;
												}

												if (overwriteBlock.getType().equals(Material.GRASS_BLOCK)) {
													// Players will not waste grass blocks in skyblock
													overwrittenBlocks += 10;
												}
											}
										}
									}
								}

								// If more than 0.30% of blocks are not air, cancel the event. 0.11% is bridging
								// to middle for 24x26, 0.06 for 45x58. Doubled for less safety
								if (overwrittenBlocks > (x * z * 17) * 0.01) {
									player.sendMessage(
											ChatColor.RED + "This area has too many blocks to place this island");
									player.sendMessage(ChatColor.RED + "" + overwrittenBlocks
											+ " blocks are in the way of the island.");
									player.sendMessage(ChatColor.RED + "" + ((x * z * 17) * 0.01)
											+ " max blocks in the way (1% total size of island)");
									player.sendMessage(
											ChatColor.RED + "Please hover over the spawnegg to view the space needed");
									player.sendMessage(ChatColor.RED
											+ "A chest, and a large amount of grass, will cause this check to immediately fail");
									player.sendMessage(
											ChatColor.RED + "Placing with your offhand will bypass this check");
									event.setCancelled(true);
								} else {
									// TODO: Fix this code
									lore = lore.substring(lore.lastIndexOf(" ") + 1);
									WorldeditManager manager = new WorldeditManager();
									manager.pasteSchematic(lore, event.getClickedBlock().getLocation());
									// If item both in main hand and off hand, main hand used to click
									player.getInventory().getItemInMainHand()
											.setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);

									// Do biome stuff here
									if (biome != null & biomeRange != 0) {
										Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bsbadmin biomes set "
												+ event.getPlayer().getName() + " " + biome + " RANGE " + biomeRange);
									}
									event.setCancelled(true);
								}
							} else {
								event.getPlayer()
										.sendMessage(ChatColor.RED + "Only the island owner can place islands");
								event.setCancelled(true);
							}
						}
					}
				} else if (event.getHand().equals(EquipmentSlot.OFF_HAND)
						&& player.getInventory().getItemInOffHand().getType() != Material.AIR
						&& player.getInventory().getItemInOffHand().getItemMeta().hasLore()) {

					String biome = null;
					int biomeRange = 0;

					for (String lore : player.getInventory().getItemInOffHand().getItemMeta().getLore()) {
						if (lore.contains("Island: ")) {
							// Only the island owner can paste islands
							if (bento.getIslands().getIslandAt(event.getClickedBlock().getLocation()).get().getOwner()
									.equals(event.getPlayer().getUniqueId())) {
								// TODO: Fix this code
								lore = lore.substring(lore.lastIndexOf(" ") + 1);
								WorldeditManager manager = new WorldeditManager();
								manager.pasteSchematic(lore, event.getClickedBlock().getLocation());

								for (String lore2 : player.getInventory().getItemInOffHand().getItemMeta().getLore()) {
									if (lore2.contains("Biome: ")) {
										biome = lore2.substring(lore2.lastIndexOf(": ") + 2);
									}

									if (lore2.contains("Biome Range: ")) {
										biomeRange = Integer.parseInt(lore2.substring(lore2.lastIndexOf(": ") + 2));
									}
								}

								// Do biome stuff here
								if (biome != null & biomeRange != 0) {
									Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bsbadmin biomes set "
											+ event.getPlayer().getName() + " " + biome + " RANGE " + biomeRange);
								}

								// If item both in main hand and off hand, main hand used to click
								player.getInventory().getItemInOffHand()
										.setAmount(player.getInventory().getItemInOffHand().getAmount() - 1);

								event.setCancelled(true);
							} else {
								event.getPlayer()
										.sendMessage(ChatColor.RED + "Only the island owner can place islands");
								event.setCancelled(true);
							}
						}
					}
				}
			} catch (Exception e) {
				event.getPlayer().sendMessage(ChatColor.RED + "Only the island owner can place islands.");
				event.setCancelled(true);
			}
		}
	}

	// Explains itself
	public void addSellPricesMeta(Inventory playerInventory, Player player) {

		// This for loop displays prices for items in the player's inventory
		removeSellPricesMeta(playerInventory, player);

		for (ItemStack i : player.getOpenInventory().getBottomInventory().getContents()) {
			try {
				double price = eco.getSellPrice(i.getType()) * i.getAmount();
				if (price != 0) {

					// Checks for special item meta or names
					if (i.getItemMeta().hasLore() || i.getItemMeta().hasDisplayName()) {
						continue;
					}
					ItemMeta meta = Bukkit.getItemFactory().getItemMeta(i.getType());

					List<String> lore = new ArrayList<String>();

					lore.add(ChatColor.GREEN + "Sell Price");
					lore.add(ChatColor.WHITE + "$" + price);
					lore.add("");
					lore.add(ChatColor.AQUA + "Click to sell");
					meta.setLore(lore);
					i.setItemMeta(meta);
				}
				// }
			} catch (Exception nullpointerexeption) {
				// do nothing
			}
		}
		player.getInventory().setContents(player.getInventory().getContents());
	}

	// Explains itself
	public void removeSellPricesMeta(Inventory playerInventory, Player player) {
		for (ItemStack i : playerInventory.getContents()) {
			try {
				// Checks for special item meta or names
				if (i.getItemMeta().getLore().get(0).contains("Sell Price")) {
					ItemMeta meta = Bukkit.getItemFactory().getItemMeta(i.getType());
					List<String> lore = new ArrayList<String>();
					meta.setLore(lore);
					i.setItemMeta(meta);
				}
			} catch (Exception nullpointerexeption) {
				// do nothing
			}
		}
		player.getInventory().setContents(player.getInventory().getContents());
	}

	public void updatePrices(Inventory updateInventory, Player player) {
		for (int i = 0; i < updateInventory.getSize(); i++) {

			ItemStack j = updateInventory.getItem(i);

			if (j != null) {

				if (j.getItemMeta().getDisplayName().contains("Buy")) {
					double price = eco.getBuyPrice(j.getType()) * j.getAmount();
					if (price != 0) {

						List<String> lore = new ArrayList<String>();

						lore.add(ChatColor.WHITE + "Price " + price);

						j.getItemMeta().setLore(lore);

						updateInventory.setItem(i, j);
						player.openInventory(updateInventory);
					}
				}
				if (j.getItemMeta().getDisplayName().contains("Sell")) {
					double price = eco.getSellPrice(j.getType()) * j.getAmount();
					if (price != 0) {

						List<String> lore = new ArrayList<String>();

						lore.add(ChatColor.WHITE + "Amount " + price);

						j.getItemMeta().setLore(lore);

						updateInventory.setItem(i, j);
						player.openInventory(updateInventory);
					}
				}
			}

		}
	}
}
