package com.example.quizifyrampu
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class QuizActivity : AppCompatActivity() {

    private lateinit var tvScore: TextView
    private lateinit var tvQuestionCounter: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var btnAnswer1: Button
    private lateinit var btnAnswer2: Button
    private lateinit var btnAnswer3: Button
    private lateinit var btnAnswer4: Button
    private lateinit var backButton: ImageView
    private lateinit var btnExit: ImageView
    private lateinit var btnHome: ImageView
    private lateinit var btnProfile: ImageView

    private val client = OkHttpClient()
    private var questions: List<Question> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0
    private var correctAnswers = 0
    private val totalQuestions = 10
    private var firstQuestion: String = "N/A"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        tvScore = findViewById(R.id.tv_score)
        tvQuestionCounter = findViewById(R.id.tv_question_counter)
        tvQuestion = findViewById(R.id.tv_question)
        btnAnswer1 = findViewById(R.id.btn_answer1)
        btnAnswer2 = findViewById(R.id.btn_answer2)
        btnAnswer3 = findViewById(R.id.btn_answer3)
        btnAnswer4 = findViewById(R.id.btn_answer4)
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

        val tvCategory = findViewById<TextView>(R.id.tv_category)
        val category = intent.getStringExtra("category") ?: "General Knowledge"
        tvCategory.text = category

        val difficulty = intent.getStringExtra("difficulty") ?: "easy"
        Log.d("QuizActivity", "Category: $category, Difficulty: $difficulty")

        fetchQuestions(category, difficulty)
        setButtonClickListeners()

        updateScoreView()
        updateQuestionCounterView()

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



    private fun setButtonClickListeners() {
        btnAnswer1.setOnClickListener { checkAnswer(btnAnswer1.text.toString()) }
        btnAnswer2.setOnClickListener { checkAnswer(btnAnswer2.text.toString()) }
        btnAnswer3.setOnClickListener { checkAnswer(btnAnswer3.text.toString()) }
        btnAnswer4.setOnClickListener { checkAnswer(btnAnswer4.text.toString()) }
    }


    private fun fetchQuestions(category: String, difficulty: String) {
        val combinedQuestions = mutableListOf<Question>()
        val serverUrl = "http://157.230.8.219/quizify/get_questions.php?category=$category&difficulty=$difficulty"

        val credentials = Credentials.basic("aplikatori", "nA7:B&")
        val serverRequest = Request.Builder()
            .url(serverUrl)
            .addHeader("Authorization", credentials)
            .build()

        client.newCall(serverRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("QuizActivity", "Error fetching questions from API", e)
                runOnUiThread { showErrorToast("Neuspjelo dohvaćanje pitanja sa servera") }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseString = response.body?.string() ?: ""
                    Log.d("QuizActivity", "Server response: $responseString")

                    if (!response.isSuccessful) {
                        runOnUiThread { showErrorToast("Greška: ${response.code}") }
                        return
                    }

                    val json = JSONObject(responseString)
                    val results = json.getJSONArray("questions")

                    for (i in 0 until results.length()) {
                        val questionObj = results.getJSONObject(i)
                        val question = questionObj.optString("text", "")
                        val correctAnswer = questionObj.optString("correct_answer", "")
                        val answers = mutableListOf(
                            correctAnswer,
                            questionObj.optString("incorrect_answer1", ""),
                            questionObj.optString("incorrect_answer2", ""),
                            questionObj.optString("incorrect_answer3", "")
                        ).filter { it.isNotEmpty() }.shuffled()

                        if (question.isNotEmpty() && correctAnswer.isNotEmpty()) {
                            combinedQuestions.add(Question(question, correctAnswer, answers))
                        }
                    }

                    val questionsFromDatabase = combinedQuestions.size
                    val remainingQuestions = totalQuestions - questionsFromDatabase

                    if (remainingQuestions > 0) {
                        loadOpenTdbQuestions(category, difficulty, remainingQuestions, combinedQuestions)
                    } else {
                        runOnUiThread {
                            questions = combinedQuestions
                            currentQuestionIndex = 0
                            updateUI(questions[currentQuestionIndex])
                        }
                    }

                } catch (e: Exception) {
                    Log.e("QuizActivity", "Error parsing server questions", e)
                    runOnUiThread { showErrorToast("Greška pri obradi podataka sa servera") }
                }
            }
        })
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(this@QuizActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun loadOpenTdbQuestions(
        category: String,
        difficulty: String,
        remainingQuestions: Int,
        combinedQuestions: MutableList<Question>
    ) {
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
            else -> "9"
        }
        val openTdbUrl = "https://opentdb.com/api.php?amount=$remainingQuestions&category=$categoryId&difficulty=$difficulty&type=multiple"
        val openTdbRequest = Request.Builder().url(openTdbUrl).build()

        client.newCall(openTdbRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("QuizActivity", "Error fetching questions from OpenTDB", e)
                runOnUiThread { showErrorToast("Greška pri dohvaćanju pitanja s OpenTDB") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val json = JSONObject(responseBody.string())
                    val results = json.getJSONArray("results")

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

                        combinedQuestions.add(Question(question, correctAnswer, answers))
                    }

                    runOnUiThread {
                        questions = combinedQuestions
                        currentQuestionIndex = 0
                        updateUI(questions[currentQuestionIndex])
                    }
                }
            }
        })
    }

    private fun updateUI(question: Question) {
        if (firstQuestion == "N/A") firstQuestion = question.text
        tvQuestion.text = question.text
        btnAnswer1.text = question.answers[0]
        btnAnswer2.text = question.answers[1]
        btnAnswer3.text = question.answers[2]
        btnAnswer4.text = question.answers[3]
        setButtonsEnabled(true)
    }

    private fun checkAnswer(selectedAnswer: String) {
        setButtonsEnabled(false)

        val correctAnswer = questions[currentQuestionIndex].correctAnswer

        when (correctAnswer) {
            btnAnswer1.text -> btnAnswer1.setBackgroundResource(R.drawable.answer_correct)
            btnAnswer2.text -> btnAnswer2.setBackgroundResource(R.drawable.answer_correct)
            btnAnswer3.text -> btnAnswer3.setBackgroundResource(R.drawable.answer_correct)
            btnAnswer4.text -> btnAnswer4.setBackgroundResource(R.drawable.answer_correct)
        }

        if (selectedAnswer != correctAnswer) {
            when (selectedAnswer) {
                btnAnswer1.text -> btnAnswer1.setBackgroundResource(R.drawable.answer_incorrect)
                btnAnswer2.text -> btnAnswer2.setBackgroundResource(R.drawable.answer_incorrect)
                btnAnswer3.text -> btnAnswer3.setBackgroundResource(R.drawable.answer_incorrect)
                btnAnswer4.text -> btnAnswer4.setBackgroundResource(R.drawable.answer_incorrect)
            }
        } else {
            score += getScoreForDifficulty(intent.getStringExtra("difficulty") ?: "easy")
            correctAnswers++
            updateScoreView()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            resetButtonColors()
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                updateUI(questions[currentQuestionIndex])
                updateQuestionCounterView()
            } else {
                finishQuiz()
            }
        }, 1000)
    }

    private fun getScoreForDifficulty(difficulty: String): Int {
        return when (difficulty) {
            "easy" -> 100
            "medium" -> 500
            "hard" -> 1000
            else -> 100
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnAnswer1.isEnabled = enabled
        btnAnswer2.isEnabled = enabled
        btnAnswer3.isEnabled = enabled
        btnAnswer4.isEnabled = enabled
    }

    private fun resetButtonColors() {
        btnAnswer1.setBackgroundResource(R.drawable.button_with_border)
        btnAnswer2.setBackgroundResource(R.drawable.button_with_border)
        btnAnswer3.setBackgroundResource(R.drawable.button_with_border)
        btnAnswer4.setBackgroundResource(R.drawable.button_with_border)
    }

    private fun decodeHtml(html: String): String {
        return html.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&uuml;", "ü")
            .replace("&Ouml;", "Ö")
            .replace("&#039;", "'")
            .replace("&eacute;", "é")
            .replace("&Eacute;", "É")
            .replace("&agrave;", "à")
            .replace("&Agrave;", "À")
            .replace("&aacute;", "á")
            .replace("&Aacute;", "Á")
            .replace("&acirc;", "â")
            .replace("&Acirc;", "Â")
            .replace("&atilde;", "ã")
            .replace("&Atilde;", "Ã")
            .replace("&auml;", "ä")
            .replace("&Auml;", "Ä")
            .replace("&aring;", "å")
            .replace("&Aring;", "Å")
            .replace("&ccedil;", "ç")
            .replace("&Ccedil;", "Ç")
            .replace("&egrave;", "è")
            .replace("&Egrave;", "È")
            .replace("&euml;", "ë")
            .replace("&Euml;", "Ë")
            .replace("&iacute;", "í")
            .replace("&Iacute;", "Í")
            .replace("&icirc;", "î")
            .replace("&Icirc;", "Î")
            .replace("&iuml;", "ï")
            .replace("&Iuml;", "Ï")
            .replace("&oacute;", "ó")
            .replace("&Oacute;", "Ó")
            .replace("&ocirc;", "ô")
            .replace("&Ocirc;", "Ô")
            .replace("&otilde;", "õ")
            .replace("&Otilde;", "Õ")
            .replace("&ouml;", "ö")
            .replace("&Ouml;", "Ö")
            .replace("&uacute;", "ú")
            .replace("&Uacute;", "Ú")
            .replace("&ucirc;", "û")
            .replace("&Ucirc;", "Û")
            .replace("&uuml;", "ü")
            .replace("&Uuml;", "Ü")
            .replace("&yacute;", "ý")
            .replace("&Yacute;", "Ý")
            .replace("&yuml;", "ÿ")
            .replace("&Yuml;", "Ÿ")
            .replace("&ntilde;", "ñ")
            .replace("&Ntilde;", "Ñ")
            .replace("&szlig;", "ß")
            .replace("&euro;", "€")
            .replace("&pound;", "£")
            .replace("&yen;", "¥")
            .replace("&copy;", "©")
            .replace("&reg;", "®")
            .replace("&trade;", "™")
            .replace("&deg;", "°")
            .replace("&laquo;", "«")
            .replace("&raquo;", "»")
            .replace("&laquo;", "«")
            .replace("&bull;", "•")
            .replace("&prime;", "′")
            .replace("&Prime;", "″")
            .replace("&lsquo;", "‘")
            .replace("&rsquo;", "’")
            .replace("&sbquo;", "‚")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .replace("&bdquo;", "„")
            .replace("&ndash;", "–")
            .replace("&mdash;", "—")
            .replace("&lsquor;", "‘")
            .replace("&rsquor;", "’")
            .replace("&#8211;", "–")
            .replace("&#8212;", "—")
            .replace("&#8220;", "“")
            .replace("&#8221;", "”")
            .replace("&#8249;", "‹")
            .replace("&#8250;", "›")
            .replace("&#8364;", "€")
            .replace("&#8482;", "™")
            .replace("&#169;", "©")
            .replace("&#174;", "®")
    }

    @SuppressLint("SetTextI18n")
    private fun updateScoreView() {
        tvScore.text = "Bodovi: $score"
    }

    @SuppressLint("SetTextI18n")
    private fun updateQuestionCounterView() {
        tvQuestionCounter.text = "Pitanje ${currentQuestionIndex + 1}/$totalQuestions"
    }

    private fun finishQuiz() {
        val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "You are not logged in. Results will not be saved.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, GameModeActivity::class.java))
            finish()
            return
        }

        val category = intent.getStringExtra("category") ?: "General Knowledge"
        val difficulty = intent.getStringExtra("difficulty") ?: "easy"
        val date = System.currentTimeMillis() // Trenutni timestamp

        val json = JSONObject().apply {
            put("user_id", userId)
            put("score", score)
            put("category", category)
            put("difficulty", difficulty)
            put("date", date)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        val credentials = Credentials.basic("aplikatori", "nA7:B&")

        // 1. Prvo ažuriraj ukupan score u `update_score.php`
        val scoreRequest = Request.Builder()
            .url("http://157.230.8.219/quizify/update_score.php")
            .addHeader("Authorization", credentials)
            .post(body)
            .build()

        client.newCall(scoreRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("QuizActivity", "Failed to update score: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@QuizActivity, "Error updating score", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("QuizActivity", "Failed to update score: HTTP ${response.code}")
                }
            }
        })

        // 2. Zatim spremi podatke u `history` tablicu putem `update_quiz_history.php`
        val historyRequest = Request.Builder()
            .url("http://157.230.8.219/quizify/update_quiz_history.php")
            .addHeader("Authorization", credentials)
            .post(body)
            .build()

        client.newCall(historyRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("QuizActivity", "Failed to save quiz history: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@QuizActivity, "Error saving quiz history", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("QuizActivity", "Quiz history successfully saved")
                    runOnUiThread {
                        Toast.makeText(this@QuizActivity, "Quiz history saved!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("QuizActivity", "Failed to save quiz history: HTTP ${response.code}")
                }
            }
        })


        val intent = Intent(this, GameModeActivity::class.java)
        startActivity(intent)
        finish()
    }





    private fun submitResults(
        userId: Int, category: String, correctAnswers: Int,
        totalQuestions: Int, mostAnsweredQuestion: String
    ) {
        val json = JSONObject().apply {
            put("user_id", userId)
            put("category", category)
            put("correct_answers", correctAnswers)
            put("total_questions", totalQuestions)
            put("most_answered_question", mostAnsweredQuestion)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        val credentials = Credentials.basic("aplikatori", "nA7:B&")

        val request = Request.Builder()
            .url("http://157.230.8.219/quizify/submit_quiz.php")
            .addHeader("Authorization", credentials)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("QuizActivity", "Greška pri slanju rezultata: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("QuizActivity", "Rezultati poslani: ${response.body?.string()}")
            }
        })
    }
    data class Question(val text: String, val correctAnswer: String, val answers: List<String>)
}