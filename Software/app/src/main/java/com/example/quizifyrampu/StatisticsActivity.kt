package com.example.quizifyrampu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class StatisticsActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private var userId: Int = -1
    private lateinit var listView: ListView
    private lateinit var pieChart: PieChart
    private lateinit var backButton: ImageView
    private lateinit var btnExit: ImageView
    private lateinit var btnHome: ImageView
    private lateinit var btnProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        btnExit = findViewById(R.id.btn_exit)
        btnHome = findViewById(R.id.btn_home)
        btnProfile = findViewById(R.id.btn_profile)
        backButton = findViewById(R.id.btn_back)

        listView = findViewById(R.id.lv_statistics)
        pieChart = findViewById(R.id.pieChart)

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

        val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("user_id", -1)


        if (userId == -1) {
            Toast.makeText(this, "Not signed in. Unable to show statistics", Toast.LENGTH_LONG).show()
            finish()
        } else {
            fetchStatistics()
        }
    }

    private fun fetchStatistics() {
        val credentials = Credentials.basic("aplikatori", "nA7:B&")
        val request = Request.Builder()
            .url("http://157.230.8.219/quizify/get_user_statistics.php?user_id=$userId")
            .addHeader("Authorization", credentials)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("StatisticsActivity", "API Poziv nije uspio: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@StatisticsActivity, "Greška: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish() // Zatvaranje aktivnosti u slučaju greške
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("StatisticsActivity", "HTTP Kod odgovora: ${response.code}")
                Log.d("StatisticsActivity", "Odgovor s servera: $responseBody")

                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val jsonArray = jsonObject.optJSONArray("data")

                        if (jsonArray == null || jsonArray.length() == 0) {
                            // Ako nema podataka o statistici
                            runOnUiThread {
                                Toast.makeText(this@StatisticsActivity, "No available statistics for this user", Toast.LENGTH_LONG).show()
                                finish() // Zatvaranje aktivnosti
                            }
                            return
                        }

                        // Nastavi sa obradom statistike ako podaci postoje
                        val categoryMap = mutableMapOf<String, MutableMap<String, Any>>()
                        val questionCountMap = mutableMapOf<String, Int>()
                        val pieEntries = mutableListOf<PieEntry>()
                        var totalCorrectAnswers = 0
                        var mostFrequentQuestion = ""

                        for (i in 0 until jsonArray.length()) {
                            val stat = jsonArray.getJSONObject(i)
                            val category = stat.getString("category")
                            val correctAnswers = stat.getInt("correct_answers")
                            val totalQuestions = stat.getInt("total_questions")
                            val mostAnswered = stat.getString("most_answered_question")

                            questionCountMap[mostAnswered] = questionCountMap.getOrDefault(mostAnswered, 0) + 1

                            totalCorrectAnswers += correctAnswers

                            if (categoryMap.containsKey(category)) {
                                val existing = categoryMap[category]!!
                                existing["correct_answers"] = (existing["correct_answers"] as Int) + correctAnswers
                                existing["total_questions"] = (existing["total_questions"] as Int) + totalQuestions
                            } else {
                                categoryMap[category] = mutableMapOf(
                                    "correct_answers" to correctAnswers,
                                    "total_questions" to totalQuestions,
                                    "most_answered_question" to mostAnswered
                                )
                            }
                        }

                        var maxCount = 0
                        for ((question, count) in questionCountMap) {
                            if (count > maxCount) {
                                maxCount = count
                                mostFrequentQuestion = question
                            }
                        }

                        val dataList = mutableListOf<Map<String, String>>()
                        for ((category, data) in categoryMap) {
                            val correctAnswers = data["correct_answers"] as Int
                            val totalQuestions = data["total_questions"] as Int
                            val mostAnswered = data["most_answered_question"] as String
                            val percentage = (correctAnswers.toFloat() / totalQuestions * 100).toInt()

                            dataList.add(
                                mapOf(
                                    "Category" to category,
                                    "Correct Answers" to correctAnswers.toString(),
                                    "Total Questions" to totalQuestions.toString(),
                                    "Most Answered" to mostAnswered,
                                    "Percentage" to "$percentage%"
                                )
                            )
                            pieEntries.add(PieEntry(percentage.toFloat(), category))
                        }

                        runOnUiThread {
                            displayStatistics(dataList, totalCorrectAnswers, mostFrequentQuestion)
                            setupPieChart(pieEntries)
                        }
                    } catch (e: Exception) {
                        Log.e("StatisticsActivity", "Greška u obradi JSON odgovora: ${e.message}", e)
                        runOnUiThread {
                            Toast.makeText(this@StatisticsActivity, "Greška u obradi podataka", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                } else {
                    Log.e("StatisticsActivity", "Neuspješan odgovor: HTTP ${response.code}")
                    runOnUiThread {
                        Toast.makeText(this@StatisticsActivity, "Greška u dohvatu podataka: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        })
    }



    private fun displayStatistics(data: List<Map<String, String>>, totalCorrectAnswers: Int, mostFrequentQuestion: String) {
        val adapter = object : SimpleAdapter(
            this,
            data,
            R.layout.item_statistics,
            arrayOf("Category", "Correct Answers", "Most Answered", "Percentage"),
            intArrayOf(R.id.tvCategory, R.id.tvCorrectAnswers, R.id.tvMostAnswered, R.id.tvPercentage)
        ) {
            override fun setViewText(v: android.widget.TextView?, text: String?) {
                super.setViewText(v, text)
                if (v?.id == R.id.tvPercentage) {
                    v.setTextColor(resources.getColor(R.color.teal_700, theme))
                }
            }
        }
        listView.adapter = adapter

       // Toast.makeText(
       //     this, "Ukupan broj točnih odgovora: $totalCorrectAnswers\nNajčešće pitanje: $mostFrequentQuestion",
       //     Toast.LENGTH_LONG
       // ).show()
    }

    private fun setupPieChart(pieEntries: List<PieEntry>) {
        val summaryTextView: TextView = findViewById(R.id.tvSummary)

        summaryTextView.text = "Statistics by category: ${pieEntries.size} categories"

        val dataSet = PieDataSet(pieEntries,"")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)

        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.centerText = "Correct answers (%)"
        pieChart.setEntryLabelTextSize(12f)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}