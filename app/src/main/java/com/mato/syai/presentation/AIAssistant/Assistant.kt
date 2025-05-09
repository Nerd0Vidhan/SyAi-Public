package com.mato.syai.presentation.AIAssistant

import com.google.ai.client.generativeai.GenerativeModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mato.syai.core.AIassistant

object GeminiProvider {
    private val API_KEY = AIassistant.getAPIKEY()

    val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = API_KEY
    )
}

suspend fun generateText(prompt: String): String {
    return try {
        val response = GeminiProvider.generativeModel.generateContent(prompt)
        response.text ?: "No response"
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}



class GeminiTextViewModel : ViewModel() {

    private val _generatedText = MutableStateFlow("Waiting for prompt...")
    val generatedText: StateFlow<String> = _generatedText

    fun fetchText(prompt: String) {
        viewModelScope.launch {
            _generatedText.value = "Generating..."
            _generatedText.value = generateText(prompt)
        }
    }
}



@Composable
fun GeminiTextGeneratorUI(viewModel: GeminiTextViewModel = viewModel()) {
    var prompt by remember { mutableStateOf("") }
    val response by viewModel.generatedText.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Enter prompt") }
        )
        Button(onClick = { viewModel.fetchText(prompt) }) {
            Text("Generate")
        }
        Text(text = response)
    }
}

@Preview(showBackground = true)
@Composable
fun GeminiTextGeneratorUIPreview() {
    GeminiTextGeneratorUI()
}

