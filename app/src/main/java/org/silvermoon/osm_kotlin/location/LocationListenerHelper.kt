package org.silvermoon.osm_kotlin.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import java.util.*

class LocationListenerHelper(protected var mContext: Context) {
    private var mLocationManager: LocationManager? = null
    private var mMyLocationListener: IMyLocationListener? = null
    private var mGotGpsLocation = false
    var heading: Int? = null
        private set
    private var mHeadingMagneticVariation = 0.0
    private var mLastLocation: Location? = null
    @JvmOverloads
    fun startListeningLocation(
        myLocationListener: IMyLocationListener? = null,
        minTime: Int = 4000,
        minDistance: Int = 0
    ) {
        mMyLocationListener = myLocationListener
        mLocationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = mLocationManager!!.getProviders(true)
        for (provider in providers) {
            try {
                if (ActivityCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                if (LocationManager.GPS_PROVIDER == provider) mLocationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    minTime.toLong(),
                    minDistance.toFloat(),
                    locationListenerGps
                ) else if (LocationManager.NETWORK_PROVIDER == provider) mLocationManager!!.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    minTime.toLong(),
                    minDistance.toFloat(),
                    locationListenerNetwork
                ) else if (LocationManager.PASSIVE_PROVIDER == provider) mLocationManager!!.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER,
                    minTime.toLong(),
                    minDistance.toFloat(),
                    locationListenerPassive
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mGotGpsLocation = false
    }

    fun stopListeningLocation() {
        mLocationManager!!.removeUpdates(locationListenerGps)
        mLocationManager!!.removeUpdates(locationListenerNetwork)
        mLocationManager!!.removeUpdates(locationListenerPassive)
        mMyLocationListener = null
        mGotGpsLocation = false
    }

    fun onNewLocation(loc: Location) {
        mLastLocation = loc
        setMagneticVariation(loc)
        if (mMyLocationListener != null) mMyLocationListener!!.onNewLocation(loc)
    }

    val lastKnownLocation: Location?
        get() {
            if (mLastLocation != null) return mLastLocation
            try {
                if (mLocationManager == null) mLocationManager =
                    mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = mLocationManager!!.getProviders(true)
                val locations = TreeMap<Long, Location>(Collections.reverseOrder())
                for (provider in providers) {
                    val l = if (ActivityCompat.checkSelfPermission(
                            mContext,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            mContext,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        null
                    }
                    else {
                        mLocationManager!!.getLastKnownLocation(provider!!)
                    }
                    if (l != null) locations[l.time] = l
                }
                for ((_, value) in locations) {
                    return value
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    var locationListenerGps: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (mLocationManager != null && mGotGpsLocation == false) {
                mGotGpsLocation = true
                mLocationManager!!.removeUpdates(locationListenerNetwork)
                mLocationManager!!.removeUpdates(locationListenerPassive)
            }
            onNewLocation(location)
        }

        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    }
    var locationListenerNetwork: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (mLocationManager != null && mGotGpsLocation) {
                mLocationManager!!.removeUpdates(this)
                return
            }
            onNewLocation(location)
        }

        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    }
    var locationListenerPassive: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (mLocationManager != null && mGotGpsLocation) {
                mLocationManager!!.removeUpdates(this)
                return
            }
            onNewLocation(location)
        }

        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    }

    fun setHeading(magneticHeading: Float): Boolean {
        val heading = magneticHeading + mHeadingMagneticVariation
        val headingRounded = (heading / 20).toInt() * 20 // round heading
        if (this.heading != null && headingRounded == heading.toInt()) return false
        this.heading = headingRounded
        return true
    }

    private fun setMagneticVariation(location: Location) {
        var timestamp = location.time
        if (timestamp == 0L) {
            // Hack for Samsung phones which don't populate the time field
            timestamp = System.currentTimeMillis()
        }
        val field = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(), timestamp
        )
        mHeadingMagneticVariation = field.declination.toDouble()
    }

    interface IMyLocationListener {
        fun onNewLocation(location: Location?)
    }

    companion object {
        fun isGPSProvidersAvailable(c: Context): Boolean {
            try {
                val locationManager =
                    c.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                for (gpsDevices in locationManager.getProviders(true)) {
                    if (gpsDevices.equals(
                            LocationManager.GPS_PROVIDER,
                            ignoreCase = true
                        ) || gpsDevices.equals(LocationManager.NETWORK_PROVIDER, ignoreCase = true)
                    ) {
                        return true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }
    }
}