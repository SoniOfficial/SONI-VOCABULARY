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
    val logoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(650),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(550),
        label = "logoAlpha"
    )
    val textAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(650, delayMillis = 150),
        label = "textAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF100B20)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "LEXIO Logo",
                modifier = Modifier
                    .size(112.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "lexio",
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                color = Color(0xFFD8C3FF),
                modifier = Modifier.alpha(textAlpha)
            )
        }
    }
}
