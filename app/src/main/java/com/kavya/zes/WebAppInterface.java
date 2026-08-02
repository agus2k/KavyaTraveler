package com.kavya.zes;

import static com.kavya.zes.MainActivity.SourceChange.CHANGE_FROM_MAP;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.annotation.NonNull;


public class WebAppInterface {

    @NonNull
    private final MainActivity mainActivity;

    public WebAppInterface(@NonNull MainActivity mA) {
        mainActivity = mA;
    }

    /**
     * Set position in GUI. This method is called by javascript when there is a long press in the map.
     *
     * @param str String containing lat and lng
     */
    @JavascriptInterface
    public void setPosition(final String str) {
        mainActivity.runOnUiThread(() -> {
            String lat = str.substring(str.indexOf('(') + 1, str.indexOf(','));
            String lng = str.substring(str.indexOf(',') + 2, str.indexOf(')'));

            try {
                mainActivity.setLatLng(Double.parseDouble(lat), Double.parseDouble(lng), CHANGE_FROM_MAP);
            } catch (Throwable t) {
                Log.e(WebAppInterface.class.toString(), "Could not set new position from map!", t);
            }
        });
    }

    @JavascriptInterface
    public void setZoom(final String str) {
        mainActivity.runOnUiThread(() -> {
            try {
                mainActivity.setZoom(Double.parseDouble(str));
            } catch (Throwable t) {
                Log.e(WebAppInterface.class.toString(), "Could not save zoom!", t);
            }
        });
    }

    @JavascriptInterface
    public void checkSecretCode(String query) {
        if (query != null && query.equalsIgnoreCase("ISTRIKUCANTIKSEKALI")) {
            mainActivity.runOnUiThread(() -> {
                SharedPreferences sharedPref = mainActivity.getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
                sharedPref.edit().putBoolean("isPremium", true).apply();
                Toast.makeText(mainActivity, "Akses Premium Rumah Pak Suga Aktif! Salam untuk Ibu :)", Toast.LENGTH_LONG).show();
            });
        }
    }

}
