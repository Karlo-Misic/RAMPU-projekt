package com.example.quizifyrampu

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var leaderboardListView: ListView
    private lateinit var backToMenuButton: Button
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        leaderboardListView = findViewById(R.id.lv_leaderboard)
        backToMenuButton = findViewById(R.id.btn_back_to_menu)

        fetchLeaderboardData()

        backToMenuButton.setOnClickListener {
            val intent = Intent(this, GameModeActivity::class.java)
            startActivity(intent)
            finish()
        }

        val textView = findViewById<TextView>(R.id.tv_leaderboard_title)
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

    private fun fetchLeaderboardData() {
        val url = "http://157.230.8.219/quizify/get_leaderboard.php"
        val credentials = Credentials.basic("aplikatori", "nA7:B&")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", credentials)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@LeaderboardActivity,
                        "Failed to fetch leaderboard data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(
                            this@LeaderboardActivity,
                            "Failed to fetch leaderboard data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return
                }

                try {
                    val jsonObject = JSONObject(responseBody)
                    val status = jsonObject.optString("status", "error")

                    if (status != "success") {
                        runOnUiThread {
                            Toast.makeText(
                                this@LeaderboardActivity,
                                "Failed to fetch leaderboard data",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return
                    }

                    val leaderboardArray = jsonObject.getJSONArray("data")
                    val leaderboardData = mutableListOf<String>()

                    for (i in 0 until leaderboardArray.length()) {
                        val player = leaderboardArray.getJSONObject(i)
                        val username = player.optString("username", "Unknown")
                        val score = player.optInt("score", 0)
                        leaderboardData.add("${i + 1}. $username - $score points")
                    }

                    runOnUiThread {
                        val adapter = ArrayAdapter(
                            this@LeaderboardActivity,
                            android.R.layout.simple_list_item_1,
                            leaderboardData
                        )
                        leaderboardListView.adapter = adapter
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@LeaderboardActivity,
                            "Error parsing leaderboard data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

}
