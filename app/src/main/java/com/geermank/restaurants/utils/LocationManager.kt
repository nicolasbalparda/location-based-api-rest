package com.geermank.restaurants.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task

class LocationManager(
        private var context: Context
) {

    interface LocationCallback{
        fun onClientSettingsError(ex: Exception)
        fun onLocationPermissionsNeeded()
        fun onLocationObtained(location: Location)
    }

    private var listener: LocationCallback

    init {
        try{
            listener = context as LocationCallback
        }catch (ex: ClassCastException){
            throw ClassCastException("Activity must implements this interface")
        }
    }

    fun checkLocationSettings(){

        val locationRequest = getLocationRequest()

        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            verifyLocationPermissions()
        }.addOnFailureListener{ ex ->
            listener.onClientSettingsError(ex)
        }
    }

    fun verifyLocationPermissions(){
        if (locationPermissionGranted()){
            getLastKnownLocation()
        }else{
            listener.onLocationPermissionsNeeded()
        }
    }

    fun getLastKnownLocation() {

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            if (location == null){
                // Last location is not know because it may be the first time the device
                // requests its location (new device, restored from factory)
                getLocation()
                return@addOnSuccessListener
            }

            listener.onLocationObtained(location)
        }
    }

    private fun getLocation(){

    }

    private fun locationPermissionGranted(): Boolean =
            ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun getLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 10000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

}