//
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.remember
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.tooling.preview.Preview
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.compose.setContent
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.*
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextField
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.lifecycleScope
//import com.google.ai.client.generativeai.GenerativeModel
////import com.google.ai.client.generativeai.GenerativeModelConfig
////import com.google.ai.client.generativeai.GenerationConfig
////import com.google.ai.client.generativeai.SafetySetting
//import com.google.ai.client.generativeai.type.content
//import kotlinx.coroutines.launch
//import com.mato.syai.core.model.GeminiViewModel
//
////@Composable
////fun GeminiScreen(viewModel: GeminiViewModel = viewModel()) {
////    val response by viewModel.response
////    var prompt by remember { mutableStateOf("") }
////
////    Column(modifier = Modifier.padding(16.dp)) {
////        OutlinedTextField(
////            value = prompt,
////            onValueChange = { prompt = it },
////            label = { Text("Enter your prompt") },
////            modifier = Modifier.fillMaxWidth()
////        )
////        Spacer(modifier = Modifier.height(8.dp))
////        Button(onClick = { viewModel.generateText(prompt) }) {
////            Text("Generate Text")
////        }
////        Spacer(modifier = Modifier.height(16.dp))
////        Text("Response:", style = MaterialTheme.typography.titleMedium)
////        Text(response)
////    }
////}
//
//@Composable
//fun GeminiIntegrationScreen() {
//    var inputText by remember { mutableStateOf("") }
//    var outputText by remember { mutableStateOf("") }
//    var loading by remember { mutableStateOf(false) }
//    val context = LocalContext.current
//
//    // Replace with your actual API key
//    val apiKey = "AIzaSyA1bhWoYX9YnBIxwZSV-mOuGP4GCvFcpHA"
//
//    // Initialize GenerativeModel (ensure you have the com.google.ai.client.generativeai dependency)
//    val generativeModel = remember {
//        GenerativeModel(
//            modelName = "gemini-pro", // Or "gemini-pro-vision" for multimodal
//            apiKey = apiKey,
////            generationConfig = GenerationConfig(
////                temperature = 0.9f,
////                topK = 1,
////                topP = 1f,
////                maxOutputTokens = 2048
////            ),
////            safetySettings = listOf(
////                SafetySetting(SafetySetting.HarmCategory.HARASSMENT, SafetySetting.HarmBlockThreshold.BLOCK_NONE),
////                SafetySetting(SafetySetting.HarmCategory.HATE_SPEECH, SafetySetting.HarmBlockThreshold.BLOCK_NONE),
////                SafetySetting(SafetySetting.HarmCategory.SEXUALLY_EXPLICIT, SafetySetting.HarmBlockThreshold.BLOCK_NONE),
////                SafetySetting(SafetySetting.HarmCategory.DANGEROUS_CONTENT, SafetySetting.HarmBlockThreshold.BLOCK_NONE)
////            )
//        )
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        TextField(
//            value = inputText,
//            onValueChange = { inputText = it },
//            label = { Text("Enter your prompt") },
//            modifier = Modifier.fillMaxWidth()
//        )
//        Button(
//            onClick = {
//                if (inputText.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY") {
//                    loading = true
//                    outputText = "Loading..."
////                    (context as? MainActivity)?.lifecycleScope?.launch {
//                        try {
//                            val response = generativeModel.generateContent(content {
//                                text(inputText)
//                            })
//                            outputText = response.text ?: "No response"
//                        } catch (e: Exception) {
//                            outputText = "Error: ${e.localizedMessage}"
//                        } finally {
//                            loading = false
////                        }
//                    }
//                } else if (apiKey == "YOUR_GEMINI_API_KEY") {
//                    outputText = "Please replace 'YOUR_GEMINI_API_KEY' with your actual API key."
//                } else {
//                    outputText = "Please enter a prompt."
//                }
//            },
//            enabled = !loading
//        ) {
//            Text(if (loading) "Loading..." else "Generate")
//        }
//        Text(
//            text = "Response:",
////            style = MaterialTheme.typography.h6
//        )
//        Text(outputText)
//    }
//}
//
//
//@Preview(showBackground = true)
//@Composable
//fun GeminiScreenPreview() {
//    GeminiScreen()
//}

//import androidx.compose.runtime.livedata.observeAsState
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import retrofit2.Call
//import retrofit2.http.GET
//import retrofit2.http.Query
//
//interface GeminiApiService {
//
//    // Example API Endpoint to get current prices (replace with actual Gemini API endpoints)
//    @GET("v2/marketdata/btcusd/price")
//    fun getBTCPrice(): Call<GeminiResponse>
//}
//
//data class GeminiResponse(
//    val symbol: String,
//    val price: String
//)
//
//import okhttp3.OkHttpClient
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//
//object ApiClient {
//
//    private const val BASE_URL = "https://api.gemini.com/"
//
//    private val retrofit: Retrofit = Retrofit.Builder()
//        .baseUrl(BASE_URL)
//        .client(OkHttpClient())
//        .addConverterFactory(GsonConverterFactory.create())
//        .build()
//
//    val geminiApiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)
//}
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.liveData
//import retrofit2.Callback
//import retrofit2.Response
//
//class GeminiViewModel : ViewModel() {
//
//    val btcPrice = liveData {
//        val call = ApiClient.geminiApiService.getBTCPrice()
//
//        call.enqueue(object : Callback<GeminiResponse> {
//            override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
//                if (response.isSuccessful) {
//                    val price = response.body()?.price
//                    emit(price ?: "Error: No price")
//                } else {
//                    emit("Error: ${response.message()}")
//                }
//            }
//
//            override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
//                emit("Failure: ${t.message}")
//            }
//        })
//    }
//}
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Text
//import com.example.geminiapp.ui.theme.GeminiAppTheme
//import androidx.compose.foundation.layout.fillMaxSize
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            GeminiAppTheme {
//                // Call your composable here
//                DisplayBTCPrice()
//            }
//        }
//    }
//}
//
//@Composable
//fun DisplayBTCPrice(geminiViewModel: GeminiViewModel = viewModel()) {
//    val price = geminiViewModel.btcPrice.observeAsState("Loading...")
//
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(text = "Current BTC Price (Gemini API):")
//        Spacer(modifier = Modifier.height(16.dp))
//        Text(text = price.value ?: "Error fetching price")
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    GeminiAppTheme {
//        DisplayBTCPrice()
//    }
//}
