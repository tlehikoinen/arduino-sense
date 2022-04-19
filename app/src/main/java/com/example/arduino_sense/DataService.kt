package com.example.arduino_sense

import android.provider.ContactsContract
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

private const val BASE_URL = "https://arduino-sense.herokuapp.com/"

val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

val retroFit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

data class TempHumidJsonModel (
    val id: Int,
    val user_id: Int,
    val temperature: Int,
    val humidity: Int,
    val date: String
)


interface TempHumidInterface {
    @GET("data")
    fun getAllData(): Call<List<TempHumidJsonModel>>

    @GET("/data/{username}")
    fun getUserData(
        @Path("username") username: String
    ): Call<List<TempHumidJsonModel>>

    @FormUrlEncoded
    @POST("/data")
    fun postData(
        @Field("temperature") temperature: Int,
        @Field("humidity") humidity: Int,
        @Header("Authorization") authorization: String
    ): Call<List<String>>

}

object DataApi {
    val retrofitService: TempHumidInterface by lazy {
        retroFit.create(TempHumidInterface::class.java)
    }
}

class DataService {
    fun fetchAllData() {
        DataApi.retrofitService.getAllData().enqueue(
            object: Callback<List<TempHumidJsonModel>> {
                override fun onResponse(
                    call: Call<List<TempHumidJsonModel>>?,
                    response: Response<List<TempHumidJsonModel>>?
                ) {
                    Log.d("jpk", "Fetching all data")
                    response?.body()?.forEach { Log.d("jpk", "${it.toString()}") }
                }

                override fun onFailure(call: Call<List<TempHumidJsonModel>>?, t: Throwable?) {
                    Log.d("jpk", "Fetch for getAllData failed ${t?.message}")
                }
            }
        )
    }

    fun fetchUserData(username: String) {
        DataApi.retrofitService.getUserData(username).enqueue(
            object: Callback<List<TempHumidJsonModel>> {
                override fun onResponse(
                    call: Call<List<TempHumidJsonModel>>?,
                    response: Response<List<TempHumidJsonModel>>?
                ) {
                    Log.d("jpk","Fetching data for $username")
                    response?.body()?.forEach { Log.d("jpk", "$it")}
                }

                override fun onFailure(call: Call<List<TempHumidJsonModel>>?, t: Throwable?) {
                    Log.d("jpk", "Fetch data for user $username failed")
                }
            }
        )
    }

    fun postData(authorization: String, temperature: Int, humidity: Int) {
        DataApi.retrofitService.postData(temperature, humidity, authorization).enqueue(
            object: Callback<List<String>> {
                override fun onResponse(
                    call: Call<List<String>>?,
                    response: Response<List<String>>?
                ) {
                    Log.d("jpk","Post data response")
                    Log.d("jpk", response?.code().toString())
                }

                override fun onFailure(call: Call<List<String>>?, t: Throwable?) {
                    Log.d("jpk", "Post data failed")
                }
            }
        )
    }
}

data class UserJsonModel (
    val id: Int,
    val username: String,
    val password: String
)


interface UserInterface {
    @GET("user")
    fun getAllUsers(): Call<List<UserJsonModel>>

    @GET("/data/{id}")
    fun getUser(
        @Path("id") userId: Int
    ): Call<List<UserJsonModel>>

    @FormUrlEncoded
    @POST("/user")
    fun addUser(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<List<String>>

    @FormUrlEncoded
    @POST("/user/login")
    fun loginUser(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<List<String>>

}

object UserApi {
    val retrofitService: UserInterface by lazy {
        retroFit.create(UserInterface::class.java)
    }
}

class UserService {
    fun getUsers() {
        UserApi.retrofitService.getAllUsers().enqueue(
            object: Callback<List<UserJsonModel>> {
                override fun onResponse(
                    call: Call<List<UserJsonModel>>?,
                    response: Response<List<UserJsonModel>>?
                ) {
                    Log.d("jpk", "Response for getAllUsers")
                    response?.body()?.forEach { Log.d("jpk", "$it")}
                }

                override fun onFailure(call: Call<List<UserJsonModel>>?, t: Throwable?) {
                    Log.d("jpk", "GetAllUsers failed")
                }
            }
        )
    }
    fun getUserData(userId: Int) {
        UserApi.retrofitService.getUser(userId).enqueue(
            object: Callback<List<UserJsonModel>> {
                override fun onResponse(
                    call: Call<List<UserJsonModel>>?,
                    response: Response<List<UserJsonModel>>?
                ) {
                    Log.d("jpk", "Response for getUserData $userId: ${response?.body()}")
                }

                override fun onFailure(call: Call<List<UserJsonModel>>?, t: Throwable?) {
                    Log.d("jpk", "GetUserData $userId failed")
                }
            }
        )
    }

    fun loginUser(username: String, password: String) {
        UserApi.retrofitService.loginUser(username, password).enqueue(
            object: Callback<List<String>> {
                override fun onResponse(
                    call: Call<List<String>>?,
                    response: Response<List<String>>?
                ) {
                    Log.d("jpk", "login response ${response?.body()}")
                }

                override fun onFailure(call: Call<List<String>>?, t: Throwable?) {
                    Log.d("jpk", "Login failed")
                }
            }
        )
    }

    fun createUser(username: String, password: String) {
        UserApi.retrofitService.addUser(username, password).enqueue(
            object: Callback<List<String>> {
                override fun onResponse(
                    call: Call<List<String>>?,
                    response: Response<List<String>>?
                ) {
                    Log.d("jpk", "Create user response ${response?.body()}")
                    Log.d("jpk", response?.code().toString())
                }

                override fun onFailure(call: Call<List<String>>?, t: Throwable?) {
                    Log.d("jpk", "Create user failed")
                }
            }
        )
    }
}


