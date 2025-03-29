package com.example.quizifyrampu

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DifficultySelectionActivity : AppCompatActivity() {

    private lateinit var tvSelectedGameMode: TextView
    private lateinit var tvSelectedCategory: TextView
    private lateinit var btnEasy: Button
    private lateinit var btnMedium: Button
    private lateinit var btnHard: Button
    private lateinit var btnRandom: Button
    private lateinit var backButton: ImageView
    private lateinit var btnExit: ImageView
    private lateinit var btnHome: ImageView
    private lateinit var btnProfile: ImageView

    private var selectedGameMode: String? = null
    private var category: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_difficulty_selection)

        tvSelectedGameMode = findViewById(R.id.tvSelectedGameMode)
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory)
        btnEasy = findViewById(R.id.btn_easy)
        btnMedium = findViewById(R.id.btn_medium)
        btnHard = findViewById(R.id.btn_hard)
        btnRandom = findViewById(R.id.btn_random)
        btnExit = findViewById(R.id.btn_exit)
        btnHome = findViewById(R.id.btn_home)
        btnProfile = findViewById(R.id.btn_profile)
        backButton = findViewById(R.id.btn_back)

        selectedGameMode = intent.getStringExtra("gameMode")
        category = intent.getStringExtra("category")

        tvSelectedGameMode.text = "Game Mode: $selectedGameMode"
        tvSelectedCategory.text = "Kategorija: $category"

        btnEasy.setOnClickListener { startQuiz("easy") }
        btnMedium.setOnClickListener { startQuiz("medium") }
        btnHard.setOnClickListener { startQuiz("hard") }
        btnRandom.setOnClickListener { startQuiz("random") }

        backButton.setOnClickListener {
            finish()
        }

        btnExit.setOnClickListener { finishAffinity() }

        btnHome.setOnClickListener {
            val intent = Intent(this, GameModeActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnProfile.setOnClickListener {
            val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getInt("user_id", -1)
            if (userId == -1) {
                Toast.makeText(this, "You are not signed in!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
        }

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
    }

    private fun startQuiz(difficulty: String) {
        val intent = Intent(this, if (selectedGameMode == "timeRush") {
            TimeRushActivity::class.java
        } else {
            QuizActivity::class.java
        })

        intent.putExtra("gameMode", selectedGameMode)
        intent.putExtra("category", category)
        intent.putExtra("difficulty", difficulty)

        startActivity(intent)
    }
}