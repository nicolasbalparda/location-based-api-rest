package com.geermank.restaurants.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.geermank.restaurants.R
import com.geermank.restaurants.repository.models.Restaurant
import kotlinx.android.synthetic.main.item_restaurants.view.*

class RestaurantsAdapter(
    private val restaurants: List<Restaurant?>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object{
        private const val RESTAURANT_VIEW_TYPE = 0
        private const val LOADING_VIEW_TYPE = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val view: View?
        return if (viewType == RESTAURANT_VIEW_TYPE){
            view = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurants,parent,false)
            RestaurantsHolder(view)
        }else{
            view = LayoutInflater.from(parent.context).inflate(R.layout.item_progress,parent,false)
            LoadingHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return restaurants.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RestaurantsHolder){
            holder.bind(restaurants[position] as Restaurant)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (restaurants[position] != null){
            RESTAURANT_VIEW_TYPE
        }else {
            LOADING_VIEW_TYPE
        }
    }

    inner class RestaurantsHolder(
        private val view: View
    ): RecyclerView.ViewHolder(view){

        fun bind(restaurant: Restaurant){
            //getImage(restaurant.headerImage)

            view.tv_restaurant_title.text = restaurant.name
            view.tv_restaurant_starts.text = restaurant.generalScore.toString()

            val deliveryInfo = view.context.getString(R.string.delivery) + " " +
                    "$${restaurant.shippingAmount}  " + view.context.getString(R.string.dot) +
                    "  ${restaurant.deliveryTime}" + " " + view.context.getString(R.string.minutes_short)
            view.tv_restaurant_delivery_info.text = deliveryInfo

            view.tv_restaurant_categories.text = restaurant.allCategories
        }

        /*private fun getImage(headerImage: String) {

            val builder = Picasso.Builder(view.context)

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val newRequest = chain.request().newBuilder()
                        .addHeader("Authorization",view.context.getSharedPreferences(Constants.APP_PREFS,Context.MODE_PRIVATE).getString(BaseRepository.KEY_TOKEN,"")!!.trim())
                        .build()

                    chain.proceed(newRequest)
                }.build()

            val picasso = builder.downloader(OkHttp3Downloader(client)).build()
            picasso.load(ApiConstants.IMAGES_BASE_URL+headerImage)
                .into(view.iv_restaurant_header, object : Callback {

                    override fun onSuccess() {

                    }

                    override fun onError(ex: Exception?) {
                        ex?.printStackTrace()
                    }
                })
        }*/
    }

    inner class LoadingHolder(view: View): RecyclerView.ViewHolder(view)

}