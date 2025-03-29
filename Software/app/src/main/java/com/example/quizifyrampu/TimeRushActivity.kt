package com.example.quizifyrampu

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class TimeRushActivity : AppCompatActivity() {

    private lateinit var tvQuestion: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvScore: TextView
    private lateinit var btnAnswer1: Button
    private lateinit var btnAnswer2: Button
    private lateinit var btnAnswer3: Button
    private lateinit var btnAnswer4: Button
    private lateinit var progressTimer: ProgressBar
    private lateinit var backButton: ImageView
    private lateinit var btnExit: ImageView
    private lateinit var btnHome: ImageView
    private lateinit var btnProfile: ImageView

    private val client = OkHttpClient()
    private var questions: List<QuizActivity.Question> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0
    private lateinit var countDownTimer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_rush)

        tvQuestion = findViewById(R.id.tv_question)
        tvTimer = findViewById(R.id.tv_timer)
        tvScore = findViewById(R.id.tv_score)
        btnAnswer1 = findViewById(R.id.btn_answer1)
        btnAnswer2 = findViewById(R.id.btn_answer2)
        btnAnswer3 = findViewById(R.id.btn_answer3)
        btnAnswer4 = findViewById(R.id.btn_answer4)
        progressTimer = findViewById(R.id.progress_timer)
        btnExit = findViewById(R.id.btn_exit)
        btnHome = findViewById(R.id.btn_home)
        btnProfile = findViewById(R.id.btn_profile)
        backButton = findViewById(R.id.btn_back)

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


        val category = intent.getStringExtra("category") ?: "9"
        val difficulty = intent.getStringExtra("difficulty") ?: "easy"

        fetchQuestions(category, difficulty)
        setButtonClickListeners()
    }

    private fun setButtonClickListeners() {
        btnAnswer1.setOnClickListener { checkAnswer(btnAnswer1.text.toString(), btnAnswer1) }
        btnAnswer2.setOnClickListener { checkAnswer(btnAnswer2.text.toString(), btnAnswer2) }
        btnAnswer3.setOnClickListener { checkAnswer(btnAnswer3.text.toString(), btnAnswer3) }
        btnAnswer4.setOnClickListener { checkAnswer(btnAnswer4.text.toString(), btnAnswer4) }
    }

    private fun fetchQuestions(category: String, difficulty: String) {
        val categoryId = when (category) {
            "Movies" -> "11"
            "Games" -> "15"
            "History" -> "23"
            "Geography" -> "22"
            "Music" -> "12"
            "Physics" -> "17"
            "Biology" -> "17"
            "General Knowledge" -> "9"
            "Sports" -> "21"
            "IT" -> "18"
            "Cars" -> "28"
            "All Categories" -> "9"
            else -> "9"
        }

        val url = if (difficulty == "random") {
            "https://opentdb.com/api.php?amount=50&category=$categoryId&type=multiple"
        } else {
            "https://opentdb.com/api.php?amount=50&category=$categoryId&difficulty=$difficulty&type=multiple"
        }

        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TimeRushActivity", "Neuspjelo dohvaćanje pitanja", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("TimeRushActivity", "Odgovor neuspješan: ${response.code}")
                    return
                }

                response.body?.let { responseBody ->
                    val json = JSONObject(responseBody.string())
                    val results = json.getJSONArray("results")
                    val questionList = mutableListOf<QuizActivity.Question>()

                    for (i in 0 until results.length()) {
                        val questionObj = results.getJSONObject(i)
                        val question = decodeHtml(questionObj.getString("question"))
                        val correctAnswer = decodeHtml(questionObj.getString("correct_answer"))
                        val incorrectAnswers = questionObj.getJSONArray("incorrect_answers")
                        val answers = mutableListOf(correctAnswer)

                        for (j in 0 until incorrectAnswers.length()) {
                            answers.add(decodeHtml(incorrectAnswers.getString(j)))
                        }
                        answers.shuffle()

                        questionList.add(QuizActivity.Question(question, correctAnswer, answers))
                    }

                    questions = questionList

                    runOnUiThread {
                        if (questions.isNotEmpty()) {
                            startGame()
                        }
                    }
                } ?: Log.e("TimeRushActivity", "Odgovor u body je prazan")
            }
        })
    }

    private fun startGame() {
        currentQuestionIndex = 0
        score = 0
        updateScoreView()
        updateUI(questions[currentQuestionIndex])
        startTimer()
    }

    @SuppressLint("SetTextI18n")
    private fun startTimer() {
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                tvTimer.text = "$seconds"
                progressTimer.progress = (millisUntilFinished.toFloat() / 60000 * 100).toInt()
            }

            override fun onFinish() {
                endGame()
            }
        }.start()
    }

    private fun updateUI(question: QuizActivity.Question) {
        tvQuestion.text = question.text
        btnAnswer1.text = question.answers[0]
        btnAnswer2.text = question.answers[1]
        btnAnswer3.text = question.answers[2]
        btnAnswer4.text = question.answers[3]
        setButtonsEnabled(true)
    }

    private fun checkAnswer(selectedAnswer: String, selectedButton: Button) {
        setButtonsEnabled(false)

        val correctAnswer = questions[currentQuestionIndex].correctAnswer

        if (selectedAnswer == correctAnswer) {
            score += 100
            selectedButton.setBackgroundResource(R.drawable.answer_correct)
        } else {
            score = maxOf(0, score - 100) // Ensure score does not drop below 0
            selectedButton.setBackgroundResource(R.drawable.answer_incorrect)
            highlightCorrectAnswer(correctAnswer)
        }

        updateScoreView()

        // Move to the next question
        currentQuestionIndex++

        Handler(mainLooper).postDelayed({
            resetButtonBackgrounds()
            if (currentQuestionIndex < questions.size) {
                updateUI(questions[currentQuestionIndex])
            } else {
                endGame() // End the game if all questions are answered
            }
        }, 1000)
    }


    private fun resetButtonBackgrounds() {
        btnAnswer1.setBackgroundResource(R.drawable.button_with_border)
        btnAnswer2.setBackgroundResource(R.drawable.button_with_border)
        btnAnswer3.setBackgroundResource(R.drawable.button_with_border)
        btnAnswer4.setBackgroundResource(R.drawable.button_with_border)
    }

    private fun highlightCorrectAnswer(correctAnswer: String) {
        when (correctAnswer) {
            btnAnswer1.text -> btnAnswer1.setBackgroundResource(R.drawable.answer_correct)
            btnAnswer2.text -> btnAnswer2.setBackgroundResource(R.drawable.answer_correct)
            btnAnswer3.text -> btnAnswer3.setBackgroundResource(R.drawable.answer_correct)
            btnAnswer4.text -> btnAnswer4.setBackgroundResource(R.drawable.answer_correct)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateScoreView() {
        tvScore.text = "Bodovi: $score"
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnAnswer1.isEnabled = enabled
        btnAnswer2.isEnabled = enabled
        btnAnswer3.isEnabled = enabled
        btnAnswer4.isEnabled = enabled
    }

    private fun endGame() {
        countDownTimer.cancel()

        // Prikaz poruke o završetku igre i rezultata
        Toast.makeText(
            this,
            "Game Over! Your score: $score",
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(this, GameModeActivity::class.java)
        intent.putExtra("score", score)
        startActivity(intent)
        finish()
    }


    private fun decodeHtml(html: String): String {
        return html.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#039;", "'")
    }
}
