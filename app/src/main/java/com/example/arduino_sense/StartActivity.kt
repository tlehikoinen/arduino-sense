package com.example.arduino_sense

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {
    private lateinit var openLogin: Button
    private lateinit var openSignup: Button
    lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = this.getSharedPreferences("token", Context.MODE_PRIVATE)
        setContentView(R.layout.start_activity)
        readToken()
        initOpenLogin()
        initOpenSignup()
    }


    private fun initOpenSignup() {
        openSignup = findViewById(R.id.btn_open_signup)
        openSignup.setOnClickListener {
            val intent = Intent(this@StartActivity, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initOpenLogin() {
        openLogin = findViewById(R.id.btn_open_login)
        openLogin.setOnClickListener {
            loginOrConnect()
        }
        loginOrLogoutText()
    }
    private fun readToken() {
        val token = pref.getString("token", "DEFAULT").toString()
        data.setToken(token)
    }
    private fun loginOrConnect(){
        if (data.getToken().startsWith("Bearer")) {
            finishAffinity()
            val intent = Intent(this@StartActivity, MainActivity::class.java)
            startActivity(intent)
        }
        else {
            val intent = Intent(this@StartActivity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginOrLogoutText() {
        openLogin.setText(if (data.getToken().startsWith("Bearer ")) "Connect" else "Login")
    }

}