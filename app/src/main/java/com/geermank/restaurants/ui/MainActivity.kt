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
import com.geermank.restaurants.utils.LocationManager
import com.geermank.restaurants.viewmodel.BaseViewModelFactory
import com.geermank.restaurants.viewmodel.MainViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity(), LocationManager.LocationCallback {

    private lateinit var viewModel: MainViewModel

    private val locationManager: LocationManager by lazy { initLocationManager() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViewModel()

        // Renews token every time the user launches the app
        // An optimization would be to request it only when we detect
        // that our locally saved token has expired
        renewToken()
    }

    private fun initViewModel(){
        viewModel = ViewModelProviders.of(this,
                BaseViewModelFactory{ MainViewModel(getSharedPreferences(Constants.APP_PREFS, Context.MODE_PRIVATE)) })
                .get(MainViewModel::class.java)
    }

    private fun initLocationManager(): LocationManager = LocationManager(this)

    private fun renewToken(){
        viewModel.renewToken().observe(this, Observer { response ->

            if (response.exception != null){
                showError(response.localizedMessage!!)
                return@Observer
            }

            locationManager.checkLocationSettings()
        })
    }

    private fun showError(message: Int){

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.main_activity_fetch_token_error_title)
            .setMessage(message)
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

    override fun onClientSettingsError(ex: Exception) {
        if (ex is ResolvableApiException){
            ex.startResolutionForResult(this, Constants.RC_CHECK_SETTINGS)
        }
    }

    override fun onLocationPermissionsNeeded() {
        requestLocationPermissions()
    }

    override fun onLocationObtained(location: Location) {

    }

    private fun requestLocationPermissions() =
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            Constants.RC_LOCATION_PERMISSION)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Constants.RC_CHECK_SETTINGS){
            if (resultCode == Activity.RESULT_OK){
                locationManager.verifyLocationPermissions()
            }else{
                // Can't continue without GPS ON
                locationManager.checkLocationSettings()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (requestCode == Constants.RC_LOCATION_PERMISSION){

            if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                locationManager.getLastKnownLocation()
            }else{
                // Can't continue without location
                requestLocationPermissions()
            }

        }

    }

}
