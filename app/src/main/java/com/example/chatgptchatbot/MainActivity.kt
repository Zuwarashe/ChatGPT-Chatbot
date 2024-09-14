package com.example.chatgptchatbot

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inQuestion = findViewById<EditText>(R.id.Question)
        val button = findViewById<Button>(R.id.SubmitBtn)
        val responseView = findViewById<TextView>(R.id.Response)

        button.setOnClickListener {
            val question = inQuestion.text.toString()
            Log.d("ChatGPT", "User question: $question")

            getResponse(question) { response ->
                runOnUiThread {
                    responseView.text = response
                    Log.d("ChatGPT", "Response: $response")
                }
            }
        }
    }

    fun getResponse(question: String, callback: (String) -> Unit) {
        val url = "https://api.openai.com/v1/chat/completions"
        val apiKey = ""

        // Create JSON request body for GPT-3.5 Turbo
        val requestBody = """
    {
      "model": "gpt-3.5-turbo",
      "messages": [{"role": "user", "content": "$question"}],
      "max_tokens": 100,
      "temperature": 0.7
    }
    """.trimIndent()

        val mediaType = "application/json".toMediaType()
        val body = requestBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        Log.d("ChatGPT", "Sending request to OpenAI...")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ChatGPT", "Request failed: ${e.message}")
                callback("Failed to get response: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val statusCode = response.code
                Log.d("ChatGPT", "Response received. Status code: $statusCode")

                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    Log.d("ChatGPT", "Raw response data: $responseData")

                    try {
                        val completion = JSONObject(responseData!!)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        callback(completion)
                    } catch (e: Exception) {
                        Log.e("ChatGPT", "Failed to parse response: ${e.message}")
                        callback("Error parsing response: ${e.message}")
                    }
                } else {
                    val errorBody = response.body?.string() ?: "No error body"
                    Log.e("ChatGPT", "Error from server. Status code: $statusCode, Body: $errorBody")
                    callback("Error: ${response.message}. Status code: $statusCode, Body: $errorBody")
                }
            }
        })
    }
}
