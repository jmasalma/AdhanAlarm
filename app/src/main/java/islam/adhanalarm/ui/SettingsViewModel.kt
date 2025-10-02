package islam.adhanalarm.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import islam.adhanalarm.handler.LocationHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.security.GeneralSecurityException

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _latitude = MutableStateFlow("0.0")
    val latitude = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow("0.0")
    val longitude = _longitude.asStateFlow()

    private val _calculationMethod = MutableStateFlow(4)
    val calculationMethod = _calculationMethod.asStateFlow()

    private val _timeFormat = MutableStateFlow(0)
    val timeFormat = _timeFormat.asStateFlow()

    private val _notificationFajr = MutableStateFlow(1)
    val notificationFajr = _notificationFajr.asStateFlow()

    private val _notificationSunrise = MutableStateFlow(1)
    val notificationSunrise = _notificationSunrise.asStateFlow()

    private val _notificationDhuhr = MutableStateFlow(1)
    val notificationDhuhr = _notificationDhuhr.asStateFlow()

    private val _notificationAsr = MutableStateFlow(1)
    val notificationAsr = _notificationAsr.asStateFlow()

    private val _notificationMaghrib = MutableStateFlow(1)
    val notificationMaghrib = _notificationMaghrib.asStateFlow()

    private val _notificationIshaa = MutableStateFlow(1)
    val notificationIshaa = _notificationIshaa.asStateFlow()

    private val _altitude = MutableStateFlow("0")
    val altitude = _altitude.asStateFlow()

    private val _pressure = MutableStateFlow("110")
    val pressure = _pressure.asStateFlow()

    private val _temperature = MutableStateFlow("10")
    val temperature = _temperature.asStateFlow()

    private val _roundingType = MutableStateFlow(2)
    val roundingType = _roundingType.asStateFlow()

    private val _offsetMinutes = MutableStateFlow("0")
    val offsetMinutes = _offsetMinutes.asStateFlow()

    private val sharedPreferences: SharedPreferences
    private val locationHandler: LocationHandler
    private val locationObserver: Observer<Location>

    init {
        sharedPreferences = try {
            val masterKey = MasterKey.Builder(application, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                application,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Failed to create encrypted shared preferences", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to create encrypted shared preferences", e)
        }

        locationHandler = LocationHandler(application.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        locationObserver = Observer { location ->
            location?.let {
                updateLatitude(it.latitude.toString())
                updateLongitude(it.longitude.toString())
            }
        }
        locationHandler.getLocation().observeForever(locationObserver)

        loadSettings()
    }

    private fun loadSettings() {
        _latitude.value = sharedPreferences.getString("latitude", "43.67") ?: "43.67"
        _longitude.value = sharedPreferences.getString("longitude", "-79.417") ?: "-79.417"
        _calculationMethod.value = sharedPreferences.getString("calculationMethodsIndex", "4")?.toInt() ?: 4
        _timeFormat.value = sharedPreferences.getString("timeFormatIndex", "0")?.toInt() ?: 0
        _notificationFajr.value = sharedPreferences.getString("notificationMethod0", "1")?.toInt() ?: 1
        _notificationSunrise.value = sharedPreferences.getString("notificationMethod1", "1")?.toInt() ?: 1
        _notificationDhuhr.value = sharedPreferences.getString("notificationMethod2", "1")?.toInt() ?: 1
        _notificationAsr.value = sharedPreferences.getString("notificationMethod3", "1")?.toInt() ?: 1
        _notificationMaghrib.value = sharedPreferences.getString("notificationMethod4", "1")?.toInt() ?: 1
        _notificationIshaa.value = sharedPreferences.getString("notificationMethod5", "1")?.toInt() ?: 1
        _altitude.value = sharedPreferences.getString("altitude", "0") ?: "0"
        _pressure.value = sharedPreferences.getString("pressure", "110") ?: "110"
        _temperature.value = sharedPreferences.getString("temperature", "10") ?: "10"
        _roundingType.value = sharedPreferences.getString("roundingTypesIndex", "2")?.toInt() ?: 2
        _offsetMinutes.value = sharedPreferences.getString("offsetMinutes", "0") ?: "0"
    }

    fun updateLatitude(value: String) {
        _latitude.value = value
        sharedPreferences.edit().putString("latitude", value).apply()
    }

    fun updateLongitude(value: String) {
        _longitude.value = value
        sharedPreferences.edit().putString("longitude", value).apply()
    }

    fun updateCalculationMethod(value: Int) {
        _calculationMethod.value = value
        sharedPreferences.edit().putString("calculationMethodsIndex", value.toString()).apply()
    }

    fun updateTimeFormat(value: Int) {
        _timeFormat.value = value
        sharedPreferences.edit().putString("timeFormatIndex", value.toString()).apply()
    }

    fun updateNotificationFajr(value: Int) {
        _notificationFajr.value = value
        sharedPreferences.edit().putString("notificationMethod0", value.toString()).apply()
    }

    fun updateNotificationSunrise(value: Int) {
        _notificationSunrise.value = value
        sharedPreferences.edit().putString("notificationMethod1", value.toString()).apply()
    }

    fun updateNotificationDhuhr(value: Int) {
        _notificationDhuhr.value = value
        sharedPreferences.edit().putString("notificationMethod2", value.toString()).apply()
    }

    fun updateNotificationAsr(value: Int) {
        _notificationAsr.value = value
        sharedPreferences.edit().putString("notificationMethod3", value.toString()).apply()
    }

    fun updateNotificationMaghrib(value: Int) {
        _notificationMaghrib.value = value
        sharedPreferences.edit().putString("notificationMethod4", value.toString()).apply()
    }

    fun updateNotificationIshaa(value: Int) {
        _notificationIshaa.value = value
        sharedPreferences.edit().putString("notificationMethod5", value.toString()).apply()
    }

    fun updateAltitude(value: String) {
        _altitude.value = value
        sharedPreferences.edit().putString("altitude", value).apply()
    }

    fun updatePressure(value: String) {
        _pressure.value = value
        sharedPreferences.edit().putString("pressure", value).apply()
    }

    fun updateTemperature(value: String) {
        _temperature.value = value
        sharedPreferences.edit().putString("temperature", value).apply()
    }

    fun updateRoundingType(value: Int) {
        _roundingType.value = value
        sharedPreferences.edit().putString("roundingTypesIndex", value.toString()).apply()
    }

    fun updateOffsetMinutes(value: String) {
        _offsetMinutes.value = value
        sharedPreferences.edit().putString("offsetMinutes", value.toString()).apply()
    }

    fun lookupGps() {
        locationHandler.update()
    }

    override fun onCleared() {
        super.onCleared()
        locationHandler.getLocation().removeObserver(locationObserver)
    }
}