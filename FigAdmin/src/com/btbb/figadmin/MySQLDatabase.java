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

import static org.bukkit.Bukkit.getLogger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;

public class MySQLDatabase extends Database {

	private FigAdmin plugin;

	public MySQLDatabase(FigAdmin instance) {
		plugin = instance;
	}

	public Connection getSQLConnection(String mysqlDatabase) {
		FileConfiguration Config = plugin.getConfig();
		String mysqlUser = Config.getString("mysql-user", "root");
		String mysqlPassword = Config.getString("mysql-password", "root");
		try {

			return DriverManager.getConnection(mysqlDatabase + "?autoReconnect=true&user=" + mysqlUser + "&password=" + mysqlPassword);

		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "Unable to retreive connection", ex);
		}
		return null;
	}

	public Connection getSQLConnection() {
		String mysqlDatabase = plugin.getConfig().getString("mysql-database", "jdbc:mysql://localhost:3306/minecraft");
		return getSQLConnection(mysqlDatabase);

	}

	public boolean initialize(FigAdmin plugin) {
		this.plugin = plugin;
		Connection conn = null;
		PreparedStatement ps = null;

		String table = plugin.getConfig().getString("mysql-table");
		try {
			conn = getSQLConnection();
			DatabaseMetaData dbm = conn.getMetaData();
			// Table create if not it exists
			if (!dbm.getTables(null, null, table, null).next()) {
				getLogger().log(Level.INFO, "[FigAdmin] Creating table " + table + ".");
				ps = conn.prepareStatement("CREATE TABLE `" + table + "` ( \n" + "  `name` varchar(32) DEFAULT 'undefined', \n"
						+ "  `uuid` varchar(36) NOT NULL, \n" + "  `reason` text NOT NULL, \n " + "  `admin` varchar(32) NOT NULL, \n"
						+ "  `time` bigint(20) NOT NULL, \n " + "  `temptime` bigint(20) NOT NULL DEFAULT '0', \n"
						+ "  `type` int(11) NOT NULL DEFAULT '0', \n" + "  `id` int(11) NOT NULL AUTO_INCREMENT, \n"
						+ "  `ip` varchar(16) DEFAULT NULL, \n" + "  PRIMARY KEY (`id`) USING BTREE \n"
						+ ") ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ROW_FORMAT=DYNAMIC;");
				ps.execute();
				if (!dbm.getTables(null, null, table, null).next())
					throw new SQLException("Table " + table + " not found; tired to create and failed");
			}
			// Clean up old temp bans
			ps = conn.prepareStatement("DELETE FROM " + table + " WHERE (type = 0 OR type = 1) AND (temptime > 0) AND (temptime < ?)");
			ps.setLong(1, System.currentTimeMillis());
			ps.execute();
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
			return false;
		}
		try {
			if (ps != null)
				ps.close();
			if (conn != null)
				conn.close();
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
		}

		return true;

	}

	public String getAddress(UUID playerId) {

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String mysqlTable = plugin.getConfig().getString("mysql-table");
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("SELECT `ip` FROM `" + mysqlTable + "` WHERE uuid = ?");
			ps.setString(1, playerId.toString());
			rs = ps.executeQuery();
			while (rs.next()) {
				String ip = rs.getString("ip");
				return ip;
			}
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
				if (rs != null)
					rs.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}

		}
		return null;
	}

	public boolean removeFromBanlist(UUID playerId) {

		String mysqlTable = plugin.getConfig().getString("mysql-table");

		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("DELETE FROM " + mysqlTable + " WHERE uuid = ? AND (type = 0 or type = 1) ORDER BY time DESC");
			ps.setString(1, playerId.toString());
			ps.executeUpdate();
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
			return false;
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return true;
	}

	public boolean removeFromBanlist(String player) {
		String mysqlTable = plugin.getConfig().getString("mysql-table");
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("DELETE FROM " + mysqlTable + " WHERE name = ? AND (type = 0 or type = 1) ORDER BY time DESC");
			ps.setString(1, player);
			ps.executeUpdate();
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
			return false;
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return true;
	}

	public void addPlayer(EditBan e) {

		String mysqlTable = plugin.getConfig().getString("mysql-table");

		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = getSQLConnection();
			ps = conn
					.prepareStatement("INSERT INTO `" + mysqlTable + "` (name,uuid,reason,admin,temptime,type,time,ip) VALUES(?,?,?,?,?,?,?,?)");
			ps.setString(1, e.name);
			ps.setString(2, e.uuid.toString());
			ps.setString(3, e.reason);
			ps.setString(4, e.admin);
			if (e.endTime < 1) {
				ps.setLong(5, 0);
			} else
				ps.setLong(5, e.endTime);
			ps.setInt(6, e.type.ordinal());
			ps.setLong(7, System.currentTimeMillis());
			ps.setString(8, e.ipAddress);
			ps.executeUpdate();
			// Update banedit ID
			PreparedStatement getID = conn.prepareStatement("SELECT LAST_INSERT_ID()");
			ResultSet rsID = getID.executeQuery();
			rsID.next();
			e.id = rsID.getInt(1);
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
	}

	public String getBanReason(UUID playerId) {
		Connection conn = getSQLConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String mysqlTable = plugin.getConfig().getString("mysql-table");
		try {
			ps = conn.prepareStatement("SELECT * FROM `" + mysqlTable + "` WHERE uuid = ? ORDER BY id DESC LIMIT 1");
			ps.setString(1, playerId.toString());
			rs = ps.executeQuery();
			while (rs.next()) {
				String reason = rs.getString("reason");
				return reason;
			}
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return null;
	}

	public void updateAddress(UUID playerId, String ip) {

		Connection conn = null;
		PreparedStatement ps = null;
		String mysqlTable = plugin.getConfig().getString("mysql-table");
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("UPDATE `" + mysqlTable + "` SET ip = ? WHERE uuid = ?");
			ps.setString(1, ip);
			ps.setString(2, playerId.toString());
			ps.executeUpdate();
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}

	}

	public ArrayList<EditBan> listRecords(UUID player) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String mysqlTable = plugin.getConfig().getString("mysql-table");
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("SELECT * FROM `" + mysqlTable + "` WHERE uuid = ? ORDER BY time DESC LIMIT 10");
			ps.setString(1, player.toString());
			rs = ps.executeQuery();
			ArrayList<EditBan> bans = new ArrayList<EditBan>();
			while (rs.next()) {
				bans.add(getEditBan(rs));
			}
			return bans;
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
				if (rs != null)
					rs.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return null;
	}

	@Override
	public ArrayList<EditBan> listRecords(String name, boolean exact) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String mysqlTable = plugin.getConfig().getString("mysql-table");
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement(
					"SELECT * FROM `" + mysqlTable + "` WHERE name " + ((exact) ? "= ?" : "LIKE ?") + " ORDER BY time DESC LIMIT 10");
			if (exact) {
				ps.setString(1, name);
			} else {
				ps.setString(1, "%" + name + "%");
			}
			rs = ps.executeQuery();
			ArrayList<EditBan> bans = new ArrayList<EditBan>();
			while (rs.next()) {
				bans.add(getEditBan(rs));
			}
			return bans;
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
				if (rs != null)
					rs.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return null;
	}

	protected EditBan loadFullRecord(UUID uuid) {
		return loadFullRecord(uuid, -1);
	}

	protected EditBan loadFullRecord(int id) {
		return loadFullRecord(null, id);
	}

	/*
	 * if pName = null then it will look up for index id
	 */
	private EditBan loadFullRecord(UUID playerId, int id) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String mysqlTable = plugin.getConfig().getString("mysql-table");
		try {
			String statement = "SELECT * FROM " + mysqlTable + " WHERE uuid = ? ORDER BY time DESC LIMIT 1";
			if (playerId == null) {
				statement = "SELECT * FROM " + mysqlTable + " WHERE id = ?";
			}
			conn = getSQLConnection();
			ps = conn.prepareStatement(statement);
			if (playerId == null) {
				ps.setInt(1, id);
			} else {
				ps.setString(1, playerId.toString());
			}
			rs = ps.executeQuery();
			while (rs.next()) {
				return new EditBan(rs.getInt("id"), UUID.fromString(rs.getString("uuid")), rs.getString("name"), rs.getString("reason"),
						rs.getString("admin"), rs.getLong("time"), rs.getLong("temptime"), EditBan.BanType.values()[rs.getInt("type")], rs.getString("ip"));
			}
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
				if (rs != null)
					rs.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return null;
	}

	public boolean saveFullRecord(EditBan ban) {

		String mysqlTable = plugin.getConfig().getString("mysql-table");

		Connection conn = null;
		PreparedStatement ps = null;

		boolean success = false;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("UPDATE " + mysqlTable
					+ " SET name = ?, reason = ?, admin = ?, time = ?, temptime = ?, type = ?, uuid = ?, ip = ? WHERE id = ? LIMIT 1");
			
			ps.setString(1, ban.name);
			ps.setString(2, ban.reason);
			ps.setString(3, ban.admin);
			ps.setLong(4, ban.time);
			ps.setLong(5, ban.endTime);
			ps.setInt(6, ban.type.ordinal());
			ps.setString(7, ban.uuid.toString());
			ps.setString(8, ban.ipAddress);
			ps.setInt(9, ban.id);
			success = ps.executeUpdate() > 0;
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return success;
	}

	@Override
	public ArrayList<EditBan> getBannedPlayers() {
		ArrayList<EditBan> list = new ArrayList<EditBan>();
		Connection conn = getSQLConnection();
		String mysqlTable = plugin.getConfig().getString("mysql-table");
		if (conn == null) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Could not establish SQL connection. Disabling FigAdmin");
			plugin.getServer().getPluginManager().disablePlugin(plugin);
			return list;
		} else {

			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				ps = conn.prepareStatement(
						"SELECT * FROM " + mysqlTable + " WHERE (temptime > ? OR temptime = 0)");
				ps.setLong(1, System.currentTimeMillis());
				rs = ps.executeQuery();
				while (rs.next()) {
					EditBan e = getEditBan(rs);
					list.add(e);
				}
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
			} finally {
				try {
					if (ps != null)
						ps.close();
					if (rs != null)
						rs.close();
					if (conn != null)
						conn.close();
				} catch (SQLException ex) {
					FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
				}
			}

			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
				plugin.getServer().getPluginManager().disablePlugin(plugin);
			}
		}
		return list;

	}

	private EditBan getEditBan(ResultSet rs) throws SQLException {
		return new EditBan(rs.getInt("id"), UUID.fromString(rs.getString("uuid")), rs.getString("name"), rs.getString("reason"),
				rs.getString("admin"), rs.getLong("time"), rs.getLong("temptime"), EditBan.BanType.values()[rs.getInt("type")], rs.getString("ip"));
	}

	@Override
	protected boolean deleteFullRecord(int id) {

		String mysqlTable = plugin.getConfig().getString("mysql-table");

		Connection conn = null;
		PreparedStatement ps = null;
		boolean success = false;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("DELETE FROM " + mysqlTable + " WHERE id = ?  ORDER BY time DESC");
			ps.setInt(1, id);
			ps.executeUpdate();
			success = ps.getUpdateCount() > 0;
			
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
			return false;
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return success;
	}

	@Override
	public int getWarnCount(UUID player) {

		int warns = 0;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String mysqlTable = plugin.getConfig().getString("mysql-table");
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("SELECT * FROM `" + mysqlTable + "` WHERE uuid = ? AND type = 2");

			ps.setString(1, player.toString());

			rs = ps.executeQuery();
			while (rs.next()) {
				warns++;
			}
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
				if (rs != null)
					rs.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return warns;
	}

	@Override
	public int clearWarnings(UUID playerId) {
		String mysqlTable = plugin.getConfig().getString("mysql-table");

		Connection conn = null;
		PreparedStatement ps = null;
		int warns = 0;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("DELETE FROM " + mysqlTable + " WHERE uuid = ? AND type = 2");
			ps.setString(1, playerId.toString());
			ps.executeUpdate();
			warns = ps.getUpdateCount();
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return warns;
	}

	@Override
	public int unbanIP(String ip) {
		String mysqlTable = plugin.getConfig().getString("mysql-table");
		Connection conn = null;
		PreparedStatement ps = null;
		int res = 0;
		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement("DELETE FROM " + mysqlTable + " WHERE ip = ? AND (type = 0 or type = 1) ORDER BY time DESC");
			ps.setString(1, ip);
			res = ps.executeUpdate();
		} catch (SQLException ex) {
			FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Couldn't execute MySQL statement: ", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				FigAdmin.log.log(Level.SEVERE, "[FigAdmin] Failed to close MySQL connection: ", ex);
			}
		}
		return res;
	}

}
