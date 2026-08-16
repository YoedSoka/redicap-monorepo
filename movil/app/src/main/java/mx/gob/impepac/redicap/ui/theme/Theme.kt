package mx.gob.impepac.redicap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val RedicapColorScheme = lightColorScheme(
    primary = ImpepacMagenta600,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = ImpepacMagenta50,
    onPrimaryContainer = ImpepacMagenta600,
    secondary = ImpepacPurple700,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = ImpepacPurple50,
    onSecondaryContainer = ImpepacPurple700,
    background = ImpepacSurface,
    onBackground = ImpepacInk,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = ImpepacInk,
    error = ImpepacMagenta600,
)

private val RedicapTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
)

@Composable
fun RedicapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RedicapColorScheme,
        typography = RedicapTypography,
        content = content,
    )
}
