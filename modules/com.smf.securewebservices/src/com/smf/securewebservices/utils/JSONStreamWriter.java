package com.smf.securewebservices.utils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.smf.securewebservices.service.SecureJsonDataService;
/**
 * @author androettop
 */
public class JSONStreamWriter extends SecureJsonDataService.QueryResultWriter {
	private JSONArray data = new JSONArray();

	@Override
	public void write(JSONObject json) {
		data.put(json);
	}

	public JSONArray getJSONArray() {
		return this.data;
	}
}