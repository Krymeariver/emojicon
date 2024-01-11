import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun WearApp() {
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
                    val endX = (canvasCenter.x + lineRadius * Math.cos(angle)).toFloat()
                    val endY = (canvasCenter.y + lineRadius * Math.sin(angle)).toFloat()
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
