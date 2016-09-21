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
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Provides methods for exporting bans to vanilla minecraft JSON
 * planned-players.json and panned-ips.json
 * 
 * @author Serge
 *
 */
public class VanillaBans {

	private VanillaBans() {
	}

	private static final SimpleDateFormat mcDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

	public static void exportBans(List<EditBan> bans) throws IOException {
		JsonArray mcbans = new JsonArray();
		JsonArray ipbans = new JsonArray();
		for (EditBan b : bans) {
			String created = "", expires = "";
			if (b.time > 0)
				created = mcDateFormat.format(new Date(b.time));
			if (b.endTime > 0)
				expires = mcDateFormat.format(new Date(b.endTime));
			if (b.type == EditBan.BanType.IPBAN && b.ipAddress != null)
				ipbans.add(new IPBan(b.ipAddress, created, b.admin, expires, b.reason).toJson());
			if (b.type == EditBan.BanType.BAN || (b.type == EditBan.BanType.IPBAN && b.ipAddress == null))
				mcbans.add(new MCBan(b.uuid.toString(), b.name, created, b.admin, expires, b.reason).toJson());
		}

		GsonBuilder gsonbuilder = (new GsonBuilder()).setPrettyPrinting();
		Gson b = gsonbuilder.create();
		String s = b.toJson(mcbans);
		BufferedWriter bufferedwriter = null;

		try {
			bufferedwriter = Files.newWriter(new File("banned-players.json"), Charsets.UTF_8);
			bufferedwriter.write(s);
		} finally {
			IOUtils.closeQuietly(bufferedwriter);

			s = b.toJson(ipbans);
		}
		try {
			bufferedwriter = Files.newWriter(new File("banned-ips.json"), Charsets.UTF_8);
			bufferedwriter.write(s);
		} finally {
			IOUtils.closeQuietly(bufferedwriter);
		}
	}

	private static class MCBan {

		String uuid, name, created, source, expires, reason;

		/**
		 * @param "uuid":
		 *            "8667ba71-b85a-4004-af54-457a9734eed7",
		 * @param"name": "Steve",
		 * @param"created": "2016-09-20 15:04:49 -0300",
		 * @param"source": "(Unknown)",
		 * @param"expires": "forever",
		 * @param"reason": "Banned by an operator."
		 */
		MCBan(String uuid, String name, String created, String source, String expires, String reason) {
			this.uuid = uuid;
			this.name = name;
			this.created = created;
			this.source = source;
			this.expires = expires;
			this.reason = reason;
		}

		JsonObject toJson() {
			JsonObject obj = new JsonObject();
			obj.addProperty("uuid", uuid);
			obj.addProperty("name", name);
			obj.addProperty("created", created);
			obj.addProperty("source", source);
			obj.addProperty("expires", expires);
			obj.addProperty("reason", reason);

			return obj;
		}
	}

	private static class IPBan {
		String ip, created, source, expires, reason;

		/**
		 * @param"ip": "127.0.0.1",
		 * @param"created": "2016-09-20 16:29:50 -0300",
		 * @param"source": "Server",
		 * @param"expires": "forever",
		 * @param"reason": "Banned by an operator."
		 * 
		 * @param ip
		 * @param created
		 * @param source
		 * @param expires
		 * @param reason
		 */
		IPBan(String ip, String created, String source, String expires, String reason) {
			this.ip = ip;
			this.created = created;
			this.source = source;
			this.expires = expires;
			this.reason = reason;
		}

		JsonObject toJson() {
			JsonObject obj = new JsonObject();
			obj.addProperty("ip", ip);
			obj.addProperty("created", created);
			obj.addProperty("source", source);
			obj.addProperty("expires", expires);
			obj.addProperty("reason", reason);

			return obj;
		}
	}
}
