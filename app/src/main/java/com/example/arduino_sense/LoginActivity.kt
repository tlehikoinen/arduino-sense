package com.example.arduino_sense

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils

class LoginActivity : AppCompatActivity() {

    lateinit var login_btn: Button
    lateinit var text_username: EditText
    lateinit var text_password: EditText
    lateinit var text_response: TextView
    var userService = UserService()
    lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = this.getSharedPreferences("token", Context.MODE_PRIVATE)
        setContentView(R.layout.activity_login)
        initLoginBtn()
        initTextBoxes()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun initLoginBtn() {
        login_btn = findViewById(R.id.btn_login)
        login_btn.setOnClickListener { login() }
    }
    private fun initTextBoxes() {
        text_username = findViewById(R.id.signup_username)
        text_password = findViewById(R.id.signup_password)
        text_response = findViewById(R.id.login_text_response)
    }

    private fun saveToken(value: String) {
        val edit = pref.edit()
        edit.putString("token", "Bearer ".plus(value))
        edit.commit()
    }

    private fun login() {
        Log.d("login", "username ${text_username.text} password ${text_password.text}")
        val reqUser = PostUserReq(text_username.text.toString(), text_password.text.toString())
        userService.loginUser(reqUser, object : UserService.LoginCallback {
            override fun onSuccess(token: String) {
                Log.d("tag", "LOGIN SUCCESS")
                Log.d("tag", "$token")
                saveToken(token)
                data.setUsername(reqUser.username)
                data.setToken(token)
                onBackPressed()
            }

            override fun onFailure() {
                text_response.setText("Login failed")
                set_res_text_delay("Login failed", 2000)
                Log.d("tag", "LOGIN FAILRUE")
            }

        })
        Log.d("tag", "AFTER LOGIN")
        Log.d("tag", data.getToken())

    }

    private fun set_res_text_delay(text: String, delayMs: Long) {
        text_response.setText(text)
        Handler(Looper.getMainLooper()).postDelayed(
            { text_response.setText("") },
            delayMs
        )
    }
}