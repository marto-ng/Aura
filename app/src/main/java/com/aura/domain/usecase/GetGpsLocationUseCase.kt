package com.aura.domain.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.aura.domain.model.LocationDomainModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class GetGpsLocationUseCase(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend operator fun invoke(): LocationDomainModel? = withContext(Dispatchers.IO) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        val deviceLocation = suspendCancellableCoroutine<Location?> { continuation ->
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc: Location? ->
                    if (continuation.isActive) continuation.resume(loc)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        } ?: tryFallbackLocationManager()

        if (deviceLocation == null) return@withContext null

        val lat = deviceLocation.latitude
        val lon = deviceLocation.longitude

        var cityName = "Ubicación Actual"
        var countryName = ""
        var adminArea: String? = null

        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Ubicación Actual"
                countryName = addr.countryName ?: ""
                adminArea = addr.adminArea
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        LocationDomainModel(
            name = cityName,
            country = countryName,
            admin1 = adminArea,
            latitude = lat,
            longitude = lon
        )
    }

    private fun tryFallbackLocationManager(): Location? {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val providers = lm.getProviders(true)
            var bestLoc: Location? = null
            for (p in providers) {
                @SuppressLint("MissingPermission")
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (bestLoc == null || loc.accuracy < bestLoc.accuracy) {
                    bestLoc = loc
                }
            }
            return bestLoc
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
