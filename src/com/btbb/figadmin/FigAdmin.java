/*
Copyright (C) 2011, 2016 Serge Humphrey <sergehumphrey@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.btbb.figadmin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import com.evilmidget38.UUIDFetcher;

/**
 * Admin plugin for Bukkit.
 * 
 * @author yottabyte
 * @author Serge Humphrey
 */

public class FigAdmin extends JavaPlugin {

	public static final Logger log = Logger.getLogger("Minecraft");

	Database db;
	String maindir = "plugins/FigAdmin/";
	/**
	 * Cached list only to be used by {@link FigAdminPlayerListener}
	 */
	ArrayList<EditBan> bannedPlayers;
	private final FigAdminPlayerListener playerListener = new FigAdminPlayerListener(this);

	public FileConfiguration config;
	public boolean autoComplete;
	protected EditCommand editor;
	private FigCmdHandler cmdHandler;

	public void onDisable() {
		log.log(Level.INFO, "FigAdmin disabled.");
	}

	/**
	 * Create a default configuration file from the .jar.
	 * 
	 * @param name
	 */
	public void setupConfig() {
		this.config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();

	}

	public static boolean validName(String name) {
		return name.length() > 2 && name.length() < 17 && !name.matches("(?i).*[^a-z0-9_].*");
	}

	public void onEnable() {
		new File(maindir).mkdir();
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
		}
		setupConfig();

		boolean useMysql = getConfig().getBoolean("mysql", false);
		if (useMysql) {
			try {
				db = new MySQLDatabase(this);
			} catch (Exception e) {
				log.log(Level.CONFIG, "Ohhh Shit! Can't start MySQL Database!");
				System.out.println("FigAdmin [Error]: Can't initialize databse.");
				return;
			}
		} else {
			db = new FlatFileDatabase();
		}
		boolean dbinit = false;
		if (!(dbinit = db.initialize(this))) {
			if (useMysql) {
				log.log(Level.WARNING, "[FigAdmin] Can't set up MySQL, trying flatfile");
				db = new FlatFileDatabase();
				if (!(dbinit = db.initialize(this))) {
					log.log(Level.WARNING, "[FigAdmin] Flatfile doesn't work either, disabling FigAdmin");
				}
			}
		}
		if (!dbinit) {
			log.log(Level.WARNING, "[FigAdmin] Can't set up flatfile, disabling FigAdmin");
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}

		this.autoComplete = getConfig().getBoolean("auto-complete", true);

		// Register our events
		getServer().getPluginManager().registerEvents(playerListener, this);

		editor = new EditCommand(this);
		getCommand("editban").setExecutor(editor);

		cmdHandler = new FigCmdHandler(this);
		PluginDescriptionFile pdfFile = this.getDescription();
		this.updateCache();
		log.log(Level.INFO, pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
	}

	/**
	 * Combines array of strings into a single string
	 * 
	 * @param startIndex
	 * @param string
	 * @param seperator
	 * @return
	 */
	public String combineSplit(int startIndex, String[] string, String seperator) {
		StringBuilder builder = new StringBuilder();
		for (int i = startIndex; i < string.length; i++) {
			builder.append(string[i]);
			builder.append(seperator);
		}
		builder.deleteCharAt(builder.length() - seperator.length());
		return builder.toString();
	}

	public long parseTimeSpec(String time, String unit) {
		long sec;
		try {
			sec = Integer.parseInt(time) * 60;
		} catch (NumberFormatException ex) {
			return 0;
		}
		if (unit.startsWith("hour"))
			sec *= 60;
		else if (unit.startsWith("day"))
			sec *= (60 * 24);
		else if (unit.startsWith("week"))
			sec *= (7 * 60 * 24);
		else if (unit.startsWith("month"))
			sec *= (30 * 60 * 24);
		else if (unit.startsWith("min"))
			sec *= 1;
		else if (unit.startsWith("sec"))
			sec /= 60;
		return sec * 1000;
	}

	public String expandName(String name) {
		if (!autoComplete)
			return name;
		if (name.equals("*"))
			return name;
		int m = 0;
		String Result = "";
		Player[] players = getServer().getOnlinePlayers().toArray(new Player[getServer().getOnlinePlayers().size()]);
		for (int n = 0; n < players.length; n++) {
			String str = players[n].getName();
			if (str.matches("(?i).*" + name + ".*")) {
				m++;
				Result = str;
				if (m == 2) {
					return null;
				}
			}
			if (str.equalsIgnoreCase(name))
				return str;
		}
		if (m == 1)
			return Result;
		if (m > 1) {
			return null;
		}
		if (m < 1) {
			return name;
		}
		return name;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		getServer().getScheduler().runTask(this, new Runnable() {
			@Override
			public void run() {
				if (!cmdHandler.onCommand(sender, command, commandLabel, args) && command.getUsage().length() > 0) {
					for (String line : command.getUsage().replace("<command>", commandLabel).split("\n")) {
						sender.sendMessage(formatMessage("&c" + line));
					}
				}
			}
		});
		return true;
	}

	protected EditBan isBanned(String name) {
		for (EditBan eb : db.getBannedPlayers())
			if (eb.name.equalsIgnoreCase(name) && eb.type != EditBan.BanType.WARN)
				return eb;
		return null;
	}

	protected EditBan isBanned(UUID id) {
		for (EditBan eb : db.getBannedPlayers())
			if (eb.uuid.equals(id) && eb.type != EditBan.BanType.WARN)
				return eb;
		return null;
	}

	public static boolean hasPermission(CommandSender sender, String perm) {
		if (sender instanceof Player) {
			Player p = (Player) sender;
			if (p.isOp()) {
				return true;
			}
			return sender.hasPermission(perm);
		} else {
			// must be console
			return true;
		}

	}

	public static OfflinePlayer getPlayer(String name) {
		// lookup order: online players, offline players, lookup from mojang
		for (Player p : Bukkit.getServer().getOnlinePlayers())
			if (name.equalsIgnoreCase(p.getName()))
				return p;
		for (OfflinePlayer p : Bukkit.getServer().getOfflinePlayers())
			if (name.equalsIgnoreCase(p.getName()))
				return p;
		return null;
	}

	public static UUID getUUIDofPlayer(String name) {
		// lookup order: online players, offline players, lookup from mojang
		for (Player p : Bukkit.getServer().getOnlinePlayers())
			if (name.equalsIgnoreCase(p.getName()))
				return p.getUniqueId();
		for (OfflinePlayer p : Bukkit.getServer().getOfflinePlayers())
			if (name.equalsIgnoreCase(p.getName()))
				return p.getUniqueId();
		// do a web lookup
		return getUUIDfromMojang(name);
	}

	public static UUID getUUIDfromMojang(String name) {
		try {
			return UUIDFetcher.getUUIDOf(name);
		} catch (Exception e) {
			log.log(Level.WARNING, "Unable to lookup UUID for " + name, e);
			e.printStackTrace();
		}
		return null;
	}

	public static String formatMessage(String str) {
		String funnyChar = new Character((char) 167).toString();
		str = str.replaceAll("&", funnyChar);
		return str;
	}

	public void updateCache() {
		getServer().getScheduler().runTask(this, new Runnable() {
			public void run() {
				bannedPlayers = db.getBannedPlayers();
			}
		});
	}
}
