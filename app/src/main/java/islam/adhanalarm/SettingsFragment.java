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

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

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
    SharedPreferences mSharedPreferences;
    LocationHandler mLocationHandler;
    private Observer<Location> mLocationObserver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mLocationHandler = new LocationHandler((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE));
        mLocationObserver = new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location currentLocation) {
                if (currentLocation == null) return;
                mSharedPreferences.edit()
                        .putString("latitude", Double.toString(currentLocation.getLatitude()))
                        .putString("longitude", Double.toString(currentLocation.getLongitude()))
                        .apply();
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
        mSharedPreferences = getPreferenceManager().getSharedPreferences();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        updateSummaries();
    }

    private void updateSummaries() {
        Map<String, ?> preferencesMap = mSharedPreferences.getAll();
        for (Map.Entry<String, ?> preferenceEntry : preferencesMap.entrySet()) {
            if (TEXT_ENTRIES.contains(preferenceEntry.getKey())) {
                updateSummary((EditTextPreference) findPreference(preferenceEntry.getKey()));
            }
        }
    }

    @Override
    public void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (sharedPreferences.contains(key) && TEXT_ENTRIES.contains(key)) {
            updateSummary((EditTextPreference) findPreference(key));
        }
    }

    private void updateSummary(EditTextPreference preference) {
        preference.setSummary(preference.getText());
    }
}