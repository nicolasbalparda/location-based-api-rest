package com.geermank.restaurants.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.geermank.restaurants.R
import com.geermank.restaurants.utils.Constants
import com.geermank.restaurants.viewmodel.BaseViewModelFactory
import com.geermank.restaurants.viewmodel.MainViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {

    companion object{
        // Requests code for settings verification and location permission
        private const val RC_CHECK_SETTINGS = 111
        private const val RC_LOCATION_PERMISSION = 112
    }

    private val viewModel: MainViewModel by lazy {

        ViewModelProviders.of(this,
            BaseViewModelFactory{ MainViewModel(getSharedPreferences(Constants.APP_PREFS, Context.MODE_PRIVATE)) })
            .get(MainViewModel::class.java)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Renews token every time the user launches the app
        // An optimization would be to request it only when we detect
        // that our locally saved token has expired
        renewToken()
    }

    private fun renewToken(){
        viewModel.renewToken().observe(this, Observer { response ->

            if (response.error) {
                showError(response.message,response.code)
                return@Observer
            }

            verifyLocationSettings()

        })
    }

    private fun showError(message: String, code: Int){

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.main_activity_fetch_token_error_title)
            .setMessage("$message $code")
            .setPositiveButton(R.string.main_activity_fetch_token_error_retry){ dialog, _ ->
                dialog.dismiss()

                // On token request error, we have to retry the petition
                // since we can't make any other call without it
                renewToken()
            }
            // Don't allow user to dismiss this dialog unless he tries to
            // get token again
            .setCancelable(false)

        dialog.show()
    }

    /**
     * Verifies if current device settings satisfies our needs
     *
     * Punctually, it verifies whether the GPS is active or not
     */
    private fun verifyLocationSettings(){

        val locationRequest = getLocationRequest()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {

            if (locationPermissionGranted()){
                getLastKnownLocation()
            }else{
                requestLocationPermissions()
            }

        }.addOnFailureListener{ ex ->

            if (ex is ResolvableApiException){
                ex.startResolutionForResult(this, RC_CHECK_SETTINGS)
            }

        }
    }

    private fun getLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 10000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }


    private fun locationPermissionGranted(): Boolean =
        ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermissions() =
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            RC_LOCATION_PERMISSION)


    private fun getLastKnownLocation() {

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            if (location == null){
                // Last location is not know because it may be the first time the device
                // requests its location (new device, restored from factory)
                getLocation()
                return@addOnSuccessListener
            }

            viewModel.storeLocation(location.latitude,location.longitude)
            Toast.makeText(this,"Success",Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLocation(){

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_CHECK_SETTINGS){

            if(resultCode == Activity.RESULT_OK){
                getLastKnownLocation()
            }else{
                verifyLocationSettings()
            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (requestCode == RC_LOCATION_PERMISSION){

            if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getLastKnownLocation()
            }else{
                // Can't continue without location
                requestLocationPermissions()
            }

        }

    }

}
