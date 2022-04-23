package com.example.arduino_sense

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ResponseBody
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

data class PostDataReq (
    val temperature: Int,
    val humidity: Int
)

interface TempHumidInterface {
    @GET("data")
    fun getAllData(): Call<List<TempHumidJsonModel>>

    @GET("/data/{username}")
    fun getUserData(
        @Path("username") username: String
    ): Call<List<TempHumidJsonModel>>

    @POST("/data")
    fun postData(
        @Body dataBody: PostDataReq,
        @Header("Authorization") authorization: String
    ): Call<ResponseBody>

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

    fun postData(authorization: String, dataBody: PostDataReq) {
        DataApi.retrofitService.postData(dataBody, authorization).enqueue(
            object: Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>?,
                    response: Response<ResponseBody>?
                ) {
                    if (response?.code() == 200) {
                        Log.d("jpk","Post data responded with status code ${response?.code().toString()}")
                        Log.d("jpk", response?.message().toString())
                    } else {
                        Log.e("jpk", "Post data failed: ${response?.errorBody()?.string()}")
                    }
                }
                override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
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

data class PostUserReq (
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

    @POST("/user")
    fun addUser(
        @Body userBody: PostUserReq
    ): Call<ResponseBody>

    @POST("/user/login")
    fun loginUser(
        @Body userBody: PostUserReq
    ): Call<ResponseBody>

}

object UserApi {
    val retrofitService: UserInterface by lazy {
        retroFit.create(UserInterface::class.java)
    }
}

class UserService {
    interface LoginCallback {
        fun onSuccess(token: String)
        fun onFailure()
    }
    interface SignupCallback {
        fun onSuccess(message: String)
        fun onFailure(errorMessage: String)
    }
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

    fun loginUser(userBody: PostUserReq, loginCallback: LoginCallback) {
        var token: String = ""
        UserApi.retrofitService.loginUser(userBody).enqueue(
            object: Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>?,
                    response: Response<ResponseBody>?
                ) {
                    if (response?.code() == 200) {
                        token = response?.body().string()
                        Log.d("jpk", "login successful with status code 200. " +
                                "Token: ${token}")
                        loginCallback.onSuccess(token)
                    } else {
                        Log.e("jpk", "Login failed with status code ${response?.code()}. ${response?.message().toString()}")
                        loginCallback.onFailure()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                    loginCallback.onFailure()
                    Log.e("jpk", "Login failed")
                }
            }
        )
    }

    fun createUser(userBody: PostUserReq, signupCallback: SignupCallback) {
        UserApi.retrofitService.addUser(userBody).enqueue(
            object: Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>?,
                    response: Response<ResponseBody>?
                ) {
                    if (response?.code() == 200) {
                        Log.d("jpk", "Create user responded with status code 200 ${response?.message()}")
                        signupCallback.onSuccess("User created successfully")
                    } else {
                        val errorMessage = response?.errorBody()?.string() ?: "Empty error message"
                        if (response != null) {
                            signupCallback.onFailure(errorMessage)
                        }
                        Log.e("jpk", "Creating user failed with status code ${response?.code()} " +
                                "${errorMessage}")
                    }
                }
                override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                    signupCallback.onFailure("Create user failed")
                    Log.d("jpk", "Create user failed")
                }
            }
        )
    }
}


