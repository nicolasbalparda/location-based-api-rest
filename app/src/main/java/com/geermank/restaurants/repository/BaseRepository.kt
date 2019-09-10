package com.geermank.restaurants.repository

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.geermank.restaurants.R
import com.geermank.restaurants.repository.models.AuthResponseWrapper
import com.geermank.restaurants.repository.models.Token
import com.geermank.restaurants.utils.ApiConstants
import com.geermank.restaurants.utils.Constants
import retrofit2.*
import retrofit2.converter.moshi.MoshiConverterFactory


/**
 * Base class to any other kind of repository that we will create. Its purpose is to
 * handle common operations among every repo in our app
 *
 * As a constructor parameter, we will have to pass SharedPref reference to handle token
 * operations needed in any request
 *
 * TODO include DI to get SharedPreferences
 *
 */
open class BaseRepository(
    private var preferences: SharedPreferences
) {

    companion object{
        const val KEY_TOKEN = "KEY_TOKEN"
    }

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ApiConstants.BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()


    fun getApiService(): ApiService{
        return retrofit.create(ApiService::class.java)
    }

    fun refreshToken(): LiveData<AuthResponseWrapper>{

        val tokenResponse = MutableLiveData<AuthResponseWrapper>()

        getApiService().getToken(ApiConstants.CLIENT_ID,ApiConstants.CLIENT_SECRET)
            .enqueue(object : Callback<Token> {

                override fun onFailure(call: Call<Token>, t: Throwable) {
                    val res = AuthResponseWrapper(t.message, R.string.request_error_failed)
                    tokenResponse.postValue(res)
                }

                override fun onResponse(call: Call<Token>, response: Response<Token>) {

                    val res = AuthResponseWrapper()

                    if (response.isSuccessful){
                        preferences.edit()
                            .putString(KEY_TOKEN,response.body()!!.token)
                            .apply()
                    }else{
                        res.localizedMessage = getLocalizedResponseMessage(response.code())
                        res.exception = response.message()
                    }

                    tokenResponse.postValue(res)
                }

            })

        return tokenResponse
    }

    fun getToken():String? = preferences.getString(KEY_TOKEN,null)

    /**
     * Get localized message resource from API Http response code
     *
     * This messages are generic, based on Http Group
     */
    fun getLocalizedResponseMessage(code: Int): Int{

        return when(code){
            in 200..299 -> R.string.request_successful
            in 300..399 -> R.string.request_redirecting
            in 400..499 -> R.string.request_error_client_problem
            in 500..599 -> R.string.request_error_server_problem

            else -> R.string.request_error_failed
        }
    }

}