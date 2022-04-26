package com.example.arduino_sense

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import okhttp3.ResponseBody

class SignupActivity : AppCompatActivity() {
    lateinit var signup_btn: Button
    lateinit var text_username: EditText
    lateinit var text_password: EditText
    lateinit var text_response: TextView
    var userService = UserService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        initSignupBtn()
        initTextBoxes()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun initSignupBtn() {
        signup_btn = findViewById(R.id.btn_signup)
        signup_btn.setOnClickListener { signUp() }
    }
    private fun initTextBoxes() {
        text_username = findViewById(R.id.signup_username)
        text_password = findViewById(R.id.signup_password)
        text_response = findViewById(R.id.signup_text_response)
    }

    private fun signUp() {
        Log.d("Signup", "username ${text_username.text} password ${text_password.text}")
        val reqUser = PostUserReq(text_username.text.toString(), text_password.text.toString())
        userService.createUser(reqUser, object : UserService.SignupCallback {
            override fun onSuccess(response: String) {
                Log.d("signup", "success ${response}")
                toast("SignUp success, you can login now")
                onBackPressed()
            }

            override fun onFailure(errorMessage: String) {
                Log.d("signup", "signup failed $errorMessage")
                set_res_text_delay(errorMessage, 2000)
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