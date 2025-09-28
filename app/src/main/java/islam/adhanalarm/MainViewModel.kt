package islam.adhanalarm

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.preference.PreferenceManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import islam.adhanalarm.handler.CompassHandler
import islam.adhanalarm.handler.LocationHandler
import islam.adhanalarm.handler.ScheduleData
import islam.adhanalarm.handler.ScheduleHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sourceforge.jitl.Jitl
import net.sourceforge.jitl.astro.Direction

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val compassHandler: CompassHandler
    private val locationHandler: LocationHandler
    private val settings = PreferenceManager.getDefaultSharedPreferences(application)

    private val _scheduleData = MediatorLiveData<ScheduleData>()
    val scheduleData: LiveData<ScheduleData> = _scheduleData

    private val _qiblaDirection = MediatorLiveData<Double>()
    val qiblaDirection: LiveData<Double> = _qiblaDirection

    val northDirection: LiveData<Float>
    val location: LiveData<Location>

    init {
        compassHandler = CompassHandler(application.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
        locationHandler = LocationHandler(application.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        northDirection = compassHandler.northDirection
        location = locationHandler.location

        _scheduleData.addSource(location) { updateData() }
        _qiblaDirection.addSource(location) { updateData() }
    }

    fun startCompass() {
        compassHandler.startTracking()
    }

    fun stopCompass() {
        compassHandler.stopTracking()
    }

    fun updateLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            locationHandler.update()
        }
    }

    fun updateData() {
        location.value?.let { loc ->
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    val latitude = loc.latitude.toString()
                    val longitude = loc.longitude.toString()
                    val altitude = settings.getString("altitude", "0")
                    val pressure = settings.getString("pressure", "1010")
                    val temperature = settings.getString("temperature", "10")

                    val locationAstro = ScheduleHandler.getLocation(latitude, longitude, altitude, pressure, temperature)

                    // Calculate and post schedule
                    var calculationMethodIndex = settings.getString("calculationMethodsIndex", null)
                    if (calculationMethodIndex == null) {
                        val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.getDefault())
                        try {
                            val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                            val countryCode = addresses?.firstOrNull()?.countryCode
                            if (countryCode != null) {
                                val locale = java.util.Locale("", countryCode)
                                val countryCodeAlpha3 = locale.isO3Country.toUpperCase(java.util.Locale.ROOT)
                                for ((index, codes) in CONSTANT.CALCULATION_METHOD_COUNTRY_CODES.withIndex()) {
                                    if (codes.contains(countryCodeAlpha3)) {
                                        calculationMethodIndex = index.toString()
                                        break
                                    }
                                }
                            }
                        } catch (e: java.io.IOException) {
                            // Ignore
                        }
                        if (calculationMethodIndex == null) {
                            calculationMethodIndex = CONSTANT.DEFAULT_CALCULATION_METHOD.toString()
                        }
                        settings.edit().putString("calculationMethodsIndex", calculationMethodIndex).apply()
                    }
                    val roundingTypeIndex = settings.getString("roundingTypesIndex", CONSTANT.DEFAULT_ROUNDING_TYPE.toString())
                    val offsetMinutes = settings.getString("offsetMinutes", "0")?.toInt() ?: 0
                    val newScheduleData = ScheduleHandler.calculate(locationAstro, calculationMethodIndex, roundingTypeIndex, offsetMinutes)
                    _scheduleData.postValue(newScheduleData)

                    // Calculate and post qibla direction
                    val qibla = Jitl.getNorthQibla(locationAstro)
                    _qiblaDirection.postValue(qibla.getDecimalValue(Direction.NORTH))
                }
            }
        }
    }
}