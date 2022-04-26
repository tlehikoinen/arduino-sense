package com.example.arduino_sense

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.google.gson.Gson

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

    private fun saveUser(value: LoggedUser) {
        val edit = pref.edit()
        val gson = Gson()
        val jsonUser: String = gson.toJson(value)
        edit.putString("user", jsonUser)
        edit.commit()
    }

    private fun login() {
        Log.d("login", "username ${text_username.text} password ${text_password.text}")
        val reqUser = PostUserReq(text_username.text.toString(), text_password.text.toString())
        userService.loginUser(reqUser, object : UserService.LoginCallback {
            override fun onSuccess(token: String) {
                val loggedUser = LoggedUser(username=reqUser.username, token="Bearer ".plus(token))
                saveUser(loggedUser)
                data.setUsername(reqUser.username)
                data.setToken("Bearer ".plus(token))
                onBackPressed()
                toast("Logged in as: ${reqUser.username}")
                /*
                finishAffinity()
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)

                 */
            }

            override fun onFailure() {
                set_res_text_delay("Login failed", 2000)
                Log.d("tag", "LOGIN FAILURE")
            }
        })
    }
    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
    private fun set_res_text_delay(text: String, delayMs: Long) {
        text_response.setText(text)
        Handler(Looper.getMainLooper()).postDelayed(
            { text_response.setText("") },
            delayMs
        )
    }
}