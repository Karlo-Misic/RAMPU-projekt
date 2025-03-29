package com.example.quizifyrampu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvFirstName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvBadge: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var btnHome: ImageView
    private lateinit var btnProfile: ImageView
    private lateinit var btnExit: ImageView
    private lateinit var backButton: ImageView
    private lateinit var btnQuizHistory: Button


    private val client = OkHttpClient()
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tvFirstName = findViewById(R.id.tv_first_name_profile)
        tvUsername = findViewById(R.id.tv_username_profile)
        etEmail = findViewById(R.id.et_email_profile)
        etPassword = findViewById(R.id.et_password_profile)
        tvBadge = findViewById(R.id.tv_badge)
        btnEditProfile = findViewById(R.id.btn_edit_profile)
        btnLogout = findViewById(R.id.btn_logout)
        btnHome = findViewById(R.id.btn_home)
        btnProfile = findViewById(R.id.btn_profile)
        btnExit = findViewById(R.id.btn_exit)
        backButton = findViewById(R.id.btn_back)

        btnQuizHistory = findViewById(R.id.btn_quiz_history)
        btnQuizHistory.setOnClickListener {
            val intent = Intent(this, QuizHistoryActivity::class.java)
            startActivity(intent)
        }

        val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Niste prijavljeni", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            fetchUserProfile(userId)
        }

        btnEditProfile.setOnClickListener { updateUserProfile(userId) }
        btnLogout.setOnClickListener { logoutUser() }
        backButton.setOnClickListener { finish() }
        btnExit.setOnClickListener { finishAffinity() }
        btnHome.setOnClickListener {
            startActivity(Intent(this, GameModeActivity::class.java))
            finish()
        }
        btnProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
    }

    private fun fetchUserProfile(userId: Int) {
        val credentials = Credentials.basic("aplikatori", "nA7:B&")
        val request = Request.Builder()
            .url("http://157.230.8.219/quizify/get_user_profile.php?user_id=$userId")
            .addHeader("Authorization", credentials)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Greška pri dohvatu podataka", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("ProfileActivity", "Server response: $responseBody") // Log odgovora servera

                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.getString("status") == "success") {
                            val data = jsonResponse.getJSONObject("data")
                            val score = data.optInt("score", 0)

                            runOnUiThread {
                                tvFirstName.text = data.optString("ime_i_prezime", "N/A")
                                tvUsername.text = data.optString("username", "N/A")
                                etEmail.setText(data.optString("email", ""))
                                tvBadge.text = "Značka: ${getBadgeForScore(score)}"
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@ProfileActivity, "Greška: ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileActivity", "JSON Parsing Error: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(this@ProfileActivity, "Greška u parsiranju podataka", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun getBadgeForScore(score: Int): String {
        return when {
            score >= 10000 -> "Master"
            score >= 5000 -> "Expert"
            score >= 1000 -> "Intermediate"
            score >= 500 -> "Beginner"
            else -> "Novice"
        }
    }

    private fun updateUserProfile(userId: Int) {
        val credentials = Credentials.basic("aplikatori", "nA7:B&")
        val json = JSONObject().apply {
            put("user_id", userId)
            put("email", etEmail.text.toString())
            put("password", etPassword.text.toString())
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("http://157.230.8.219/quizify/update_user_profile.php")
            .addHeader("Authorization", credentials)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Greška pri ažuriranju profila", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Profil ažuriran", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun logoutUser() {
        val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
