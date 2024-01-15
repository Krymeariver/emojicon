package com.example.emojicon.presentation

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: IconTextViewModel = viewModel()
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "wearApp") {
                composable("wearApp") { WearApp(navController, viewModel) }
                composable("selection/{iconIndex}") { backStackEntry ->
                    SelectionScreen(navController, backStackEntry.arguments?.getString("iconIndex")?.toInt() ?: 0)
                }
                composable("textInput/{iconIndex}") { backStackEntry ->
                    val iconIndex = backStackEntry.arguments?.getString("iconIndex")?.toInt() ?: 0
                    TextInputScreen(navController, viewModel, iconIndex)
                }
                composable("fullScreenDisplay/{content}") { backStackEntry ->
                    val content = backStackEntry.arguments?.getString("content") ?: ""
                    FullScreenDisplayScreen(content, navController)
                }
            }
        }
    }
}

class IconTextViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private val _iconTexts = MutableStateFlow<Map<Int, String>>(loadSavedTexts())
    val iconTexts: StateFlow<Map<Int, String>> = _iconTexts.asStateFlow()

    init {
        // Call loadSavedTexts() during initialization to load saved data
        _iconTexts.value = loadSavedTexts()
    }

    private fun loadSavedTexts(): Map<Int, String> {
        val sharedPrefs = context.getSharedPreferences("icon_texts", Context.MODE_PRIVATE)
        val textsSet = sharedPrefs.getStringSet("texts", emptySet())

        // Convert the set to a map
        val textsMap = mutableMapOf<Int, String>()
        textsSet?.forEach { textEntry ->
            val parts = textEntry.split(":")
            if (parts.size == 2) {
                val key = parts[0].toInt()
                val value = parts[1]
                textsMap[key] = value
            }
        }

        return textsMap
    }



    fun updateTextForIcon(iconIndex: Int, text: String) {
        viewModelScope.launch {
            val updatedMap = _iconTexts.value.toMutableMap().apply {
                put(iconIndex, text)
            }
            _iconTexts.value = updatedMap
            saveTexts(updatedMap)
        }
    }

    private fun saveTexts(texts: Map<Int, String>) {
        val sharedPrefs = context.getSharedPreferences("icon_texts", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            // Clear previous data
            clear()

            // Convert the map to a set of strings
            val textsSet = texts.entries.map { "${it.key}:${it.value}" }.toSet()

            // Save the set
            putStringSet("texts", textsSet)

            apply()
        }
    }
}

//@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearApp(navController: NavController, viewModel: IconTextViewModel) {
    val iconTexts by viewModel.iconTexts.collectAsState()
    val canvasWidth = LocalConfiguration.current.screenWidthDp.dp
    val canvasHeight = LocalConfiguration.current.screenHeightDp.dp
    val triangleRadius = minOf(canvasWidth, canvasHeight) * 0.35f // 35% of the canvas size

    // Define the positions for each "+" icon as a percentage of the screen's width and height
    val positions = listOf(
        Pair(triangleRadius, canvasWidth * 0.6f - triangleRadius), // Position for icon 1
        Pair(triangleRadius * 0.6f, canvasWidth * 0.5f), // Position for icon 2
        Pair(triangleRadius, canvasWidth * 0.4f + triangleRadius), // Position for icon 3
        Pair(canvasHeight - triangleRadius, canvasWidth * 0.4f + triangleRadius), // Position for icon 4
        Pair(canvasHeight - triangleRadius * 0.6f, canvasWidth * 0.5f), // Position for icon 5
        Pair(canvasHeight - triangleRadius, canvasWidth * 0.6f - triangleRadius) // Position for icon 6
    )

    MaterialTheme { // Using default Material Theme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray), // Set the background color
            contentAlignment = Alignment.Center
        ) {
            // Drawing the 'X' and horizontal line
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasCenter = Offset(size.width / 2, size.height / 2)
                val lineStrokeWidth = 2.dp.toPx()
                val fullRadius =
                    size.minDimension / 2  // Use the smaller of the width/height to fit the circle in the view
                val lineRadius = fullRadius * 0.90f // 90% of the full radius

                // Draw horizontal line
                drawLine(
                    color = Color.White,
                    start = Offset(canvasCenter.x - lineRadius, canvasCenter.y),
                    end = Offset(canvasCenter.x + lineRadius, canvasCenter.y),
                    strokeWidth = lineStrokeWidth
                )

                // Calculate the end points for the 'X' lines at 60-degree angles
                val angleIncrement = 60.0
                for (i in 0..5) {
                    val angle = Math.toRadians(angleIncrement * i)
                    val endX = (canvasCenter.x + lineRadius * cos(angle)).toFloat()
                    val endY = (canvasCenter.y + lineRadius * sin(angle)).toFloat()
                    drawLine(
                        color = Color.White,
                        start = canvasCenter,
                        end = Offset(endX, endY),
                        strokeWidth = lineStrokeWidth
                    )
                }
            }

            positions.forEachIndexed { index, position ->
                val iconIndex = index + 1
                val displayText =  iconTexts[iconIndex] ?: "+"
                val fontSize = dynamicFontSize(displayText)
                val numLines = numberOfLines(displayText)

                Box(
                    modifier = Modifier
                        .offset(
                            x = position.second - canvasWidth / 2,
                            y = position.first - canvasHeight / 2
                        )
                        .combinedClickable(
                            onClick = {
                                if (displayText == "+") {
                                    navController.navigate("selection/$iconIndex")
                                } else {
                                    navController.navigate("fullScreenDisplay/${displayText}")
                                }
                            },
                            onLongClick = { navController.navigate("selection/$iconIndex") }
                        )
                        .background(Color.Transparent)
                )  {
                    Text(
                        text = displayText,
                        color = Color.White,
                        fontSize = fontSize,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .widthIn(max = 55.dp), // Set a max width for text wrapping
                        maxLines = numLines,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center // Center align the text
                    )
                }
            }
        }
    }
}

