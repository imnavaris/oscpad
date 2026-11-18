package ar.matu.oscpad

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val Grotesk = FontFamily(Font(R.font.space_grotesk))

val Mono = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold),
)

fun appTypography(): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Grotesk),
        displayMedium = base.displayMedium.copy(fontFamily = Grotesk),
        displaySmall = base.displaySmall.copy(fontFamily = Grotesk),
        headlineLarge = base.headlineLarge.copy(fontFamily = Grotesk),
        headlineMedium = base.headlineMedium.copy(fontFamily = Grotesk),
        headlineSmall = base.headlineSmall.copy(fontFamily = Grotesk),
        titleLarge = base.titleLarge.copy(fontFamily = Grotesk),
        titleMedium = base.titleMedium.copy(fontFamily = Grotesk),
        titleSmall = base.titleSmall.copy(fontFamily = Grotesk),
        bodyLarge = base.bodyLarge.copy(fontFamily = Grotesk),
        bodyMedium = base.bodyMedium.copy(fontFamily = Grotesk),
        bodySmall = base.bodySmall.copy(fontFamily = Grotesk),
        labelLarge = base.labelLarge.copy(fontFamily = Grotesk),
        labelMedium = base.labelMedium.copy(fontFamily = Grotesk),
        labelSmall = base.labelSmall.copy(fontFamily = Grotesk),
    )
}
