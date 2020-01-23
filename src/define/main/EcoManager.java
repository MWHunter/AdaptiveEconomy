package define.main;

import java.io.File;
import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class EcoManager {
	public boolean hasAmount(double amount, Player player) {
		return (AbyssSkyblock.econ.getBalance(player) > amount);
	}

	public boolean subtractAmount(double amount, Player player) {
		if (hasAmount(amount, player)) {
			AbyssSkyblock.econ.withdrawPlayer(player, amount);
			return true;
		}
		return false;
	}

	public void addAmount(double amount, Player player) {
		AbyssSkyblock.econ.depositPlayer(player, amount);
	}

	public double getBuyPrice(Material item) {
		FileConfiguration priceConfig = new YamlConfiguration();
		double buyPrice;
		
		try {
			priceConfig.load(AbyssSkyblock.plugin.getDataFolder() + File.separator + "config" + File.separator
					+ "worthValues.yml");
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
		
		if (priceConfig.getDouble(item.toString() + ".marketcap") != 0) {
			buyPrice = (priceConfig.getDouble(item.toString() + ".totalworth")
					/ priceConfig.getDouble(item.toString() + ".marketcap"));
		} else {
			buyPrice = (priceConfig.getDouble(item.toString() + ".price"));
		}
		
		buyPrice = Math.floor(buyPrice * 100) * 0.01;
		
		return (buyPrice);
	}

	public double buyItem(Material item, int amount) {
		FileConfiguration priceConfig = new YamlConfiguration();
		try {
			priceConfig.load(AbyssSkyblock.plugin.getDataFolder() + File.separator + "config" + File.separator
					+ "worthValues.yml");
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
		
		double totalWorth = priceConfig.getDouble(item.toString() + ".totalworth");
		double buyPrice = getBuyPrice(item) * amount;

		// Adds value to market, if config is using variable prices for the item
		if (totalWorth != 0) {
			priceConfig.set(item.toString() + ".totalworth", totalWorth + buyPrice);
		}

		try {
			priceConfig.save(AbyssSkyblock.plugin.getDataFolder() + File.separator + "config" + File.separator
					+ "worthValues.yml");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return (buyPrice);
	}
	
	public void sellItem(Material item, int amount) {
		FileConfiguration priceConfig = new YamlConfiguration();
		
		try {
			priceConfig.load(AbyssSkyblock.plugin.getDataFolder() + File.separator + "config" + File.separator
					+ "worthValues.yml");
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
		
		double totalWorth = priceConfig.getDouble(item.toString() + ".totalworth");
		
		priceConfig.set(item.toString() + ".totalworth", totalWorth - (getSellPrice(item) * amount));
		
		try {
			priceConfig.save(AbyssSkyblock.plugin.getDataFolder() + File.separator + "config" + File.separator
					+ "worthValues.yml");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public double getSellPrice(Material item) {
		FileConfiguration priceConfig = new YamlConfiguration();
		double price;
		try {
			priceConfig.load(AbyssSkyblock.plugin.getDataFolder() + File.separator + "config" + File.separator
					+ "worthValues.yml");
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		if (priceConfig.getDouble(item.toString() + ".marketcap") != 0) {
			price = priceConfig.getDouble(item.toString() + ".totalworth")
					/ priceConfig.getDouble(item.toString() + ".marketcap")
					* priceConfig.getDouble(item.toString() + ".sellprice");
		} else {
			price = (priceConfig.getDouble(item.toString() + ".price")
					* priceConfig.getDouble(item.toString() + ".sellprice"));
		}
		
		 price = Math.floor(price * 100) * 0.01;

		return (price);
	}
}
