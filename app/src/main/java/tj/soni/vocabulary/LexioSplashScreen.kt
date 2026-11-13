package tj.soni.vocabulary

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LexioSplashScreen() {
    val transition = rememberInfiniteTransition(label = "lexio_logo")

    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glow by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF100B20),
                        Color(0xFF21143D),
                        Color(0xFF100B20)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .scale(pulse)
                    .background(
                        Color(0xFF9B72FF).copy(alpha = glow * 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(125.dp)
                        .background(
                            Color(0xFF302052),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "LEXIO",
                        tint = Color(0xFFB99AFF),
                        modifier = Modifier.size(82.dp)
                    )

                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD66B),
                        modifier = Modifier
                            .size(34.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 5.dp, y = (-5).dp)
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            Text(
                text = "LEXIO",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 7.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Калима омӯз, ҷаҳонро фатҳ кун!",
                color = Color(0xFFC8B8E8),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(48.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(110.dp)
                    .height(3.dp),
                color = Color(0xFF9B72FF),
                trackColor = Color(0xFF49366A)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Омодасозӣ...",
                color = Color(0xFFB8ADC9),
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
