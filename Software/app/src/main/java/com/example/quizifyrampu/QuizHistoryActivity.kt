package com.example.quizifyrampu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class QuizHistoryActivity : AppCompatActivity() {

    private lateinit var listViewHistory: ListView
    private val client = OkHttpClient()
    private var userId: Int = -1
    private lateinit var backToMenuButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_history)


        backToMenuButton = findViewById(R.id.btn_back_to_menu)
        listViewHistory = findViewById(R.id.listViewHistory)

        val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Niste prijavljeni!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            fetchQuizHistory(userId)
        }

        backToMenuButton.setOnClickListener {
            val intent = Intent(this, GameModeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun fetchQuizHistory(userId: Int) {
        val credentials = Credentials.basic("aplikatori", "nA7:B&")
        val request = Request.Builder()
            .url("http://157.230.8.219/quizify/get_quiz_history.php?user_id=$userId")
            .addHeader("Authorization", credentials)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@QuizHistoryActivity, "Greška pri dohvaćanju podataka", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("QuizHistoryActivity", "Server response: $responseBody")

                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.getString("status") == "success") {
                            val historyArray = jsonResponse.getJSONArray("data")
                            val historyList = mutableListOf<String>()

                            for (i in 0 until historyArray.length()) {
                                val quiz = historyArray.getJSONObject(i)
                                val category = quiz.getString("category")
                                val difficulty = quiz.getString("difficulty")
                                val score = quiz.getInt("score")
                                val date = quiz.getString("date")

                                historyList.add("Category: $category | Difficulty: $difficulty | Score: $score | Date: $date")
                            }

                            runOnUiThread {
                                val adapter = ArrayAdapter(
                                    this@QuizHistoryActivity,
                                    android.R.layout.simple_list_item_1,
                                    historyList
                                )
                                listViewHistory.adapter = adapter
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@QuizHistoryActivity, "Nema dostupnih podataka", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("QuizHistoryActivity", "JSON Parsing Error: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(this@QuizHistoryActivity, "Greška pri parsiranju podataka", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}
