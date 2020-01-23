package define.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

public class AbyssSkyblock extends JavaPlugin {

	private File customConfigFile;
	private FileConfiguration menuConfig;

	private FileConfiguration userConfig;

	// ArrayList<FileConfiguration> configList = new ArrayList<FileConfiguration>();
	HashMap<String, FileConfiguration> configList = new HashMap<>();

	public static AbyssSkyblock plugin;
	public static final Logger log = Logger.getLogger("Minecraft");
	public static Economy econ = null;
	public static Permission perms = null;
	public static Chat chat = null;

	ArrayList<String> inventories = new ArrayList<String>();
	File folder = new File(getDataFolder() + File.separator + "gui");
	
	EcoManager eco = new EcoManager();

	@Override
	public void onEnable() {

		if (!folder.exists()) {
			folder.mkdirs();
		}
		createCustomConfig();

		plugin = this;
		getServer().getPluginManager().registerEvents(new AbyssListener(), this);

		if (!setupEconomy()) {
			log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		setupPermissions();
		setupChat();
		
		// This code works fine!
		for (File file : folder.listFiles()) {
			userConfig = new YamlConfiguration();

			try {
				userConfig.load(new File(folder, file.getName()));
			} catch (IOException | InvalidConfigurationException e) {
				e.printStackTrace();
			}

			configList.put(file.getName(), userConfig);
			inventories.add(userConfig.getString("name"));
		}
		
		// Delay is needed for inventory close to process
		Bukkit.getScheduler().scheduleSyncRepeatingTask(AbyssSkyblock.plugin, new Runnable() {
			public void run() {
				Bukkit.getScheduler().runTaskLaterAsynchronously(AbyssSkyblock.plugin, new Runnable() {
					public void run() {
						try {
							FileConfiguration priceConfig = new YamlConfiguration();
							
							priceConfig.load(AbyssSkyblock.plugin.getDataFolder() + File.separator + "config" + File.separator
									+ "worthValues.yml");
							
							for (String key : priceConfig.getKeys(false)) {
								if (priceConfig.getDouble(key + ".marketcap") != 0) {
									Double currentPrice = priceConfig.getDouble(key + ".totalworth")
											/ priceConfig.getDouble(key + ".marketcap");
									Double targetPrice = priceConfig.getDouble(key + ".targetprice");
									Double totalWorth = priceConfig.getDouble(key + ".totalworth");
									Double marketCap = priceConfig.getDouble(key + ".marketcap");
									
									if (currentPrice * 1.05 < targetPrice) {
										priceConfig.set(key + ".totalworth", totalWorth + ((targetPrice - currentPrice) * 0.05 * marketCap));
									}
									
									if ((currentPrice * 0.98) > targetPrice) {
										priceConfig.set(key + ".totalworth", totalWorth - ((currentPrice - targetPrice) * 0.02 * marketCap));
									}
									
									priceConfig.save(AbyssSkyblock.plugin.getDataFolder() + File.separator + "config" + File.separator
											+ "worthValues.yml");
								}
							}
						} catch (IOException | InvalidConfigurationException e) {
							e.printStackTrace();
						}
					}
				}, 1);
			}
		}, 18000, 36000);
	}

	// For Buying Stuff
	public void openInventoryFromConfig(Player player, String name, ItemStack buyItem, int maxBuyAmount,
			List<String> lore, String itemName, double discount) {

		FileConfiguration openedConfig = configList.get(name + ".yml");

		Inventory menuInventory = Bukkit.createInventory(null, openedConfig.getInt("slots"),
				openedConfig.getString("name"));

		for (int i = 0; i < openedConfig.getInt("slots"); i++) {
			if (openedConfig.getString(i + ".item") != null) {
				try {
					Integer amount = openedConfig.getInt(i + ".amount");

					ItemStack itemStackIterator = new ItemStack(
							Material.matchMaterial(openedConfig.getString(i + ".item")), amount);
					ItemMeta itemMetaIterator = (ItemMeta) itemStackIterator.getItemMeta();
					String line = openedConfig.getString(i + ".name");
					
					line = ChatColor.translateAlternateColorCodes('&', line);

					List<String> lore2 = new ArrayList<String>();

					ItemMeta itemMeta = (ItemMeta) buyItem.getItemMeta();
					
					if (openedConfig.getString(i + ".action").equals("buy")) {
						lore2.add(ChatColor.WHITE + "Price " + eco.getBuyPrice(buyItem.getType()) * amount * discount);
					}
					
					if (openedConfig.getString(i + ".action").equals("sell")) {
						lore2.add(ChatColor.WHITE + "Amount " + eco.getSellPrice(buyItem.getType()) * amount * discount);
					}
					
					// Wraps up all data
					itemMeta.setLore(lore2);
					itemMetaIterator.setLore(lore2);
					itemMetaIterator.setDisplayName(line);
					itemStackIterator.setItemMeta(itemMetaIterator);
					
					if (itemStackIterator.getAmount() <= maxBuyAmount) {
						menuInventory.setItem(i, itemStackIterator);
					}
					
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (lore != null) {
				ItemMeta itemMeta = (ItemMeta) buyItem.getItemMeta();
				for (int x = 0; x < lore.size(); x++) {
					lore.set(x, ChatColor.RESET + "" + ChatColor.WHITE + lore.get(x));
				}
				itemMeta.setLore(lore);
				if (itemName != null) {
					itemMeta.setDisplayName(itemName);
				}
				buyItem.setItemMeta(itemMeta);
			}
			menuInventory.setItem(openedConfig.getInt("slots") / 2, buyItem);
		}
		player.openInventory(menuInventory);
	}

	public void openInventoryFromConfig(Player player, String name) {

		FileConfiguration openedConfig = configList.get(name + ".yml");

		Inventory menuInventory = Bukkit.createInventory(null, openedConfig.getInt("slots"), openedConfig.getString("name"));

		for (int i = 0; i < openedConfig.getInt("slots"); i++) {
			if (openedConfig.getString(i + ".item") != null) {
				try {
					Integer amount = openedConfig.getInt(i + ".amount");

					ItemStack itemStackIterator = new ItemStack(
							Material.matchMaterial(openedConfig.getString(i + ".item")), amount);
					ItemMeta itemMetaIterator = (ItemMeta) itemStackIterator.getItemMeta();
					String line = openedConfig.getString(i + ".name");
					
					line = ChatColor.translateAlternateColorCodes('&', line);

					itemMetaIterator.setDisplayName(line);
					itemStackIterator.setItemMeta(itemMetaIterator);
					menuInventory.setItem(i, itemStackIterator);
				} catch (Exception e) {
					player.sendMessage("Unknown error loading item: " + i);
					e.printStackTrace();
				}
			}
		}
		player.openInventory(menuInventory);
	}

	public ArrayList<String> getInventories() {
		return inventories;
	}

	public void onDisable() {
		log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
	}

	public FileConfiguration getCustomConfig() {
		return this.menuConfig;
	}

	private void createCustomConfig() {
		// Main menu
		customConfigFile = new File(getDataFolder() + File.separator + "gui", "menu.yml");
		if (!customConfigFile.exists()) {
			customConfigFile.getParentFile().mkdirs();
			saveResource("gui/menu.yml", false);
		}

		menuConfig = new YamlConfiguration();
		try {
			menuConfig.load(customConfigFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Buy homes menu
		customConfigFile = new File(getDataFolder() + File.separator + "gui", "homesMenu.yml");
		if (!customConfigFile.exists()) {
			customConfigFile.getParentFile().mkdirs();
			saveResource("gui/homesMenu.yml", false);
		}

		// Comfirm menu
		customConfigFile = new File(getDataFolder() + File.separator + "gui", "confirmMenu.yml");
		if (!customConfigFile.exists()) {
			customConfigFile.getParentFile().mkdirs();
			saveResource("gui/confirmMenu.yml", false);
		}

		// Item prices
		customConfigFile = new File(getDataFolder(), "confirmMenu.yml");
		if (!customConfigFile.exists()) {
			customConfigFile.getParentFile().mkdirs();
			saveResource("config/worthValues.yml", false);
		}
	}

	// Code found here https://github.com/MilkBowl/VaultAPI
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	private boolean setupChat() {
		RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
		chat = rsp.getProvider();
		return chat != null;
	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
		return perms != null;
	}
	// End copy pasted code from the Vault API

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// Opens the Abyss Main menu, eventually
		if (command.getName().equalsIgnoreCase("menu")) {
			if (sender instanceof Player) {
				openInventoryFromConfig((Player) sender, "menu");
			}
			return true;
		}
		return false;
	}
}
