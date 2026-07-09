package com.kavya.zes;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BookmarkUtil {
    private static final String BOOKMARKS_KEY = "bookmarks_json";

    public static List<Bookmark> getBookmarks(Context context) {
        List<Bookmark> bookmarks = new ArrayList<>();
        SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
        String json = sharedPref.getString(BOOKMARKS_KEY, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                bookmarks.add(new Bookmark(
                        obj.getString("name"),
                        obj.getDouble("lat"),
                        obj.getDouble("lng")
                ));
            }
        } catch (Exception e) {
            Log.e("BookmarkUtil", "Error parsing bookmarks", e);
        }
        return bookmarks;
    }

    public static void saveBookmarks(Context context, List<Bookmark> bookmarks) {
        JSONArray array = new JSONArray();
        try {
            for (Bookmark b : bookmarks) {
                JSONObject obj = new JSONObject();
                obj.put("name", b.name);
                obj.put("lat", b.lat);
                obj.put("lng", b.lng);
                array.put(obj);
            }
        } catch (Exception e) {
            Log.e("BookmarkUtil", "Error saving bookmarks", e);
        }
        SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
        sharedPref.edit().putString(BOOKMARKS_KEY, array.toString()).apply();
    }
}