fun dynamicFontSize(text: String): TextUnit {
    return when {
        text.length <= 5 -> 20.sp
        text.length <= 10 -> 17.sp
        text.length <= 20 -> 12.sp
        text.length <= 30 -> 8.sp
        else -> 5.sp
    }
}

fun numberOfLines(text: String): Int {
    return when {
        text.length <= 8 -> 1
        text.length <= 20 -> 2
        else -> 3
    }
}

@Composable
fun FullScreenDisplayScreen(content: String, navController: NavController) {
    val context = LocalContext.current
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Keep the screen on for 30 seconds when this screen is active
    val keepScreenOn = remember { mutableStateOf(true) }

    // Define the rotation angles
    var rotationZ by remember { mutableStateOf(0f) }

    val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null) {
                val gravity = event.values
                val x = gravity[0]
                val y = gravity[1]
                val z = gravity[2]

                // Calculate the rotation angle based on accelerometer values
                val angle = Math.toDegrees(Math.atan2(x.toDouble(), z.toDouble()))
                rotationZ = angle.toFloat()

                // Adjust the angle to the desired range (0 to 360 degrees)
                if (rotationZ < 0) {
                    rotationZ += 360f
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }

    // Start the screen timeout when the composable is first active
    DisposableEffect(Unit) {
        val handler = Handler()
        keepScreenOn.value = true
        handler.postDelayed({
            keepScreenOn.value = false
        }, 60000) // 30 seconds

        sensorManager.registerListener(
            accelerometerListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )

        onDispose {
            // Clean up when the composable is disposed
            handler.removeCallbacksAndMessages(null)
            sensorManager.unregisterListener(accelerometerListener)
        }
    }

    // Check if the screen should stay on
    if (keepScreenOn.value) {
        powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "MyApp:KeepScreenOn"
        ).apply {
            acquire(30_000) // 30 seconds
        }
    }

    val fontSize = when {
        content.length <= 3 -> 90.sp
        content.length <= 5 -> 70.sp
        content.length <= 10 -> 40.sp
        else -> 30.sp
    }

    BackHandler {
        navController.navigate("WearApp"); // Navigate back to the previous screen
    }

    // Apply rotation transformations to the Text composable
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = content,
            color = Color.White,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .graphicsLayer(
                    rotationZ = rotationZ
                )
        )
    }

    // Handle back navigation
    BackHandler {
        navController.navigate("WearApp")
    }
}

@Composable
fun SelectionScreen(navController: NavController, iconIndex: Int) {
    // Handle the back button press
    BackHandler {
        navController.navigate("WearApp"); // Navigate back to the previous screen
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = screenWidth * 0.73f // Limit the width to 80% of the screen width

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(maxWidth)
                    .padding(14.dp)
            ) {
                Text(
                    "Which one do you want to add as an emoji?",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 6.dp, start = 10.dp)
                )
                Row {
                    SelectionItem(text = "Text", onClickFunction = { navController.navigate("textInput/$iconIndex") })
                    Spacer(modifier = Modifier.width(8.dp))
                    SelectionItem(text = "Emoji", onClickFunction = { /* Handle Emoji Click */ })
                }
                Spacer(modifier = Modifier.height(1.dp))
                Row {
                    SelectionItem(text = "Image", onClickFunction = { /* Handle Image Click */ })
                    Spacer(modifier = Modifier.width(8.dp))
                    SelectionItem(text = "Cancel", onClickFunction = { navController.navigateUp() })
                }
            }
        }
    }
}

@Composable
fun SelectionItem(text: String, onClickFunction: () -> Unit) {
    Button(
        onClick = onClickFunction,
        modifier = Modifier
            .padding(vertical = 6.dp),
        // Button already provides a circular shape and clickable behavior
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp
        )
    }
    Spacer(modifier = Modifier.height(8.dp)) // Spacer for separating the items.
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputScreen(navController: NavController, viewModel: IconTextViewModel, iconIndex: Int) {
    val textState = remember { mutableStateOf("") }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = screenWidth * 0.7f // Limit the width to 70% of the screen width

    // Handle the back button press
    BackHandler {
        navController.navigateUp() // Navigate back to the previous screen
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black), // Black background color for consistency
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(maxWidth) // Apply maxWidth
                    .padding(top = 50.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = textState.value,
                    onValueChange = { textState.value = it },
                    label = { Text("Enter Text", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedPlaceholderColor = Color.Gray
                    ),
                    textStyle = TextStyle(color = Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {  viewModel.updateTextForIcon(iconIndex, textState.value)
                    navController.navigate("wearApp") }) {
                    Text("Submit", color = Color.White)
                }
            }
        }
    }
}


