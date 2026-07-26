package com.kavya.zes;

import static com.kavya.zes.MainActivity.SourceChange.CHANGE_FROM_EDITTEXT;
import static com.kavya.zes.MainActivity.SourceChange.CHANGE_FROM_MAP;
import static com.kavya.zes.MainActivity.SourceChange.LOAD;
import static com.kavya.zes.MainActivity.SourceChange.NONE;
import static com.kavya.zes.SharedPrefsUtil.getDouble;
import static com.kavya.zes.SharedPrefsUtil.migrateOldPreferences;
import static com.kavya.zes.SharedPrefsUtil.putDouble;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements ServiceConnection {

    @NonNull
    public static final String sharedPrefKey = "com.kavya.zes.sharedprefs";
    @NonNull
    public static DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.#######", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private MaterialButton buttonApply;
    private MaterialButton buttonStop;
    private WebView webView;
    private EditText editTextLat;
    private EditText editTextLng;
    private Context context;
    private int currentVersion;

    @NonNull
    private SourceChange srcChange = NONE;

    @Nullable
    private MockedLocationService.MockedBinder binder = null;

    // Config
    private int version;
    private double lat;
    private double lng;
    private double zoom;
    private int mockCount;
    private int mockFrequency;
    private double dLat;
    private double dLng;
    private double circleLat;
    private double circleLng;
    private boolean mockSpeed;
    private long endTime;
    private String mapProvider;

    @Override
    @SuppressLint("SetJavaScriptEnabled") // XSS unlikely an issue here...
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            
            // Apply bottom margin to controls card
            View controlsCard = findViewById(R.id.controls_card);
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) controlsCard.getLayoutParams();
            lp.bottomMargin = bars.bottom + (int) (16 * getResources().getDisplayMetrics().density);
            controlsCard.setLayoutParams(lp);

            // Inject safe area top into WebView
            float density = getResources().getDisplayMetrics().density;
            int topInsetDp = (int) (bars.top / density);
            webView.evaluateJavascript("document.documentElement.style.setProperty('--safe-area-inset-top', '" + topInsetDp + "px');", null);

            return insets;
        });

        context = getApplicationContext();
        webView = findViewById(R.id.webView0);
        WebAppInterface webAppInterface = new WebAppInterface(this);

        buttonApply = findViewById(R.id.button_apply);
        buttonStop = findViewById(R.id.button_stop);
        MaterialButton buttonSettings = findViewById(R.id.button_settings);
        MaterialButton buttonBookmarks = findViewById(R.id.button_bookmarks);
        editTextLat = findViewById(R.id.editTextLat);
        editTextLng = findViewById(R.id.editTextLng);

        buttonApply.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                    return;
                }
            }
            
            // Tambahkan pengecekan izin lokasi eksplisit
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 102);
                return;
            }
            
            startMock();
        });
        buttonStop.setOnClickListener(view -> {
            if (binder != null) {
                unbindService(this);
                disconnectService();
            }
        });
        buttonSettings.setOnClickListener(view -> {
            Intent myIntent = new Intent(getBaseContext(), MoreActivity.class);
            startActivity(myIntent);
        });
        buttonBookmarks.setOnClickListener(view -> showBookmarksDialog());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.addJavascriptInterface(webAppInterface, "Android");

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentVersion = (int) (pInfo.getLongVersionCode() >> 32);
            } else {
                currentVersion = pInfo.versionCode;
            }
        } catch (NameNotFoundException e) {
            Log.e(MainActivity.class.toString(), "Could not read version info!", e);
        }

        SharedPreferences sharedPref = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        int precision = sharedPref.getInt("decimalPrecision", 7);
        StringBuilder pattern = new StringBuilder("#.");
        for (int i = 0; i < precision; i++) pattern.append("#");
        DECIMAL_FORMAT = new DecimalFormat(pattern.toString(), DecimalFormatSymbols.getInstance(Locale.ROOT));

        loadSharedPrefs();
        applyIntentOrDefault(getIntent());

        editTextLat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!editTextLat.getText().toString().isEmpty() && !editTextLat.getText().toString().equals("-")) {
                    if (srcChange != CHANGE_FROM_MAP) {
                        try {
                            lat = Double.parseDouble(editTextLat.getText().toString());
                            setLatLng(lat, lng, CHANGE_FROM_EDITTEXT);
                        } catch (Throwable t) {
                            Log.e(MainActivity.class.toString(), "Could not read latitude!", t);
                        }
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        editTextLng.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!editTextLng.getText().toString().isEmpty() && !editTextLng.getText().toString().equals("-")) {
                    if (srcChange != CHANGE_FROM_MAP) {
                        try {
                            lng = Double.parseDouble(editTextLng.getText().toString());
                            setLatLng(lat, lng, CHANGE_FROM_EDITTEXT);
                        } catch (Throwable t) {
                            Log.e(MainActivity.class.toString(), "Could not read longitude!", t);
                        }
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        //2do check running on start?
        if (endTime > System.currentTimeMillis()) {
            changeButtonToStop();
        } else {
            endTime = 0;
            saveSettings();
        }

        checkUpdate();
    }

    private void startMock() {
        Intent intent = new Intent(this, MockedLocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        if (binder == null) {
            bindService(intent, this, BIND_AUTO_CREATE);
        } else {
            applyLocation();
        }
    }

    private void checkUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            999);
                } catch (Exception e) {
                    Log.e("MainActivity", "Update flow failed", e);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        context = getApplicationContext();
        SharedPreferences sharedPref = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        int precision = sharedPref.getInt("decimalPrecision", 7);
        StringBuilder pattern = new StringBuilder("#.");
        for (int i = 0; i < precision; i++) pattern.append("#");
        DECIMAL_FORMAT = new DecimalFormat(pattern.toString(), DecimalFormatSymbols.getInstance(Locale.ROOT));

        loadSharedPrefs();

        // Check for updates in progress
        AppUpdateManagerFactory.create(this).getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    AppUpdateManagerFactory.create(this).startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, 999);
                } catch (Exception e) {
                    Log.e("MainActivity", "Resume update failed", e);
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        SharedPreferences sharedPref = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        int precision = sharedPref.getInt("decimalPrecision", 7);
        StringBuilder pattern = new StringBuilder("#.");
        for (int i = 0; i < precision; i++) pattern.append("#");
        DECIMAL_FORMAT = new DecimalFormat(pattern.toString(), DecimalFormatSymbols.getInstance(Locale.ROOT));

        loadSharedPrefs();
        applyIntentOrDefault(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Check and (re-)initialize shared preferences.
     */
    private void loadSharedPrefs() {
        migrateOldPreferences(context);

        SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);

        version = sharedPref.getInt("version", 0);
        lat = getDouble(sharedPref, "lat", -7.7496395);
        lng = getDouble(sharedPref, "lng", 113.426669);
        zoom = getDouble(sharedPref, "zoom", 12);
        mockCount = sharedPref.getInt("mockCount", 0);
        mockFrequency = sharedPref.getInt("mockFrequency", 10);
        if (mockFrequency <= 0) mockFrequency = 1;
        dLat = getDouble(sharedPref, "dLat", 0);
        dLng = getDouble(sharedPref, "dLng", 0);
        circleLat = getDouble(sharedPref, "circleLat", 0);
        circleLng = getDouble(sharedPref, "circleLng", 0);
        mockSpeed = sharedPref.getBoolean("mockSpeed", true);
        endTime = sharedPref.getLong("endTime", 0);
        mapProvider = sharedPref.getString("mapProvider", MapProviderUtil.getDefaultMapProvider(Locale.getDefault()));

        if (version != currentVersion) {
            version = currentVersion;
            saveSettings();
        }
    }

    private void saveSettings() {
        Editor editor = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE).edit();

        editor.putInt("version", version);
        putDouble(editor, "lat", lat);
        putDouble(editor, "lng", lng);
        putDouble(editor, "zoom", zoom);
        editor.putInt("mockCount", mockCount);
        editor.putInt("mockFrequency", mockFrequency);
        putDouble(editor, "dLat", dLat);
        putDouble(editor, "dLng", dLng);
        putDouble(editor, "circleLat", circleLat);
        putDouble(editor, "circleLng", circleLng);
        editor.putBoolean("mockSpeed", mockSpeed);
        editor.putLong("endTime", endTime);
        editor.putString("mapProvider", mapProvider);

        editor.apply();
    }

    private void applyIntentOrDefault(Intent intent) {
        String intentData = intent.getDataString();
        if (intentData != null) {
            try {
                GeoUri uri = GeoUri.parse(intentData);
                Log.i(MainActivity.class.toString(), "Received geo intent: " + uri);
                if (uri != null) {
                    lat = uri.lat();
                    lng = uri.lng();
                    Double zoomTmp = uri.zoom();
                    if (zoomTmp != null) zoom = zoomTmp;
                }
            } catch (Throwable t) {
                Log.e(MainActivity.class.toString(), "Could not read geo intent!", t);
            }
        }

        setLatLng(lat, lng, LOAD);

        SharedPreferences sharedPref = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        int radius = sharedPref.getInt("circleRadius", 300);

        webView.loadUrl(Uri.parse("file:///android_asset/map.html").buildUpon()
                .appendQueryParameter("lat", "" + lat)
                .appendQueryParameter("lng", "" + lng)
                .appendQueryParameter("zoom", "" + zoom)
                .appendQueryParameter("radius", "" + radius)
                .appendQueryParameter("cLat", "" + circleLat)
                .appendQueryParameter("cLng", "" + circleLng)
                .appendQueryParameter("provider", mapProvider)
                .build()
                .toString());
    }

    /**
     * Apply a mocked location, and start an alarm to keep doing it if mockCount is > 1
     * This method is called when "Apply" button is pressed.
     */
    protected void applyLocation() {
        if (latIsEmpty() || lngIsEmpty()) {
            showSnackbar(context.getResources().getString(R.string.MainActivity_NoLatLong));
            return;
        }

        lat = Double.parseDouble(editTextLat.getText().toString());
        lng = Double.parseDouble(editTextLng.getText().toString());

        if (binder != null) {
            float[] speed = {0};
            if (mockSpeed) {
                Location.distanceBetween(lat, lng, lat + dLat / 1000000, lng + dLng / 1000000, speed);
                speed[0] /= mockFrequency * 1000L;
            }
            binder.startMock(lng, lat, dLng / 1000000, dLat / 1000000, mockFrequency * 1000L, mockCount, speed[0]);
            endTime = System.currentTimeMillis() + (mockCount - 1L) * mockFrequency * 1000L;
            saveSettings();
        }
    }

    /**
     * Shows a snackbar
     */
    void showSnackbar(String str) {
        Snackbar.make(findViewById(R.id.main_layout), str, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Shows a snackbar
     */
    void showSnackbar(@StringRes int strRes) {
        Snackbar.make(findViewById(R.id.main_layout), strRes, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Returns true editTextLat has no text
     */
    boolean latIsEmpty() {
        if (editTextLat.getText() == null) return true;
        return editTextLat.getText().toString().trim().isEmpty();
    }

    /**
     * Returns true editTextLng has no text
     */
    boolean lngIsEmpty() {
        if (editTextLng.getText() == null) return true;
        return editTextLng.getText().toString().trim().isEmpty();
    }

    private void setMapMarker(double lat, double lng) {
        if (webView == null || webView.getUrl() == null) return;
        SharedPreferences sharedPref = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        int radius = sharedPref.getInt("circleRadius", 300);
        webView.loadUrl("javascript:setOnMap(" + lat + "," + lng + "," + radius + ");");
    }

    /**
     * Changes the button to Apply, and its behavior.
     */
    void changeButtonToApply() {
        // No longer toggling a single button
    }

    /**
     * Changes the button to Stop, and its behavior.
     */
    void changeButtonToStop() {
        // No longer toggling a single button
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
        saveSettings();
    }

    /**
     * Sets latitude and longitude
     *
     * @param mLat      latitude
     * @param mLng      longitude
     * @param srcChange CHANGE_FROM_EDITTEXT or CHANGE_FROM_MAP, indicates from where comes the change
     */
    void setLatLng(double mLat, double mLng, SourceChange srcChange) {
        lat = mLat;
        lng = mLng;

        if (srcChange == CHANGE_FROM_EDITTEXT || srcChange == LOAD) {
            setMapMarker(lat, lng);
        }
        if (srcChange == CHANGE_FROM_MAP || srcChange == LOAD) {
            this.srcChange = CHANGE_FROM_MAP;
            editTextLat.setText(DECIMAL_FORMAT.format(lat));
            editTextLng.setText(DECIMAL_FORMAT.format(lng));
            this.srcChange = NONE;
        }

        saveSettings();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder = (MockedLocationService.MockedBinder) service;
        binder.mockState.observe(this, this::onMockedStateChange);
        binder.mockedLocation.observe(this, this::onMockedLocationChange);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        disconnectService();
    }

    private void disconnectService() {
        if (binder == null) return;
        binder.mockState.removeObservers(this);
        binder.mockedLocation.removeObservers(this);
        binder = null;
        indicateMockStop();
    }

    private void onMockedStateChange(MockState state) {
        switch (state) {
            case NOT_MOCKED -> indicateMockStop();
            case SERVICE_BOUND -> applyLocation();
            case MOCKED -> {
                showSnackbar(R.string.MainActivity_MockApplied);
            }
            case MOCK_ERROR -> showSnackbar(R.string.MainActivity_MockNotApplied);
        }

    }

    private void showBookmarksDialog() {
        List<Bookmark> bookmarks = BookmarkUtil.getBookmarks(this);
        ArrayAdapter<Bookmark> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bookmarks);

        new AlertDialog.Builder(this)
                .setTitle(R.string.Bookmark_Title)
                .setAdapter(adapter, (dialog, which) -> {
                    Bookmark b = bookmarks.get(which);
                    
                    new AlertDialog.Builder(this)
                            .setTitle(b.name)
                            .setItems(new String[]{"Pindah Lokasi Ke Sini", "Jadikan Pusat Lingkaran"}, (d, choice) -> {
                                if (choice == 0) {
                                    setLatLng(b.lat, b.lng, SourceChange.CHANGE_FROM_MAP);
                                    setMapMarker(b.lat, b.lng);
                                } else {
                                    circleLat = b.lat;
                                    circleLng = b.lng;
                                    saveSettings();
                                    updateCircleOnMap();
                                }
                            })
                            .show();
                })
                .setPositiveButton(R.string.Bookmark_Add, (dialog, which) -> showAddBookmarkDialog())
                .setNegativeButton(R.string.Bookmark_Cancel, null)
                .setNeutralButton(R.string.Bookmark_Delete, (dialog, which) -> {
                    showDeleteBookmarkDialog(bookmarks);
                })
                .show();
    }

    private void updateCircleOnMap() {
        if (webView == null || webView.getUrl() == null) return;
        SharedPreferences sharedPref = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        int radius = sharedPref.getInt("circleRadius", 300);
        webView.loadUrl("javascript:updateCircle(" + circleLat + "," + circleLng + "," + radius + ");");
    }

    private void showAddBookmarkDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.Bookmark_Name);

        new AlertDialog.Builder(this)
                .setTitle(R.string.Bookmark_Add)
                .setView(input)
                .setPositiveButton(R.string.Bookmark_Save, (dialog, which) -> {
                    String name = input.getText().toString();
                    if (!name.trim().isEmpty()) {
                        List<Bookmark> bookmarks = BookmarkUtil.getBookmarks(this);
                        bookmarks.add(new Bookmark(name, lat, lng));
                        BookmarkUtil.saveBookmarks(this, bookmarks);
                    }
                })
                .setNegativeButton(R.string.Bookmark_Cancel, null)
                .show();
    }

    private void showDeleteBookmarkDialog(List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) return;
        String[] names = new String[bookmarks.size()];
        for (int i = 0; i < bookmarks.size(); i++) names[i] = bookmarks.get(i).name;

        new AlertDialog.Builder(this)
                .setTitle(R.string.Bookmark_Delete)
                .setItems(names, (dialog, which) -> {
                    bookmarks.remove(which);
                    BookmarkUtil.saveBookmarks(this, bookmarks);
                    showBookmarksDialog();
                })
                .show();
    }

    private void indicateMockStop() {
        showSnackbar(R.string.MainActivity_MockStopped);
        changeButtonToApply();
    }

    private void onMockedLocationChange(Location location) {
        setMapMarker(location.getLatitude(), location.getLongitude());
    }

    public enum SourceChange {
        NONE, LOAD, CHANGE_FROM_EDITTEXT, CHANGE_FROM_MAP
    }

}
