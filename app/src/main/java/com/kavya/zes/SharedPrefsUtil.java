package com.kavya.zes;

import static com.kavya.zes.MainActivity.sharedPrefKey;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Copied from <a href="https://stackoverflow.com/a/18098090/5894824">StackOverflow</a>.
 */
public final class SharedPrefsUtil {

    private SharedPrefsUtil() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    public static Editor putDouble(@NonNull Editor edit, @NonNull String key, double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    public static double getDouble(@NonNull SharedPreferences prefs, @NonNull String key, double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }

    static void migrateOldPreferences(@NonNull Context context) {
        migrate(context, "cl.coders.mockposition.sharedpreferences");
        migrate(context, "cl.coders.faketraveler.sharedprefs");
    }

    private static void migrate(@NonNull Context context, String oldKey) {
        SharedPreferences oldPrefs = context.getSharedPreferences(oldKey, Context.MODE_PRIVATE);
        if (!oldPrefs.contains("version") && !oldPrefs.contains("lat")) return;

        Log.i(SharedPrefsUtil.class.toString(), "Migrating " + oldKey + " to new format...");
        Editor editor = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE).edit();

        if (oldPrefs.contains("version")) {
            editor.putInt("version", oldPrefs.getInt("version", 0));
        }

        if (oldKey.equals("cl.coders.mockposition.sharedpreferences")) {
            try {
                putDouble(editor, "lat", Double.parseDouble(oldPrefs.getString("lat", "12")));
            } catch (Throwable ignored) {
            }
            try {
                putDouble(editor, "lng", Double.parseDouble(oldPrefs.getString("lng", "15")));
            } catch (Throwable ignored) {
            }
            try {
                editor.putInt("mockCount", Integer.parseInt(oldPrefs.getString("howManyTimes", "0")));
            } catch (Throwable ignored) {
            }
            try {
                editor.putInt("mockFrequency", Integer.parseInt(oldPrefs.getString("timeInterval", "10")));
            } catch (Throwable ignored) {
            }
        } else {
            // Standard migration for newer formats
            putDouble(editor, "lat", getDouble(oldPrefs, "lat", 12));
            putDouble(editor, "lng", getDouble(oldPrefs, "lng", 15));
            putDouble(editor, "zoom", getDouble(oldPrefs, "zoom", 12));
            editor.putInt("mockCount", oldPrefs.getInt("mockCount", 0));
            editor.putInt("mockFrequency", oldPrefs.getInt("mockFrequency", 10));
            putDouble(editor, "dLat", getDouble(oldPrefs, "dLat", 0));
            putDouble(editor, "dLng", getDouble(oldPrefs, "dLng", 0));
            editor.putBoolean("mockSpeed", oldPrefs.getBoolean("mockSpeed", true));
            editor.putString("mapProvider", oldPrefs.getString("mapProvider", "OpenStreetMap"));
        }
        
        editor.putLong("endTime", oldPrefs.getLong("endTime", 0));
        editor.apply();

        oldPrefs.edit().clear().apply();
        Log.i(SharedPrefsUtil.class.toString(), "Migration from " + oldKey + " done!");
    }

}
