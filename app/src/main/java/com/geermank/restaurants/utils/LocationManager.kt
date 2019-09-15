package com.geermank.restaurants.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

/**
 * Auxiliary class to handle device location. Will be used in order to
 * get coordinates (latitude and longitude), with previous settings and
 * GPS verification
 */
class LocationManager(
        private var context: Context?
) {

    /**
     * Callback to be invoked whether we successfully
     * obtained user location, or we have an error doing it
     *
     * Errors can come up because user does not granted location permission
     * or GPS is not enabled
     */
    interface OnLocationManagerListener{
        fun onClientSettingsError(ex: Exception)
        fun onLocationPermissionsNeeded()
        fun onLocationObtained(location: Location?)
    }

    /**
     * This instance is initialized by casting context received
     * in class constructor. Activity must implement this interface in order
     * to use LocationManager
     */
    private var listener: OnLocationManagerListener?

    init {
        try{
            listener = context as OnLocationManagerListener
        }catch (ex: ClassCastException){
            throw ClassCastException("Activity must implement this interface")
        }
    }

    /**
     * Used to verify user device location settings and to request location update
     * if needed, only when last location known is null
     */
    private var locationRequest = LocationRequest.create().apply {
        interval = 10000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * Not always used. It will be needed if last known location is null.
     * When new location is got, it will be delivered to this callback.
     *
     * Last known location will be null in some rare cases, like in a new device or
     * device restored from factory
     */
    private val locationCallback by lazy {
        createLocationCallback()
    }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)

    fun checkLocationSettings(){

        context ?: return

        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(context!!)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            verifyLocationPermissions()
        }.addOnFailureListener{ ex ->
            listener?.onClientSettingsError(ex)
        }
    }

    fun verifyLocationPermissions(){
        if (locationPermissionGranted()){
            getLastKnownLocation()
        }else{
            listener?.onLocationPermissionsNeeded()
        }
    }

    fun getLastKnownLocation() {

        context ?: return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            if (location == null){
                // Last location is not know because it may be the first time the device
                // requests its location (new device, restored from factory)
                // When location is obtained, it will be received in location callback
                fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())
                return@addOnSuccessListener
            }

            listener?.onLocationObtained(location)
        }
    }

    /**
     * On Activity destroy, forget its context and interface reference
     */
    fun destroy(){
        this.context = null
        this.listener = null
    }

    private fun createLocationCallback(): LocationCallback{

        return object : LocationCallback() {

            override fun onLocationResult(result: LocationResult?) {
                super.onLocationResult(result)

                listener?.onLocationObtained(result?.lastLocation)

                result.let {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
        }
    }

    private fun locationPermissionGranted(): Boolean =
            ActivityCompat.checkSelfPermission(context!!,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

}