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
package com.evilmidget38;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class NameFetcher implements Callable<List<NameTimestampPair>> {
	private static final String PROFILE_URL = "https://api.mojang.com/user/profiles/";
	private final JSONParser jsonParser = new JSONParser();
	private final UUID id;

	public NameFetcher(UUID id) {
		this.id = id;
	}

	public List<NameTimestampPair> call() throws Exception {
		List<NameTimestampPair> namePairs = new LinkedList<NameTimestampPair>();
		HttpURLConnection connection = createConnection(id);
		JSONArray array = (JSONArray) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
		for (Object profile : array) {
			JSONObject jsonProfile = (JSONObject) profile;
			String name = (String) jsonProfile.get("name");
			Object o = jsonProfile.get("changedToAt");
			Long changed = -1L;
			if (o != null)
				changed = (o instanceof Long) ? (long) o : Long.parseLong(o.toString());
			namePairs.add(new NameTimestampPair(name, changed));
		}

		return namePairs;
	}

	private static HttpURLConnection createConnection(UUID id) throws Exception {
		URL url = new URL(PROFILE_URL + id.toString().replaceAll("-", "") + "/names");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		return connection;
	}
}