package islam.adhanalarm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import islam.adhanalarm.handler.LocationHandler;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    final Set<String> TEXT_ENTRIES = new HashSet<>(Arrays.asList(
            "latitude",
            "longitude",
            "altitude",
            "pressure",
            "temperature",
            "offsetMinutes"
    ));

    final Set<String> WIDGET_UPDATE_KEYS = new HashSet<>(Arrays.asList(
            "latitude",
            "longitude",
            "altitude",
            "pressure",
            "temperature",
            "offsetMinutes",
            "calculationMethod",
            "rounding",
            "timeFormat"
    ));
    private SharedPreferences mEncryptedSharedPreferences;
    private LocationHandler mLocationHandler;
    private Observer<Location> mLocationObserver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        try {
            MasterKey masterKey = new MasterKey.Builder(getActivity(), MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            mEncryptedSharedPreferences = EncryptedSharedPreferences.create(
                    getActivity(),
                    "secret_shared_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e("SettingsFragment", "Failed to create encrypted shared preferences", e);
            getActivity().finish(); // Can't work without preferences
            return;
        }

        mLocationHandler = new LocationHandler((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE));
        mLocationObserver = new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location currentLocation) {
                if (currentLocation == null) return;

                final String latString = Double.toString(currentLocation.getLatitude());
                final String lonString = Double.toString(currentLocation.getLongitude());

                SharedPreferences.Editor editor = mEncryptedSharedPreferences.edit();
                editor.putString("latitude", latString);
                editor.putString("longitude", lonString);
                editor.apply();

                // Also update the UI preferences
                SharedPreferences uiPrefs = getPreferenceManager().getSharedPreferences();
                SharedPreferences.Editor uiEditor = uiPrefs.edit();
                uiEditor.putString("latitude", latString);
                uiEditor.putString("longitude", lonString);
                uiEditor.apply();

                // Manually update the preference summaries
                EditTextPreference latitudePref = (EditTextPreference) findPreference("latitude");
                latitudePref.setText(latString);
                updateSummary(latitudePref);

                EditTextPreference longitudePref = (EditTextPreference) findPreference("longitude");
                longitudePref.setText(lonString);
                updateSummary(longitudePref);
            }
        };
        mLocationHandler.getLocation().observeForever(mLocationObserver);


        findPreference("lookupGPS").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                mLocationHandler.update();
                return true;
            }
        });

        findPreference("information").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.spiritofislam.com"));
                startActivity(browserIntent);
                return true;
            }
        });

        try {
            String versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            findPreference("information").setSummary(getText(R.string.information_text).toString().replace("#", versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationHandler != null && mLocationObserver != null) {
            mLocationHandler.getLocation().removeObserver(mLocationObserver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register listener on the UI (default) preferences
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        // Sync from encrypted to UI preferences
        syncEncryptedToUi();

        updateSummaries();
    }

    private void syncEncryptedToUi() {
        SharedPreferences uiPrefs = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor uiEditor = uiPrefs.edit();
        for (String key : TEXT_ENTRIES) {
            String value = mEncryptedSharedPreferences.getString(key, null);
            if (value != null) {
                uiEditor.putString(key, value);
            }
        }
        uiEditor.apply();
    }

    private void updateSummaries() {
        // Summaries are based on the UI preferences
        Map<String, ?> preferencesMap = getPreferenceManager().getSharedPreferences().getAll();
        for (Map.Entry<String, ?> preferenceEntry : preferencesMap.entrySet()) {
            if (TEXT_ENTRIES.contains(preferenceEntry.getKey())) {
                Preference pref = findPreference(preferenceEntry.getKey());
                if (pref instanceof EditTextPreference) {
                    updateSummary((EditTextPreference) pref);
                }
            }
        }
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // This is called when UI (default) preferences change

        // First, update the summary if it's a text-based preference
        if (TEXT_ENTRIES.contains(key)) {
            Preference pref = findPreference(key);
            if (pref instanceof EditTextPreference) {
                updateSummary((EditTextPreference) pref);
            }
        }

        // If the key is one that affects prayer time calculations, sync it and update widgets
        if (WIDGET_UPDATE_KEYS.contains(key)) {
            // Sync the change to encrypted preferences
            SharedPreferences.Editor encryptedEditor = mEncryptedSharedPreferences.edit();
            encryptedEditor.putString(key, sharedPreferences.getString(key, ""));
            encryptedEditor.apply();

            // Send a broadcast to update any active widgets
            Intent intent = new Intent(CONSTANT.ACTION_UPDATE_PRAYER_TIME);
            intent.setPackage(getActivity().getPackageName());
            getActivity().sendBroadcast(intent);
        }
    }

    private void updateSummary(EditTextPreference preference) {
        if (preference != null) {
            preference.setSummary(preference.getText());
        }
    }
}