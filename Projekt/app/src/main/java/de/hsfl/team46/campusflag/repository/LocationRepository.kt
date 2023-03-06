package de.hsfl.team46.campusflag.repository

import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData


class LocationRepository : LocationListener {
    companion object {
        private const val TAG = "LocationListener"

        private const val MIN_TIME_BW_UPDATES = 1000
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 0
    }

    private var currentLocation = MutableLiveData<Location>()
    fun getCurrentLocation(): MutableLiveData<Location> = currentLocation

    fun requestCurrentLocation(mContext: Context) {
        var loc: Location? = null

        val locationManager = mContext.getSystemService(LOCATION_SERVICE) as LocationManager

        // getting GPS status
        val checkGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        // getting network status
        val checkNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!checkGPS && !checkNetwork) {
            Toast.makeText(mContext, "No Service Provider Available", Toast.LENGTH_SHORT).show()
        } else {
            if (checkNetwork) {
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES.toLong(),
                        MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                    )

                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } catch (e: SecurityException) {
                    Toast.makeText(mContext, e.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // if GPS Enabled get lat/long using GPS Services
        if (checkGPS) {
            if (loc == null) {
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES.toLong(),
                        MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                    )

                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } catch (e: SecurityException) {
                    Toast.makeText(mContext, e.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (loc != null) {
            if (currentLocation.value != loc) {
                currentLocation.value = loc
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        if (currentLocation.value?.latitude != location.latitude && currentLocation.value?.longitude != location.longitude) {
            Log.d(TAG, "Current Location: $location")
            currentLocation.value = location
        }
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }
}