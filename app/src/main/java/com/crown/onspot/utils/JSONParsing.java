package com.crown.onspot.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONParsing {
    public static JSONObject ObjectFromString(String string) {
        try {
            return new JSONObject(string);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public static String StringFromObject(JSONObject jsonObject, String paramName) {
        try {
            return jsonObject.getString(paramName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static JSONObject ObjectFromObject(JSONObject object, String key) {
        try {
            return object.getJSONObject(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public static JSONArray ArrayFromObject(JSONObject object, String key) {
        try {
            return object.getJSONArray(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONArray();
    }
}
