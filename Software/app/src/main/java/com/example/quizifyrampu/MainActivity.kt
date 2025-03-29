package com.example.quizifyrampu

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var exitButton: ImageView
    private val client = OkHttpClient()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isUserLoggedIn()) {
            val intent = Intent(this, GameModeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val playAsGuestButton: Button = findViewById(R.id.btn_guest)
        val registerButton: Button = findViewById(R.id.btn_register)
        val loginButton: Button = findViewById(R.id.btn_sign_in)
        exitButton = findViewById(R.id.btn_exit)
        etUsername = findViewById(R.id.et_username_login)
        etPassword = findViewById(R.id.et_password_login)

        val textView = findViewById<TextView>(R.id.tv_app_title)
        val shader = LinearGradient(
            0f, 0f, 0f, textView.textSize * 1.5f,
            intArrayOf(
                Color.parseColor("#FFD700"),
                Color.parseColor("#FF8C00"),
                Color.parseColor("#FF4500")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        textView.paint.shader = shader
        textView.setShadowLayer(8f, 4f, 4f, Color.YELLOW)

        playAsGuestButton.setOnClickListener {
            clearUserSession()
            val intent = Intent(this, GameModeActivity::class.java)
            startActivity(intent)
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener { loginUser() }
        exitButton.setOnClickListener {
            finishAffinity()
        }
    }

    private fun loginUser() {
        val username = etUsername.text.toString()
        val password = etPassword.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val credentials = Credentials.basic("aplikatori", "nA7:B&")

        val request = Request.Builder()
            .url("http://157.230.8.219/quizify/login_user.php")
            .addHeader("Authorization", credentials)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(responseBody ?: "")
                        if (jsonResponse.getString("status") == "success") {
                            val user = jsonResponse.getJSONObject("user")
                            val userId = user.getInt("id")
                            val imePrezime = user.getString("ime_i_prezime")

                            saveUserSession(userId, imePrezime)

                            Toast.makeText(this@MainActivity, "Welcome, $imePrezime", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this@MainActivity, GameModeActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@MainActivity, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun saveUserSession(userId: Int, imePrezime: String) {
        val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("user_id", userId)
        editor.putString("ime_prezime", imePrezime)
        editor.putBoolean("is_logged_in", true) // Sprema da je korisnik prijavljen
        editor.apply()
    }

    private fun clearUserSession() {
        val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false)
    }
}
