package com.konvoiku.touring.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Haram Emerald (Navigation & Main Structure)
val TouringBlue = Color(0xFF047857)
val TouringBlueBright = Color(0xFF059669)
val TouringBlueLight = Color(0xFFECFDF5)
val TouringBlueDark = Color(0xFF064E3B)

// Kiswah Gold / Amber (Primary Safar CTAs)
val RoadAmber = Color(0xFFB45309)
val RoadAmberBright = Color(0xFFF59E0B)
val RoadAmberLight = Color(0xFFFFFBEB)
val RoadAmberDark = Color(0xFF78350F)

// SOS Emergency Red
val SosRed = Color(0xFFDC2626)
val SosRedDark = Color(0xFF991B1B)

// Haram Night Charcoal & Clean Mint-Ivory Neutrals
val AsphaltCharcoal = Color(0xFF0B1F16)
val SlateInk = Color(0xFF4D6659)
val MutedSteel = Color(0xFF6B8A7A)
val CanvasLight = Color(0xFFF4F8F5)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE6EFE9)
val OutlineSubtle = Color(0xFFD8E6DD)

// Deep Haram Night & Sanctuary Emerald Dark Neutrals
val CanvasDark = Color(0xFF06130E)
val SurfaceDark = Color(0xFF0C1E17)
val SurfaceVariantDark = Color(0xFF132E24)
val OutlineDark = Color(0xFF1B3B2E)

private val LightColorScheme = lightColorScheme(
    primary = TouringBlue,
    onPrimary = Color.White,
    primaryContainer = TouringBlueLight,
    onPrimaryContainer = TouringBlueDark,
    secondary = RoadAmber,
    onSecondary = Color.White,
    secondaryContainer = RoadAmberLight,
    onSecondaryContainer = RoadAmberDark,
    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    background = CanvasLight,
    onBackground = AsphaltCharcoal,
    surface = SurfaceWhite,
    onSurface = AsphaltCharcoal,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = SlateInk,
    outline = OutlineSubtle,
    error = SosRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF032217),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF042F2E),
    background = CanvasDark,
    onBackground = Color(0xFFECFDF5),
    surface = SurfaceDark,
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFF9CC2AE),
    outline = OutlineDark,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val KonvoiTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

val KonvoiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun KonvoiKuTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = KonvoiTypography,
        shapes = KonvoiShapes,
        content = content
    )
}
