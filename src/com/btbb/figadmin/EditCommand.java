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

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Used to edit ban entities
 * 
 * @author yottabyte
 * @author Serge Humphrey
 * 
 */
public class EditCommand implements CommandExecutor {

	FigAdmin plugin;

	EditBan ban = null;

	/**
	 * Used to edit ban entities
	 */
	EditCommand(FigAdmin plugin) {
		this.plugin = plugin;
	}

	private static String banType(EditBan.BanType num) {
		switch (num) {
		case BAN:
			return "Ban   ";
		case IPBAN:
			return "IP-Ban";
		case WARN:
			return "Warn  ";
		default:
			return "?";
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!FigAdmin.hasPermission(sender, "figadmin.editban")) {
			sender.sendMessage(formatMessage(plugin.getConfig().getString("messages.noPermission")));
			return true;
		}

		if (args.length < 1)
			return false;

		plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
			@Override
			public void run() {
				if (!run2() && command.getUsage().length() > 0) {
					for (String line : command.getUsage().replace("<command>", label).split("\n")) {
						sender.sendMessage(formatMessage("&c" + line));
					}
				}
			}

			public boolean run2() {
				try {
					args[0] = args[0].toLowerCase();
					if (args[0].equalsIgnoreCase("list"))
						return list(sender, label, args);

					if (args[0].equalsIgnoreCase("load"))
						return load(sender, label, args);

					if (args[0].equalsIgnoreCase("select"))
						return select(sender, label, args);

					if (args[0].equalsIgnoreCase("delete"))
						return delete(sender, label, args);

					if (args[0].equalsIgnoreCase("search"))
						return search(sender, label, args);

					if (ban == null && args[0].equalsIgnoreCase("save") || args[0].equalsIgnoreCase("cancel")
							|| args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("view") || args[0].equalsIgnoreCase("reason")
							|| args[0].equalsIgnoreCase("time") || args[0].equalsIgnoreCase("ip")) {
						if (ban == null) {
							sender.sendMessage(ChatColor.RED + "You aren't editing a ban. Do /" + label + " select <id>");
							return true;
						}
					}
					if (args[0].equalsIgnoreCase("save"))
						return save(sender, args);
					if (args[0].equalsIgnoreCase("cancel"))
						return cancel(sender, args);
					if (args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("view"))
						return view(sender, args);
					if (args[0].equalsIgnoreCase("reason"))
						return reason(sender, args);
					if (args[0].equalsIgnoreCase("time"))
						return time(sender, label, args);
					if (args[0].equalsIgnoreCase("ip"))
						return ip(sender, label, args);
				} catch (Exception exc) {
					FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Error: EditCommand");
					exc.printStackTrace();
				}
				return false;
			}
		});
		return true;

	}

	public static void showBanInfo(EditBan eb, CommandSender sender) {
		DateFormat shortTime = DateFormat.getDateTimeInstance();
		sender.sendMessage(ChatColor.AQUA + banType(eb.type));
		sender.sendMessage(ChatColor.GOLD + " | " + ChatColor.WHITE + eb.name + ChatColor.YELLOW + " was banned by " + ChatColor.WHITE
				+ eb.admin + ChatColor.YELLOW);
		if (eb.ipAddress != null)
			sender.sendMessage(ChatColor.GOLD + " | IP: " + ChatColor.WHITE + eb.ipAddress);
		sender.sendMessage(ChatColor.GOLD + " | at " + shortTime.format((new Date(eb.time))));
		if (eb.endTime > 0)
			sender.sendMessage(
					ChatColor.GOLD + " | " + ChatColor.YELLOW + "Will be unbanned at " + shortTime.format((new Date(eb.endTime))));
		sender.sendMessage(ChatColor.GOLD + " | " + ChatColor.YELLOW + "Reason: " + ChatColor.GRAY + eb.reason);
	}

	private boolean list(CommandSender sender, String label, String[] args) {
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Usage: /" + label + " list <player>");
			return true;
		}
		if (!FigAdmin.validName(args[1])) {
			sender.sendMessage(formatMessage(plugin.getConfig().getString("messages.badPlayerName", "bad player name")));
			return true;
		}
		List<EditBan> bans = plugin.db.listRecords(args[1], true);
		if (bans.isEmpty()) {
			sender.sendMessage(ChatColor.RED + "No records");
			return true;
		}
		sender.sendMessage(ChatColor.GOLD + "Found " + bans.size() + " records for user " + bans.get(0).name + ":");
		for (EditBan ban : bans) {
			sender.sendMessage(ChatColor.AQUA + banType(ban.type) + ChatColor.YELLOW + ban.id + ": " + ChatColor.GREEN + ban.reason
					+ ChatColor.YELLOW + " by " + ban.admin);
		}
		return true;
	}

	private boolean search(CommandSender sender, String label, String[] args) {
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Usage: /" + label + " search <player>");
			return true;
		}
		List<EditBan> bans = plugin.db.listRecords(args[1], false);
		if (bans.isEmpty()) {
			sender.sendMessage(ChatColor.RED + "No records");
			return true;
		}
		sender.sendMessage(ChatColor.GOLD + "Found " + bans.size() + " records for keyword " + args[1] + ":");
		for (EditBan ban : bans) {
			sender.sendMessage(ChatColor.AQUA + banType(ban.type) + ChatColor.YELLOW + ban.id + " " + ban.name + ": " + ChatColor.GREEN
					+ ban.reason + ChatColor.YELLOW + " by " + ban.admin);
		}
		return true;
	}

	private boolean load(CommandSender sender, String label, String[] args) {
		if (ban != null) {
			sender.sendMessage(ChatColor.RED + "Finish what you're doing first!");
			return true;
		}

		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Usage: /" + label + " load <player>");
			return true;
		}
		if (!FigAdmin.validName(args[1])) {
			sender.sendMessage(ChatColor.RED + formatMessage(plugin.getConfig().getString("messages.badPlayerName", "bad player name")));
			return true;
		}
		UUID id;
		OfflinePlayer op = FigAdmin.getPlayer(args[1]);
		if (op == null)
			id = FigAdmin.getUUIDfromMojang(args[1]);
		else
			id = op.getUniqueId();
		if (id == null) {

			return true;
		}
		EditBan eb = plugin.db.loadFullRecord(id);
		if (eb == null) {
			sender.sendMessage(ChatColor.RED + "Unable to find the last ban/warn of this player");
			return true;
		}
		ban = eb;
		sender.sendMessage(ChatColor.GREEN + "Editing the last ban/warn of player " + eb.name + ": ");
		showBanInfo(eb, sender);
		return true;
	}

	private boolean select(CommandSender sender, String label, String[] args) {

		if (ban != null) {
			sender.sendMessage(ChatColor.RED + "Finish what you're doing first!");
			return true;
		}

		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Usage: /" + label + " load <ban id>");
			return true;
		}

		int id;
		try {
			id = Integer.parseInt(args[1]);
		} catch (NumberFormatException exc) {
			sender.sendMessage(ChatColor.RED + "ID has to be a number!");
			return true;
		}

		EditBan eb = plugin.db.loadFullRecord(id);
		if (eb == null) {
			sender.sendMessage(ChatColor.RED + "Unable to find a ban of this player");
			return true;
		}
		ban = eb;
		sender.sendMessage(ChatColor.GREEN + "Editing the last ban/warn of player " + eb.name + ": ");
		showBanInfo(eb, sender);
		return true;
	}

	private boolean save(CommandSender sender, String[] args) {
		if (plugin.db.saveFullRecord(ban)) {
			sender.sendMessage(ChatColor.GREEN + "Saved ban!");
		} else {
			sender.sendMessage(ChatColor.RED + "Saving Failed!");
		}
		ban = null;
		return true;

	}

	private boolean view(CommandSender sender, String[] args) {
		showBanInfo(ban, sender);
		return true;

	}

	private boolean reason(CommandSender sender, String[] args) {
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Usage: reason <add/set/show> (text)");
			return true;
		}
		boolean show = false;
		if (args[1].equalsIgnoreCase("add")) {
			if (args.length < 3) {
				sender.sendMessage(ChatColor.RED + "Usage: reason add <text>");
				return true;
			}
			ban.reason += " " + plugin.combineSplit(2, args, " ");
			ban.reason = formatMessage(ban.reason);
			show = true;
		}
		if (args[1].equalsIgnoreCase("set")) {
			if (args.length < 3) {
				sender.sendMessage(ChatColor.RED + "Usage: reason set <text>");
				show = true;
			}
			ban.reason = plugin.combineSplit(2, args, " ");
			ban.reason = formatMessage(ban.reason);
			show = true;
		}
		if (show || args[1].equalsIgnoreCase("show")) {
			sender.sendMessage(ChatColor.YELLOW + "Reason: " + ChatColor.WHITE + ban.reason);
			return true;
		}
		return false;
	}

	private boolean time(CommandSender sender, String label, String[] args) {
		if (args.length < 4) {
			sender.sendMessage(ChatColor.RED + "Usage: /" + label + " time <add/sub/set> <time> <sec/min/hour/day/week/month>");
			return true;
		}
		if (ban.type == EditBan.BanType.WARN) {
			sender.sendMessage(ChatColor.RED + "No such operation is possible");
			return true;
		}
		long time = plugin.parseTimeSpec(args[2], args[3]);
		if (time == 0) {
			sender.sendMessage(ChatColor.RED + "Invalid time format");
			return true;
		}
		boolean add = args[1].equalsIgnoreCase("add"), set = args[1].equalsIgnoreCase("set"), sub = args[1].equalsIgnoreCase("sub");
		if (add || set || sub) {
			if (ban.endTime == 0) {
				ban.endTime = ban.time;
			}
			if (add) {
				ban.endTime += time;
			} else if (set) {

				ban.endTime = ban.time + time;
			} else if (sub) {
				ban.endTime -= time;
			}
			Date date = new Date();
			date.setTime(ban.endTime);
			sender.sendMessage(ChatColor.YELLOW + "New time: " + ChatColor.WHITE + date.toString());
			return true;
		}
		return false;
	}

	private boolean ip(CommandSender sender, String label, String[] args) {
		if (args.length < 2 || (args.length == 1 && args[1].equalsIgnoreCase("set"))) {
			sender.sendMessage(ChatColor.RED + "Usage: /" + label + " ip <set|show> <ip>");
			return true;
		}
		if (ban.type == EditBan.BanType.WARN) {
			sender.sendMessage(ChatColor.RED + "No such operation is possible");
			return true;
		}
		if (args[1].equalsIgnoreCase("show")) {
			sender.sendMessage(ChatColor.YELLOW + "IP: " + ChatColor.WHITE + ban.ipAddress);
			return true;
		}
		if (!args[1].equalsIgnoreCase("set"))
			return false;
		String ip = args[2];
		if (!ip.matches
				("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")) {
			sender.sendMessage(ChatColor.RED + "Invalid IP format: " + ip);
			return true;
		}
		ban.ipAddress = ip;
		if (ban.type != EditBan.BanType.IPBAN)
			sender.sendMessage(ChatColor.YELLOW + "Warning: This is not an IP ban. Changes to IP will have no effect.");
		sender.sendMessage(ChatColor.YELLOW + "New ip: " + ip);
		
		return true;
	}

	private boolean delete(CommandSender sender, String label, String[] args) {
		if (ban != null) {
			sender.sendMessage(ChatColor.RED + "Finish what you're doing first!");
			return true;
		}
		if (args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Usage: /" + label + " delete [id]");
			return true;
		}
		int id;
		try {
			id = Integer.parseInt(args[1]);
		} catch (NumberFormatException exc) {
			sender.sendMessage(ChatColor.RED + "ID has to be a number!");
			return true;
		}
		boolean success = plugin.db.deleteFullRecord(id);
		if (success)
			sender.sendMessage(ChatColor.GREEN + "Deleted record " + id);
		else
			sender.sendMessage(ChatColor.RED + "Can't find record " + id);
		return true;
	}

	private boolean cancel(CommandSender sender, String[] args) {
		ban = null;
		sender.sendMessage(ChatColor.YELLOW + "Cancelled.");
		return true;
	}

	private String formatMessage(String str) {
		String funnyChar = new Character((char) 167).toString();
		str = str.replaceAll("&", funnyChar);
		return str;
	}

}
