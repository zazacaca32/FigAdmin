/*
Copyright (C) 2016 Serge Humphrey <sergehumphrey@gmail.com>

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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class FigCmdHandler {
	FigAdmin plugin;

	public FigCmdHandler(FigAdmin plugin) {
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		String commandName = command.getName().toLowerCase();
		String[] trimmedArgs = args;

		// sender.sendMessage(ChatColor.GREEN + trimmedArgs[0]);
		if (commandName.equals("reloadfig")) {
			return reloadFig(sender);
		}
		if (commandName.equals("unban")) {
			return unBanPlayer(sender, trimmedArgs);
		}
		if (commandName.equals("ban")) {
			return banPlayer(sender, trimmedArgs);
		}
		if (commandName.equals("warn")) {
			return warnPlayer(sender, trimmedArgs);
		}
		if (commandName.equals("kick")) {
			return kickPlayer(sender, trimmedArgs);
		}
		if (commandName.equals("tempban")) {
			return tempbanPlayer(sender, trimmedArgs);
		}
		if (commandName.equals("checkban")) {
			return checkBan(sender, trimmedArgs);
		}
		if (commandName.equals("ipban")) {
			return ipBan(sender, trimmedArgs);
		}
		if (commandName.equals("exportbans")) {
			return exportBans(sender);
		}
		if (commandName.equals("unbanip")) {
			return unbanIP(sender, trimmedArgs);
		}
		if (commandName.equals("figadmin")) {
			return figAdmin(sender);
		}

		if (commandName.equals("clearwarnings") || commandName.equals("clearplayer")) {
			return clearWarnings(sender, trimmedArgs);
		}

		return false;
	}

	private boolean unBanPlayer(CommandSender sender, String[] args) {
		if (!hasPermission(sender, "figadmin.unban")) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
			return true;
		}
		Player player = null;
		String kicker = "server";
		if (sender instanceof Player) {
			player = (Player) sender;
			kicker = player.getName();
		}
		// Has enough arguments?
		if (args.length < 1)
			return false;

		String p = args[0];
		if (!validName(p)) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.badPlayerName", "bad player name")));
			return false;
		}
		boolean found = false;
		for (int i = 0; i < plugin.bannedPlayers.size(); i++) {
			EditBan e = plugin.bannedPlayers.get(i);
			if (e.name.equalsIgnoreCase(p)) {
				// If the current ban is selected in editor, get rid of it
				if (plugin.editor.ban != null && plugin.editor.ban.equals(e)) {
					plugin.editor.ban = null;
				}
				found = true;
				plugin.bannedPlayers.remove(i);
				// Don't break, cycle through all banned players in case player
				// is banned twice
				plugin.db.removeFromBanlist(e.name);
			}
		}

		// unban the player from minecraft vanilla if possible
		OfflinePlayer op = FigAdmin.getPlayer(p);
		if (op != null && op.isBanned()) {
			plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).pardon(op.getName());
			found = true;
		}
		if (found) {
			// Log in console
			FigAdmin.log.log(Level.INFO, "[FigAdmin] " + kicker + " unbanned player " + p + ".");

			String globalMsg = getConfig().getString("messages.unbanMsgGlobal", "player unban global %victim%");
			globalMsg = globalMsg.replaceAll("%victim%", p).replaceAll("%player%", kicker);

			// send a message to everyone!
			plugin.getServer().broadcastMessage(formatMessage(globalMsg));
		} else {
			// Unban failed
			String kickerMsg = getConfig().getString("messages.unbanMsgFailed", "unban failed");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			sender.sendMessage(formatMessage(kickerMsg));
		}
		return true;
	}

	private boolean kickPlayer(CommandSender sender, String[] args) {
		if (!hasPermission(sender, "figadmin.kick")) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
			return true;
		}
		Player player = null;
		String kicker = "server";
		if (sender instanceof Player) {
			player = (Player) sender;
			kicker = player.getName();
		}
		// Has enough arguments?
		if (args.length < 1) {
			return false;
		}
		String p = args[0].toLowerCase();
		// Reason stuff
		String reason;
		boolean broadcast = true;

		if (args.length > 1) {
			reason = plugin.combineSplit(1, args, " ");
		} else {
			if (p.equals("*")) {
				reason = getConfig().getString("messages.kickGlobalDefaultReason", "Global Kick");
			} else {
				reason = getConfig().getString("messages.kickDefaultReason", "Booted from server");
			}
		}
		if (p.equals("*")) {
			if (!hasPermission(sender, "figadmin.kick.all")) {
				sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
				return true;
			}
			String kickerMsg = getConfig().getString("messages.kickAllMsg");
			kickerMsg = kickerMsg.replaceAll("%player%", kicker);
			kickerMsg = kickerMsg.replaceAll("%reason%", reason);
			FigAdmin.log.log(Level.INFO, "[FigAdmin] " + formatMessage(kickerMsg));
			// Kick everyone on server
			Player ps = null;
			if (sender instanceof Player) {
				ps = (Player) sender;
			}
			for (Player pl : plugin.getServer().getOnlinePlayers()) {
				if (ps != null && ps.getName().equalsIgnoreCase(pl.getName())) {
					// don't kick sender

				} else {
					pl.kickPlayer(formatMessage(kickerMsg));
				}
			}
			return true;
		} else if (!validName(p)) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.badPlayerName", "bad player name")));
			return true;
		}
		if (plugin.autoComplete)
			p = plugin.expandName(p);
		OfflinePlayer op = FigAdmin.getPlayer(p);
		Player victim = ((op != null) && (op).isOnline()) ? (Player) op : null;
		if (victim == null) {
			String kickerMsg = getConfig().getString("messages.kickMsgFailed");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			sender.sendMessage(formatMessage(kickerMsg));
			return true;
		}

		// Log in console
		FigAdmin.log.log(Level.INFO, "[FigAdmin] " + kicker + " kicked player " + p + ". Reason: " + reason);

		// Send message to victim
		String kickerMsg = getConfig().getString("messages.kickMsgVictim");
		kickerMsg = kickerMsg.replaceAll("%player%", kicker);
		kickerMsg = kickerMsg.replaceAll("%reason%", reason);
		victim.kickPlayer(formatMessage(kickerMsg));

		if (broadcast) {
			// Send message to all players
			String kickerMsgAll = getConfig().getString("messages.kickMsgBroadcast");
			kickerMsgAll = kickerMsgAll.replaceAll("%player%", kicker);
			kickerMsgAll = kickerMsgAll.replaceAll("%reason%", reason);
			kickerMsgAll = kickerMsgAll.replaceAll("%victim%", p);
			plugin.getServer().broadcastMessage(formatMessage(kickerMsgAll));
		}
		return true;
	}

	private boolean banPlayer(CommandSender sender, String[] args) {
		try {
			if (!hasPermission(sender, "figadmin.ban")) {
				sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
				return true;
			}
			Player player = null;
			String kicker = "server";
			if (sender instanceof Player) {
				player = (Player) sender;
				kicker = player.getName();
			}

			// Has enough arguments?
			if (args.length < 1)
				return false;

			String p = args[0]; // Get the victim's name
			if (!validName(p)) {
				sender.sendMessage(formatMessage(getConfig().getString("messages.badPlayerName", "bad player name")));
				return true;
			}
			if (plugin.autoComplete)
				p = plugin.expandName(p); // If the admin has chosen to do so,
			OfflinePlayer victim = getPlayer(p); // What player is
			UUID id;
			if (victim == null)
				id = FigAdmin.getUUIDfromMojang(p);
			else
				id = victim.getUniqueId();
			if (id == null) {
				String msg = getConfig().getString("messages.noSuchPlayer").replaceAll("%player%", p);
				sender.sendMessage(formatMessage(msg));
				return true;
			}
			String reason = "Ban Hammer has Spoken!";
			boolean broadcast = true;
			if (args.length > 1)
				reason = plugin.combineSplit(1, args, " ");
			if (plugin.isBanned(p) != null) {
				// already banned
				String kickerMsg = getConfig().getString("messages.banMsgFailed");
				kickerMsg = kickerMsg.replaceAll("%victim%", p);
				sender.sendMessage(formatMessage(kickerMsg));
				return true;
			}
			boolean ipBan = getConfig().getBoolean("ip-ban");
			EditBan ban = null;

			Player onlinePlayer = (victim != null && victim instanceof Player) ? (Player) victim : null;
			String ip = null;
			if (onlinePlayer != null) {
				ip = onlinePlayer.getAddress().getAddress().getHostAddress();
			}

			if (ipBan && ip != null) {
				ban = new EditBan(id, p, reason, kicker, ip, EditBan.IPBAN);
			} else {
				ban = new EditBan(id, p, reason, kicker, ip, EditBan.BAN);
			}

			plugin.bannedPlayers.add(ban); // Add name to RAM

			// Add player to database
			plugin.db.addPlayer(ban);

			// Log in console
			FigAdmin.log.log(Level.INFO, "[FigAdmin] " + kicker + " banned player " + p + ".");

			if (onlinePlayer != null) { // If he is online, kick him with a nice
										// message :)
				// Send message to victim
				String kickerMsg = getConfig().getString("messages.banMsgVictim");
				kickerMsg = kickerMsg.replaceAll("%player%", kicker);
				kickerMsg = kickerMsg.replaceAll("%reason%", reason);
				onlinePlayer.kickPlayer(formatMessage(kickerMsg));
			}

			if (victim != null && !victim.hasPlayedBefore()) {
				String msg = getConfig().getString("messages.banOffline").replaceAll("%player%", p);
				sender.sendMessage(formatMessage(msg));

			}

			// Send message to all players
			if (broadcast) {
				String kickerMsgAll = getConfig().getString("messages.banMsgBroadcast");
				kickerMsgAll = kickerMsgAll.replaceAll("%player%", kicker);
				kickerMsgAll = kickerMsgAll.replaceAll("%reason%", reason);
				kickerMsgAll = kickerMsgAll.replaceAll("%victim%", p);
				plugin.getServer().broadcastMessage(formatMessage(kickerMsgAll));
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return true;
	}

	private boolean tempbanPlayer(CommandSender sender, String[] args) {
		if (!hasPermission(sender, "figadmin.tempban")) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
			return true;
		}
		Player player = null;
		String kicker = "server";
		if (sender instanceof Player) {
			player = (Player) sender;
			kicker = player.getName();
		}

		if (args.length < 3) 
			return false;

		String p = args[0]; // Get the victim's name
		if (!validName(p)) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.badPlayerName", "bad player name")));
			return true;
		}
		if (plugin.autoComplete)
			p = plugin.expandName(p);
		OfflinePlayer victim = getPlayer(p);
		if (victim == null) {
			String msg = getConfig().getString("messages.noSuchPlayer").replaceAll("%player%", p);
			sender.sendMessage(formatMessage(msg));
			return true;
		}
		String reason;
		boolean broadcast = true;
		if (args.length > 3)
			reason = plugin.combineSplit(3, args, " ");
		else
			reason = getConfig().getString("banDefaultReason", "Ban hammer has spoken!");

		if (plugin.isBanned(p) != null) {
			// already banned
			String kickerMsg = getConfig().getString("messages.banMsgFailed", "Ban failed");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			sender.sendMessage(formatMessage(kickerMsg));
			return true;
		}
		long tempTime = plugin.parseTimeSpec(args[1], args[2]); // parse the
																// time and
		// do other crap below
		if (tempTime == 0)
			return false;
		tempTime = System.currentTimeMillis() + tempTime;
		EditBan ban = new EditBan(victim.getUniqueId(), p, reason, kicker, tempTime, EditBan.BAN);
		plugin.bannedPlayers.add(ban);
		plugin.db.addPlayer(ban);
		FigAdmin.log.log(Level.INFO, "[FigAdmin] " + kicker + " tempbanned player " + p + ".");

		if (victim != null && victim.isOnline()) { // If he is online, kick him
													// with a nice message
			String kickerMsg = getConfig().getString("messages.tempbanMsgVictim");
			kickerMsg = kickerMsg.replaceAll("%player%", kicker);
			kickerMsg = kickerMsg.replaceAll("%reason%", reason);
			((Player) victim).kickPlayer(formatMessage(kickerMsg));
		}
		if (broadcast) {
			// Send message to all players
			String kickerMsgAll = getConfig().getString("messages.tempbanMsgBroadcast");
			kickerMsgAll = kickerMsgAll.replaceAll("%player%", kicker);
			kickerMsgAll = kickerMsgAll.replaceAll("%reason%", reason);
			kickerMsgAll = kickerMsgAll.replaceAll("%victim%", p);
			plugin.getServer().broadcastMessage(formatMessage(kickerMsgAll));
		}
		return true;
	}

	private boolean checkBan(CommandSender sender, String[] args) {
		if (!hasPermission(sender, "figadmin.checkban")) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
			return true;
		}
		if (args.length == 0) {
			return false;
		}
		String p = args[0];
		if (!validName(p)) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.badPlayerName", "bad player name")));
			return true;
		}
		EditBan e = plugin.isBanned(p);
		if (e != null) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.playerBanned", "player banned").replaceAll("%player%", p)));
			EditCommand.showBanInfo(e, sender);
		} else
			sender.sendMessage(
					formatMessage(getConfig().getString("messages.playerNotBanned", "player not banned").replaceAll("%player%", p)));
		return true;
	}

	private boolean ipBan(CommandSender sender, String[] args) {
		if (!hasPermission(sender, "figadmin.ipban")) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
			return true;
		}
		boolean success = false;
		if (args.length > 0) {
			if (args[0].equals("on") || args[0].equals("true")) {
				getConfig().set("ip-ban", true);
			} else if (args[0].equals("off") || args[0].equals("false")) {
				getConfig().set("ip-ban", false);
			} else {
				return false;
			}
			success = true;
		}
		boolean ipban = getConfig().getBoolean("ip-ban");
		sender.sendMessage(formatMessage(getConfig().getString("messages.ipBan") + " " + ((ipban) ? "on" : "off")));
		return success;
	}

	private boolean warnPlayer(CommandSender sender, String[] args) {
		if (!hasPermission(sender, "figadmin.warn")) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
			return true;
		}
		String kicker = "server";
		if (sender instanceof Player) {
			Player player = (Player) sender;
			kicker = player.getName();
		}

		// Has enough arguments?
		if (args.length < 2)
			return false;

		String p = args[0]; // Get the victim's name
		if (!validName(p)) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.badPlayerName", "bad player name")));
			return true;
		}
		if (plugin.autoComplete)
			p = plugin.expandName(p); // If the admin has chosen to do so,
										// autocomplete
		OfflinePlayer v = getPlayer(args[1]);

		// the victim?
		if (v == null || !(v instanceof Player)) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.playerNotOnline", "not online").replaceAll("%player%", p)));
			return true;
		}
		Player victim = (Player) v;
		// Reason stuff
		String reason;
		boolean broadcast = true;

		if (args.length > 1) {
			reason = plugin.combineSplit(1, args, " ");
		} else {
			// You must specify a reason
			sender.sendMessage(formatMessage(getConfig().getString("messages.warnSpecify")));
			return true;
		}

		// Add player to database
		EditBan b = new EditBan(victim.getUniqueId(), v.getName(), reason, kicker, EditBan.WARN);
		plugin.db.addPlayer(b);

		// Log in console
		FigAdmin.log.log(Level.INFO, "[FigAdmin] " + kicker + " warned player " + p + ".");

		// Send message to all players
		if (broadcast) {
			plugin.getServer()
					.broadcastMessage(formatMessage(getConfig().getString("messages.warnMsgBroadcast", "warning from %player% by %kicker%")
							.replaceAll("%player%", p).replaceAll("%kicker%", kicker)));
			plugin.getServer().broadcastMessage(ChatColor.GRAY + "  " + reason);
		} else {
			victim.sendMessage(
					formatMessage(getConfig().getString("messages.warnMsgVictim", "warning from %player%").replaceAll("%kicker%", kicker)));
			victim.sendMessage(ChatColor.GRAY + "  " + reason);

		}
		// auto ban thing
		int x = getConfig().getInt("auto-ban-on-warnings");
		if (x > 0 && plugin.db.getWarnCount(victim.getUniqueId()) > x) {
			String s = getConfig().getString("auto-ban-time");
			int i = s.indexOf(" ");
			if (i < 1) {
				sender.sendMessage(formatMessage("&cCan't auto-ban; bad time format:&e '&8" + s + "&e'"));
			} else {
				// clear warnings before banning them
				plugin.db.clearWarnings(victim.getUniqueId());
				String time = s.substring(0, i);
				String format = s.substring(i + 1);
				String[] tempargs = new String[] { p, time, format, reason };
				tempbanPlayer(sender, tempargs);
			}
		}

		return true;
	}

	private boolean reloadFig(CommandSender sender) {
		if (!hasPermission(sender, "figadmin.reload")) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
			return true;
		}

		String p = "server";
		if (sender instanceof Player) {
			Player player = (Player) sender;
			p = player.getName();
		}

		plugin.reloadConfig();
		plugin.onEnable();

		FigAdmin.log.log(Level.INFO, "[FigAdmin] " + p + " Reloaded FigAdmin.");
		sender.sendMessage(formatMessage(getConfig().getString("messages.reloadMsg", "reloaded")));
		return true;
	}

	private boolean exportBans(CommandSender sender) {
		if (!hasPermission(sender, "figadmin.export")) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
			return true;
		}

		try {
			BufferedWriter banlist = new BufferedWriter(new FileWriter("banned-players.json", true));
			for (int n = 0; n < plugin.bannedPlayers.size(); n++) {
				banlist.write(plugin.bannedPlayers.get(n).name);
				banlist.newLine();
			}
			banlist.close();
		} catch (IOException e) {
			FigAdmin.log.log(Level.SEVERE, "FigAdmin: Couldn't write to banned-players.json");
		}
		sender.sendMessage(formatMessage(getConfig().getString("messages.exportMsg", "expored")));
		return true;

	}

	private boolean unbanIP(CommandSender sender, String[] args) {
		if (!hasPermission(sender, "figadmin.unbanip")) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
			return true;
		}
		if (args.length < 1) {
			return false;
		}
		boolean success = false;
		String IP = args[0];
		for (int i = 0; i < plugin.bannedPlayers.size(); i++) {
			EditBan b = plugin.bannedPlayers.get(i);
			if (b.ipAddress != null && b.ipAddress.equals(IP)) {
				plugin.db.deleteFullRecord(b.id);
				plugin.bannedPlayers.remove(i);
				sender.sendMessage(formatMessage(getConfig().getString("messages.unbanMsg").replaceAll("%victim%", b.name)));
				success = true;
			}
		}
		if (!success) {
			String failed = getConfig().getString("messages.unbanMsgFailed", "unban failed").replaceAll("%victim%", "IP " + IP);
			sender.sendMessage(formatMessage(failed));
		}
		return true;
	}

	private boolean clearWarnings(CommandSender sender, String[] args) {
		if (!hasPermission(sender, "figadmin.clearwarnings")) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.noPermission")));
			return true;
		}
		if (args.length < 1) {
			return false;
		}
		if (!validName(args[0])) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.badPlayerName", "bad player name")));
			return true;
		}
		OfflinePlayer p = getPlayer(args[0]);
		if (p == null) {
			String msg = getConfig().getString("messages.noSuchPlayer").replaceAll("%player%", args[0]);
			sender.sendMessage(formatMessage(msg));
			return true;
		}
		int x = plugin.db.clearWarnings(p.getUniqueId());
		if (x > 0) {
			sender.sendMessage(formatMessage(getConfig().getString("messages.warnDeleted", "warnings deleted")
					.replaceAll("%player%", p.getName()).replaceAll("%number%", x + "")));

		} else {
			sender.sendMessage(
					formatMessage(getConfig().getString("messages.warnNone", "no warnings").replaceAll("%player%", p.getName())));
		}
		return true;
	}

	private String formatMessage(String str) {
		String funnyChar = new Character((char) 167).toString();
		str = str.replaceAll("&", funnyChar);
		return str;
	}

	private FileConfiguration getConfig() {
		return plugin.getConfig();
	}

	private OfflinePlayer getPlayer(String s) {
		return FigAdmin.getPlayer(s);
	}

	private boolean validName(String s) {
		return FigAdmin.validName(s);
	}

	private boolean hasPermission(CommandSender s, String p) {
		return FigAdmin.hasPermission(s, p);
	}

	private boolean figAdmin(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "FigAdmin version " + plugin.getDescription().getVersion());
		return true;
	}
}
