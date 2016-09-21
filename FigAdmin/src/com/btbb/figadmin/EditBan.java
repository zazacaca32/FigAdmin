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

import java.util.UUID;

public class EditBan {

	int id;
	UUID uuid;
	String name;
	String reason;
	String admin;
	String ipAddress;
	long time;
	long endTime;
	BanType type;

	// BAN = 0; IPBAN = 1 ;WARN = 2;
	public static enum BanType {
		BAN, IPBAN, WARN;

	}

	EditBan(int id, UUID uuid, String name, String reason, String admin, long time, long endTime, BanType type, String IP) {
		this.id = id;
		this.uuid = uuid;
		this.name = name;
		this.reason = reason;
		this.admin = admin;
		this.time = time;
		this.endTime = endTime;
		this.type = type;
		this.ipAddress = IP;
	}

	EditBan(UUID uuid, String name, String reason, String admin, long endTime, BanType type, String IP) {
		this.id = 0;
		this.uuid = uuid;
		this.name = name;
		this.reason = reason;
		this.admin = admin;
		this.time = System.currentTimeMillis();
		this.endTime = endTime;
		this.type = type;
		this.ipAddress = IP;
	}
	
	EditBan(UUID uuid, String name, String reason, String admin, BanType type, String IP) {
		this.id = 0;
		this.uuid = uuid;
		this.name = name;
		this.reason = reason;
		this.admin = admin;
		this.time = System.currentTimeMillis();
		this.endTime = 0;
		this.type = type;
		this.ipAddress = IP;
	}
	
	private EditBan() {
	}

	/*
	 * Load from data line as from this.toString()
	 */
	public static EditBan loadBan(String data) {
		String[] d = data.split("\\|");
		return loadBan(d);
	}

	public static final int UID = 0, NAME = 1, ID = 2, REASON = 3, ADMIN = 4, IP = 5, TIME = 6, ENDTIME = 7, TYPE = 8;

	public static EditBan loadBan(String[] d) {
		if (d.length < 7) {
			return null;
		}
		EditBan e = new EditBan();
		e.uuid = UUID.fromString(d[UID]);
		e.name = d[NAME];
		e.id = Integer.parseInt(d[ID]);
		e.reason = d[REASON];
		e.admin = d[ADMIN];
		e.ipAddress = (d[IP].equals("null")) ? null : d[IP];
		e.time = Long.parseLong(d[TIME]);
		e.endTime = Long.parseLong(d[ENDTIME]);
		e.type = BanType.values()[Integer.parseInt(d[TYPE])];
		return e;
	}

	public String toString() {
		StringBuffer s = new StringBuffer(id);
		s.append(uuid);
		s.append("|");
		s.append(name);
		s.append("|");
		s.append(id);
		s.append("|");
		s.append(reason);
		s.append("|");
		s.append(admin);
		s.append("|");
		s.append(ipAddress);
		s.append("|");
		s.append(time);
		s.append("|");
		s.append(endTime);
		s.append("|");
		s.append(type.ordinal());
		return s.toString();
	}

	public boolean equals(Object object) {
		if (object instanceof String) {
			return ((String) object).toLowerCase().equals(this.uuid);
		} else if (object instanceof EditBan) {
			EditBan o = (EditBan) object;
			return o.uuid.equals(this.uuid) && o.admin.equals(this.admin) && o.reason.equals(this.reason)
					&& o.ipAddress.equals(this.ipAddress) && o.time == this.time && o.endTime == this.endTime && o.type == this.type;
		}
		return false;

	}
}
