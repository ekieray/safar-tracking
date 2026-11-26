package com.konvoiku.touring.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Subtle Safar Tracking atmosphere canvas backdrop:
 * - Light mode: Mint-ivory sanctuary haze with low-opacity emerald contours & gold route trace.
 * - Dark mode: Deep Haram night emerald with subtle gold horizon glow.
 */
@Composable
fun TouringDashboardCanvasBackdrop(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isDarkTheme) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF06130E).copy(alpha = 0.42f),
                                Color(0xFF0B231A).copy(alpha = 0.30f),
                                Color(0xFF06130E).copy(alpha = 0.46f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF4F8F5).copy(alpha = 0.16f),
                                Color(0xFFECFDF5).copy(alpha = 0.10f),
                                Color(0xFFFFFBEB).copy(alpha = 0.18f)
                            )
                        )
                    )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val contourColor = if (isDarkTheme) {
                Color(0xFF34D399).copy(alpha = 0.06f)
            } else {
                Color(0xFF047857).copy(alpha = 0.045f)
            }

            for (i in 1..5) {
                val offset = i * 44.dp.toPx()
                val contourPath = Path().apply {
                    moveTo(-w * 0.1f, h * 0.18f + offset)
                    cubicTo(
                        w * 0.28f, h * 0.06f + offset * 0.8f,
                        w * 0.65f, h * 0.30f + offset * 0.9f,
                        w * 1.1f, h * 0.15f + offset
                    )
                }
                drawPath(
                    path = contourPath,
                    color = contourColor,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            val routeColor = if (isDarkTheme) {
                Color(0xFFF59E0B).copy(alpha = 0.08f)
            } else {
                Color(0xFFB45309).copy(alpha = 0.06f)
            }
            val roadPath = Path().apply {
                moveTo(-w * 0.05f, h * 0.88f)
                cubicTo(
                    w * 0.32f, h * 0.72f,
                    w * 0.68f, h * 0.96f,
                    w * 1.08f, h * 0.78f
                )
            }
            drawPath(
                path = roadPath,
                color = routeColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 16f))
                )
            )
        }
    }
}

/**
 * Haram Emerald & Kiswah Gold Hero Header with subtle Rub el Hizb 8-point star,
 * crescent moon, and Masjidil Haram silhouette.
 */
@Composable
fun TouringHeroAtmosphereHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFF047857).copy(alpha = 0.45f)),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF032217),
                            Color(0xFF064E3B),
                            Color(0xFF047857)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 480f)
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                // Warm Kiswah Gold glow at top-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF59E0B).copy(alpha = 0.28f),
                            Color(0xFFB45309).copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.88f, h * 0.24f),
                        radius = w * 0.48f
                    ),
                    radius = w * 0.48f,
                    center = Offset(w * 0.88f, h * 0.24f)
                )

                // Rub el Hizb 8-point star motif on the right
                val starCenter = Offset(w * 0.85f, h * 0.36f)
                val sqSize = 38.dp.toPx()
                val topLeft = Offset(starCenter.x - sqSize / 2f, starCenter.y - sqSize / 2f)
                drawRect(
                    color = Color(0xFFFCD34D).copy(alpha = 0.18f),
                    topLeft = topLeft,
                    size = Size(sqSize, sqSize),
                    style = Stroke(width = 1.4.dp.toPx())
                )
                rotate(degrees = 45f, pivot = starCenter) {
                    drawRect(
                        color = Color(0xFFFCD34D).copy(alpha = 0.18f),
                        topLeft = topLeft,
                        size = Size(sqSize, sqSize),
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }

                // Crescent moon accent
                drawCircle(
                    color = Color(0xFFFDE68A).copy(alpha = 0.42f),
                    radius = 10.dp.toPx(),
                    center = Offset(w * 0.85f, h * 0.36f)
                )
                drawCircle(
                    color = Color(0xFF064E3B),
                    radius = 8.dp.toPx(),
                    center = Offset(w * 0.865f, h * 0.345f)
                )

                // Subtle Masjidil Haram & Ka'bah silhouette along bottom right
                val kabahW = 24.dp.toPx()
                val kabahH = 26.dp.toPx()
                val kabahLeft = w * 0.78f
                val kabahTop = h - kabahH - 6.dp.toPx()
                drawRect(
                    color = Color(0xFF02160F).copy(alpha = 0.55f),
                    topLeft = Offset(kabahLeft, kabahTop),
                    size = Size(kabahW, kabahH)
                )
                drawRect(
                    color = Color(0xFFF59E0B).copy(alpha = 0.65f),
                    topLeft = Offset(kabahLeft, kabahTop + 6.dp.toPx()),
                    size = Size(kabahW, 3.dp.toPx())
                )

                // Subtle sacred route arc
                val roadTrace = Path().apply {
                    moveTo(w * 0.46f, h * 1.05f)
                    cubicTo(
                        w * 0.62f, h * 0.65f,
                        w * 0.78f, h * 0.78f,
                        w * 1.02f, h * 0.46f
                    )
                }
                drawPath(
                    path = roadTrace,
                    color = Color(0xFFFCD34D).copy(alpha = 0.25f),
                    style = Stroke(
                        width = 1.8.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD1FAE5),
                    lineHeight = 19.sp
                )
            }
        }
    }
}
