package com.kavya.zes;

import static com.kavya.zes.MainActivity.DECIMAL_FORMAT;
import static com.kavya.zes.MainActivity.sharedPrefKey;
import static com.kavya.zes.SharedPrefsUtil.getDouble;
import static com.kavya.zes.SharedPrefsUtil.putDouble;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.more_layout), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);

        TextView tvLeafletLicense = findViewById(R.id.tv_LeafletLicense);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvLeafletLicense.setText(Html.fromHtml(getString(R.string.ActivityMore_LeafletLicense),
                    Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvLeafletLicense.setText(Html.fromHtml(getString(R.string.ActivityMore_LeafletLicense)));
        }
        tvLeafletLicense.setMovementMethod(LinkMovementMethod.getInstance());

        EditText etDMockLat = findViewById(R.id.et_DMockLat);
        etDMockLat.setText(DECIMAL_FORMAT.format(getDouble(sharedPref, "dLat", 0)));
        etDMockLat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etDMockLat.getText().toString().trim().isEmpty()) {
                    putDouble(editor, "dLat", 0);
                } else {
                    try {
                        putDouble(editor, "dLat", Double.parseDouble(etDMockLat.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse dLat!", t);
                    }
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

        EditText etDMockLon = findViewById(R.id.et_DMockLon);
        etDMockLon.setText(DECIMAL_FORMAT.format(getDouble(sharedPref, "dLng", 0)));
        etDMockLon.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etDMockLon.getText().toString().trim().isEmpty()) {
                    putDouble(editor, "dLng", 0);
                } else {
                    try {
                        putDouble(editor, "dLng", Double.parseDouble(etDMockLon.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse dLng!", t);
                    }
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

        EditText etMockCount = findViewById(R.id.et_MockCount);
        
        EditText etCircleRadius = findViewById(R.id.et_CircleRadius);
        etCircleRadius.setText(String.format(Locale.ROOT, "%d", sharedPref.getInt("circleRadius", 300)));
        etCircleRadius.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences.Editor editor = getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE).edit();
                if (etCircleRadius.getText().toString().trim().isEmpty()) {
                    editor.putInt("circleRadius", 300);
                } else {
                    try {
                        editor.putInt("circleRadius", Integer.parseInt(etCircleRadius.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse circleRadius!", t);
                    }
                }
                editor.apply();
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        etMockCount.setText(String.format(Locale.ROOT, "%d", sharedPref.getInt("mockCount", 0)));
        etMockCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etMockCount.getText().toString().trim().isEmpty()) {
                    editor.putInt("mockCount", 0);
                } else {
                    try {
                        editor.putInt("mockCount", Integer.parseInt(etMockCount.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse mockCount!", t);
                    }
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

        EditText etMockFrequency = findViewById(R.id.et_MockFrequency);
        etMockFrequency.setText(String.format(Locale.ROOT, "%d", sharedPref.getInt("mockFrequency", 10)));
        etMockFrequency.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etMockFrequency.getText().toString().trim().isEmpty()) {
                    editor.putInt("mockFrequency", 10);
                } else {
                    try {
                        editor.putInt("mockFrequency", Integer.parseInt(etMockFrequency.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse mockFrequency!", t);
                    }
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        CheckBox mockSpeed = findViewById(R.id.cb_MockSpeed);
        mockSpeed.setChecked(sharedPref.getBoolean("mockSpeed", true));
        mockSpeed.setOnCheckedChangeListener((compoundButton, b) -> {
            Context context1 = getApplicationContext();
            SharedPreferences sharedPref1 = context1.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref1.edit();

            editor.putBoolean("mockSpeed", mockSpeed.isChecked());

            editor.apply();
        });


        EditText etMapProvider = findViewById(R.id.et_MapProvider);
        
        AutoCompleteTextView spinnerDecimalPrecision = findViewById(R.id.spinner_DecimalPrecision);
        String[] precisionOptions = new String[]{
                "6 (Contoh: -7.123456, 110.123456)",
                "7 (Contoh: -7.1234567, 110.1234567)",
                "8 (Contoh: -7.12345678, 110.12345678)",
                "9 (Contoh: -7.123456789, 110.123456789)",
                "10 (Contoh: -7.1234567890, 110.1234567890)"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, precisionOptions);
        spinnerDecimalPrecision.setAdapter(adapter);

        int currentPrecision = sharedPref.getInt("decimalPrecision", 7);
        int selectionIndex = currentPrecision - 6;
        if (selectionIndex >= 0 && selectionIndex < precisionOptions.length) {
            spinnerDecimalPrecision.setText(precisionOptions[selectionIndex], false);
        }

        spinnerDecimalPrecision.setOnItemClickListener((parent, view, position, id) -> {
            int newPrecision = position + 6;
            sharedPref.edit().putInt("decimalPrecision", newPrecision).apply();
        });

        etMapProvider.setText(sharedPref.getString("mapProvider",
                MapProviderUtil.getDefaultMapProvider(Locale.getDefault())));
        etMapProvider.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                if (etMapProvider.getText().toString().trim().isEmpty()) {
                    editor.putString("mapProvider", MapProviderUtil.getDefaultMapProvider(Locale.getDefault()));
                } else {
                    editor.putString("mapProvider", etMapProvider.getText().toString());
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

}
