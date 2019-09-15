package com.geermank.restaurants.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.geermank.restaurants.R
import com.geermank.restaurants.repository.models.Restaurant
import com.geermank.restaurants.ui.adapters.RestaurantsAdapter
import com.geermank.restaurants.utils.Constants
import com.geermank.restaurants.utils.LocationManager
import com.geermank.restaurants.viewmodel.BaseViewModelFactory
import com.geermank.restaurants.viewmodel.MainViewModel
import com.google.android.gms.common.api.ResolvableApiException
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), LocationManager.OnLocationManagerListener {

    /**
     * Auxiliary class to handle location related issues
     */
    private lateinit var locationManager: LocationManager

    //region LocationManager Callbacks
    override fun onClientSettingsError(ex: Exception) {
        if (ex is ResolvableApiException){
            ex.startResolutionForResult(this, Constants.RC_CHECK_SETTINGS)
        }
    }

    override fun onLocationPermissionsNeeded() {
        requestLocationPermissions()
    }

    override fun onLocationObtained(location: Location?) {
        location.let {
            viewModel.storeLocation(location?.latitude,location?.longitude)
            viewModel.getNearbyRestaurants(false)
        }
    }
    //endregion

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: RestaurantsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbarSetUp()

        initLocationManager()
        initViewModel()

        // Renews token every time the user launches the app
        // An optimization would be to request it only when we detect
        // that our locally saved token has expired
        renewToken()

        observeViewModel()
        observeListScrolling()

        maps_fab.setOnClickListener {
            val restaurants = viewModel.getSerializedRestaurants()
            goToMapsActivity(restaurants)
        }
    }

    private fun toolbarSetUp(){
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.main_activity_title)
    }

    private fun initLocationManager(){
        locationManager = LocationManager(this)
    }

    private fun initViewModel(){
        viewModel = ViewModelProviders.of(this,
                BaseViewModelFactory{ MainViewModel(getSharedPreferences(Constants.APP_PREFS, Context.MODE_PRIVATE)) })
                .get(MainViewModel::class.java)
    }

    /**
     * On token response, launch restaurants GET flow. If we got an error
     * when trying to get a new token, show an error to the user prompting
     * him to retry token request
     */
    private fun renewToken(){
        viewModel.renewToken().observe(this, Observer { response ->

            if (response.exception != null){

                showError(response.localizedMessage!!) { renewToken() }
                return@Observer
            }

            locationManager.checkLocationSettings()
        })
    }

    private fun showError(message: Int, positiveAction: () -> Unit){

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.main_activity_fetch_token_error_title)
            .setMessage(message)
            .setPositiveButton(R.string.main_activity_fetch_token_error_retry){ dialog, _ ->
                dialog.dismiss()

                positiveAction()
            }
            .setCancelable(false)

        dialog.show()
    }

    private fun showError(message: String, positiveAction: () -> Unit){

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.main_activity_fetch_token_error_title)
            .setMessage(message)
            .setPositiveButton(R.string.main_activity_fetch_token_error_retry){ dialog, _ ->
                dialog.dismiss()

                positiveAction()
            }
            .setCancelable(false)

        dialog.show()
    }

    private fun observeViewModel(){
        viewModel.restaurants.observe(this, Observer { restaurants ->
            showLoading(false)
            showFab(true)
            setRestaurantsList(restaurants)
        })

        viewModel.localizedError.observe(this, Observer { localizedMessage ->
            showError(localizedMessage){ viewModel.getNearbyRestaurants(viewModel.loadingMoreItems) }
        })

        viewModel.error.observe(this, Observer { error ->
            showError(error){ viewModel.getNearbyRestaurants(viewModel.loadingMoreItems) }
        })
    }

    private fun setRestaurantsList(restaurants: List<Restaurant?>) {

        if (!::adapter.isInitialized){
            adapter = RestaurantsAdapter(restaurants)
            rv_restaurants.adapter = adapter
        }else{
            adapter.notifyDataSetChanged()
        }
    }

    /**
     * Observe RecyclerView scrolling, to find out when we reach the end of the list
     * Once the end has been reached, load more restaurants from the service.
     *
     * On list scrolling, also hide fab when going down, and show it when going up.
     */
    private fun observeListScrolling(){

        val layoutManager = rv_restaurants.layoutManager as LinearLayoutManager

        rv_restaurants.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                showHideFabOnScrolling(dy)

                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if(totalItemCount == (lastVisibleItem + 1) && !viewModel.loadingMoreItems){
                    loadMoreRestaurants()
                }
            }
        })
    }

    private fun showHideFabOnScrolling(dy:Int){
        if (dy > 0 && maps_fab.isShown){
            showFab(false)
        }else if(dy < 0 && !maps_fab.isShown){
            showFab(true)
        }
    }

    /**
     * When the end of the restaurants list has been reached, we have to load
     * more restaurants from the service. To achieve this, first insert a
     * "loading item" to the list (a null value) and notify adapter that this
     * item has been added. This will make RecyclerView to show a ProgressBar
     * in the end of the list
     */
    private fun loadMoreRestaurants(){
        viewModel.insertLoadingItemInRestaurantsList()
        adapter.notifyItemInserted(viewModel.restaurantsCount() - 1)

        viewModel.getNearbyRestaurants(true)
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
        }else if (requestCode == Constants.RC_CHANGE_LOCATION){

            if (resultCode == Activity.RESULT_OK){
                val latitude = data?.getDoubleExtra(Constants.EXTRA_USER_LATITUDE,0.0)
                val longitude = data?.getDoubleExtra(Constants.EXTRA_USER_LONGITUDE,0.0)

                showLoading(true)

                viewModel.storeLocation(latitude,longitude)

                // On user location changed, clear previously obtained
                // obtained restaurants and offset before getting new ones
                viewModel.clearData()
                viewModel.getNearbyRestaurants(false)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item?.itemId){

            R.id.action_change_location -> goToMapsActivity(null)

        }

        return super.onOptionsItemSelected(item)
    }

    private fun goToMapsActivity(restaurants: String?){

        val mapsIntent = Intent(this,MapsActivity::class.java)

        if (restaurants != null){
            mapsIntent.putExtra(Constants.EXTRA_RESTAURANTS,restaurants)
        }

        mapsIntent.putExtra(Constants.EXTRA_USER_LATITUDE,viewModel.getLatitude())
        mapsIntent.putExtra(Constants.EXTRA_USER_LONGITUDE,viewModel.getLongitude())

        if (restaurants != null){
            startActivity(mapsIntent)
        }else{
            startActivityForResult(mapsIntent,Constants.RC_CHANGE_LOCATION)
        }
    }

    private fun showLoading(b: Boolean){
        if(b){
            pb.visibility = View.VISIBLE
            rv_restaurants.visibility = View.INVISIBLE
        }else{
            pb.visibility = View.INVISIBLE
            rv_restaurants.visibility = View.VISIBLE
        }
    }

    private fun showFab(b: Boolean){
        if (b){
            maps_fab.show()
        }else{
            maps_fab.hide()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.destroy()
    }

}
