package define.main;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;

import java.io.File;
import java.io.FileInputStream;

import org.bukkit.Location;

public class WorldeditManager {
	// WorldEditPlugin WorldEdit = (WorldEditPlugin)
	// Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");

	public void pasteSchematic(String filename, Location location) {

		// FileInputStream schematic = new
		// FileInputStream(AbyssSkyblock.plugin.getDataFolder() + File.separator +
		// "schematics" + File.separator + filename + ".schematic");

		
		try {
			File schematic = new File(AbyssSkyblock.plugin.getDataFolder() + File.separator + "schematics" + File.separator
					+ filename + ".schem");
			ClipboardFormat format = ClipboardFormats.findByFile(schematic);
			ClipboardReader reader = format.getReader(new FileInputStream(schematic));
			Clipboard clipboard = reader.read();
			EditSession es = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(location.getWorld()), -1);
			Operation operation = new ClipboardHolder(clipboard).createPaste(es)
					.to(BlockVector3.at(location.getX(), location.getY(), location.getZ())).ignoreAirBlocks(true)
					.build();
			Operations.complete(operation);
			es.flushSession();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
