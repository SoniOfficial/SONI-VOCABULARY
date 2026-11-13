package tj.soni.vocabulary

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LexioSplashScreen() {
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        started = true
    }

    val logoScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.72f,
        animationSpec = tween(650),
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(450),
        label = "logoAlpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(650, delayMillis = 180),
        label = "textAlpha"
    )

    val textScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.75f,
        animationSpec = tween(650, delayMillis = 180),
        label = "textScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF100B20)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_round),
                contentDescription = "LEXIO",
                modifier = Modifier
                    .size(104.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "lexio",
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = 50.sp,
                color = Color(0xFFE0CCFF),
                modifier = Modifier
                    .alpha(textAlpha)
                    .scale(textScale)
            )
        }
    }
}
