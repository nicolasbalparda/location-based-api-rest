package com.geermank.restaurants.repository

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.geermank.restaurants.repository.models.Token
import com.geermank.restaurants.repository.models.TokenResponse
import com.geermank.restaurants.utils.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
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
        .baseUrl(Constants.BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()


    fun getApiService(): ApiService{
        return retrofit.create(ApiService::class.java)
    }

    fun refreshToken(): LiveData<TokenResponse>{

        val tokenResponse = MutableLiveData<TokenResponse>()

        getApiService().getToken(Constants.CLIENT_ID,Constants.CLIENT_SECRET)
            .enqueue(object : Callback<Token> {

                override fun onFailure(call: Call<Token>, t: Throwable) {
                    val res = TokenResponse(t.localizedMessage)
                    tokenResponse.postValue(res)
                }

                override fun onResponse(call: Call<Token>, response: Response<Token>) {

                    val res = TokenResponse(response.message(),response.code())

                    if (response.isSuccessful){
                        preferences.edit()
                            .putString(KEY_TOKEN,response.body()!!.token)
                            .apply()

                        res.error = false
                    }

                    tokenResponse.postValue(res)
                }

            })

        return tokenResponse
    }

    fun getToken():String? = preferences.getString(KEY_TOKEN,null)

}