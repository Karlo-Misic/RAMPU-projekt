package com.example.quizifyrampu

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

class CategorySelectionActivity : AppCompatActivity() {

    private lateinit var btnExit: ImageView
    private lateinit var btnHome: ImageView
    private lateinit var btnProfile: ImageView
    private lateinit var btnAllCategories: Button
    private lateinit var btnMovies: Button
    private lateinit var btnGames: Button
    private lateinit var btnHistory: Button
    private lateinit var btnGeography: Button
    private lateinit var btnMusic: Button
    private lateinit var btnPhysics: Button
    private lateinit var btnBiology: Button
    private lateinit var btnCars: Button
    private lateinit var btnSport: Button
    private lateinit var btnIT: Button
    private lateinit var backButton: ImageView

    private var gameMode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_selection)

        // Inicijalizacija gumba navigacijske trake
        btnExit = findViewById(R.id.btn_exit)
        btnHome = findViewById(R.id.btn_home)
        btnProfile = findViewById(R.id.btn_profile)
        backButton = findViewById(R.id.btn_back)

        // Inicijalizacija gumba za kategorije
        btnAllCategories = findViewById(R.id.button_all_categories)
        btnMovies = findViewById(R.id.button_movies)
        btnGames = findViewById(R.id.button_games)
        btnHistory = findViewById(R.id.button_history)
        btnGeography = findViewById(R.id.button_geography)
        btnMusic = findViewById(R.id.button_music)
        btnPhysics = findViewById(R.id.button_physics)
        btnBiology = findViewById(R.id.button_biology)
        btnCars = findViewById(R.id.button_cars)
        btnSport = findViewById(R.id.button_sport)
        btnIT = findViewById(R.id.button_it)

        gameMode = intent.getStringExtra("gameMode")

        backButton.setOnClickListener {
            finish()
        }

        btnExit.setOnClickListener { finishAffinity() } // Izlaz iz aplikacije

        btnHome.setOnClickListener {
            val intent = Intent(this, GameModeActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnProfile.setOnClickListener {
            val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getInt("user_id", -1)
            if (userId == -1) {
                Toast.makeText(this, "Niste prijavljeni!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
        }

        btnAllCategories.setOnClickListener { startDifficultySelection("General") }
        btnMovies.setOnClickListener { startDifficultySelection("Movies") }
        btnGames.setOnClickListener { startDifficultySelection("Games") }
        btnHistory.setOnClickListener { startDifficultySelection("History") }
        btnGeography.setOnClickListener { startDifficultySelection("Geography") }
        btnMusic.setOnClickListener { startDifficultySelection("Music") }
        btnPhysics.setOnClickListener { startDifficultySelection("Physics") }
        btnBiology.setOnClickListener { startDifficultySelection("Biology") }
        btnCars.setOnClickListener { startDifficultySelection("Cars") }
        btnSport.setOnClickListener { startDifficultySelection("Sport") }
        btnIT.setOnClickListener { startDifficultySelection("IT") }

        // Setovanje gradienta za naslov
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

    private fun startDifficultySelection(category: String) {
        val intent = Intent(this, DifficultySelectionActivity::class.java)
        intent.putExtra("gameMode", gameMode)
        intent.putExtra("category", category)
        startActivity(intent)
    }
}
