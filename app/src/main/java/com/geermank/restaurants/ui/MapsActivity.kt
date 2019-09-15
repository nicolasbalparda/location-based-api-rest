package com.geermank.restaurants.ui

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.geermank.restaurants.R
import com.geermank.restaurants.repository.models.Restaurant
import com.geermank.restaurants.utils.Constants
import com.geermank.restaurants.utils.MoshiUtils

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var userMarker: Marker? = null

    private lateinit var restaurants: List<Restaurant?>

    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    private var allowUserToChangeLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        initRestaurants()
        initUserLocation()

        allowUserToChangeLocation = !::restaurants.isInitialized
        showChangeLocationCard()

        toolbarSetUp()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btn_confirm_location.setOnClickListener {
           val resultIntent = Intent()
            resultIntent.putExtra(Constants.EXTRA_USER_LATITUDE,userLatitude)
            resultIntent.putExtra(Constants.EXTRA_USER_LONGITUDE,userLongitude)

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

    }

    private fun toolbarSetUp(){
        setSupportActionBar(toolbar)

        val title = if (allowUserToChangeLocation) {
            getString(R.string.maps_activity_title_confirm_exact_location)
        } else {
            getString(R.string.maps_activity_title_nearby_restaurants)
        }
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    private fun initRestaurants(){
        if (intent.extras!!.containsKey(Constants.EXTRA_RESTAURANTS)){
            val serializedRestaurants = intent.getStringExtra(Constants.EXTRA_RESTAURANTS)
            restaurants = deserializeRestaurants(serializedRestaurants)
        }
    }

    private fun initUserLocation(){
        userLatitude = intent.getDoubleExtra(Constants.EXTRA_USER_LATITUDE,0.0)
        userLongitude = intent.getDoubleExtra(Constants.EXTRA_USER_LONGITUDE,0.0)
    }

    private fun deserializeRestaurants(restaurants: String): List<Restaurant?>{
        val adapter = MoshiUtils.restaurantsAdapter()
        return adapter.fromJson(restaurants)!!
    }

    private fun showChangeLocationCard(){
        if (allowUserToChangeLocation){
            card_confirm_location.visibility = View.VISIBLE
        }else{
            card_confirm_location.visibility = View.GONE
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.

     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        addRestaurantMarkers()
        val userLocation = addUserPositionMarker()

        val zoomValue = if (allowUserToChangeLocation){
            18.0f
        }else{
            14.0f
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation,zoomValue))

        setMapMoveListener()
    }

    private fun addRestaurantMarkers(){

        if (!::restaurants.isInitialized){
            return
        }

        restaurants.forEach{ restaurant ->

            val coordinates = getCoordinates(restaurant!!.coordinates)
            val point = LatLng(coordinates[0],coordinates[1])

            mMap.addMarker(MarkerOptions().position(point).title(restaurant.name))
        }

    }

    private fun addUserPositionMarker(): LatLng{

        val userLocation = LatLng(userLatitude,userLongitude)

        if (!allowUserToChangeLocation){
            userMarker = mMap.addMarker(
                MarkerOptions().position(userLocation)
                    .title(getString(R.string.your_location))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        }

        return userLocation

    }

    private fun setMapMoveListener(){

        if (!allowUserToChangeLocation) return

        iv_selected_location.visibility = View.VISIBLE

        mMap.setOnCameraMoveListener {
            userMarker?.remove()
        }

        mMap.setOnCameraIdleListener {
            userLatitude = mMap.cameraPosition.target.latitude
            userLongitude = mMap.cameraPosition.target.longitude

        }

    }

    private fun getCoordinates(coordinates: String): List<Double>{
        val stringCoordinates = coordinates.split(",")
        return listOf(stringCoordinates[0].toDouble(),stringCoordinates[1].toDouble())
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        if (item?.itemId == android.R.id.home){
            finish()
        }

        return super.onOptionsItemSelected(item)
    }

}
