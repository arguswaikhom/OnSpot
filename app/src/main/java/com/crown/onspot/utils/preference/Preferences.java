package com.crown.onspot.utils.preference;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import static android.content.Context.MODE_PRIVATE;

public class Preferences {
    private static final String TAG = Preferences.class.getName();
    private static final String TAG_CREDENTIAL_SP = "CREDENTIAL_SP";
    private static Preferences instance = null;
    private SharedPreferences preferences;

    private Preferences(Context context) {
        this.preferences = context.getSharedPreferences(TAG_CREDENTIAL_SP, MODE_PRIVATE);
    }

    public static Preferences getInstance(Context context) {
        if (instance == null) {
            return new Preferences(context);
        }
        return instance;
    }

    public void setObject(Object object, PreferenceKey key) {
        String value;
        if (object.getClass().equals(String.class)) value = (String) object;
        else value = new Gson().toJson(object);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key.name(), value);
        editor.apply();
    }

    public <T> T getObject(PreferenceKey key, Class<T> tClass) {
        String json = preferences.getString(key.name(), null);
        if (json != null) {
            if (tClass.equals(String.class)) return (T) json;
            return new Gson().fromJson(json, tClass);
        }
        return null;
    }

    public void clearAll() {
        preferences.edit().clear().apply();
    }
}
