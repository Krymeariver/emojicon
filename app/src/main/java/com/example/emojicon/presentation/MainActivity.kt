package com.example.emojicon.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "wearApp") {
                composable("wearApp") { WearApp(navController) }
                composable("selection/{iconIndex}") { backStackEntry ->
                    SelectionScreen(navController, backStackEntry.arguments?.getString("iconIndex")?.toInt() ?: 0)
                }
            }
        }
    }
}

//@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun WearApp(navController: NavController) {
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
                Box(
                    modifier = Modifier
                        .offset(x = position.second - canvasWidth / 2, y = position.first - canvasHeight / 2)
                        .clickable { navController.navigate("selection/${index + 1}") }
                        .background(Color.Transparent)
                ) {
                    Text(
                        text = "+",
                        color = Color.White,
                        fontSize = 30.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }


        }
    }
}

@Composable
fun SelectionScreen(navController: NavController, iconIndex: Int) {
    // Handle the back button press
    BackHandler {
        navController.navigateUp() // Navigate back to the previous screen
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
                    SelectionItem(text = "Text", onClickFunction = { /* Handle Text Click */ })
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
