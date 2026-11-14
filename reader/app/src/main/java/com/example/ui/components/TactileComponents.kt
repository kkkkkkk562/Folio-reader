package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.LocalStudyColors
import com.example.ui.theme.StudyColors

/**
 * Subtle tactile woven linen & notebook paper grain background modifier.
 */
fun Modifier.linenPaperBackground(
    baseColor: Color,
    studyColors: StudyColors
): Modifier = this
    .background(baseColor)
    .drawBehind {
        val step = 18.dp.toPx()
        val dotColor = studyColors.linenGrainDot
        val threadStroke = 0.6.dp.toPx()

        // Delicate horizontal & vertical linen weave threads
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = dotColor.copy(alpha = dotColor.alpha * 0.45f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = threadStroke
            )
            y += step
        }
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = dotColor.copy(alpha = dotColor.alpha * 0.3f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = threadStroke
            )
            x += step
        }
    }

/**
 * Hand-crafted wooden bookshelf plank with bevel top edge, organic woodgrain strokes,
 * front fascia, and ambient shadow cast onto the study wall below.
 */
@Composable
fun WoodenShelfPlank(
    modifier: Modifier = Modifier,
    height: Dp = 20.dp
) {
    val study = LocalStudyColors.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height

        val topLipHeight = h * 0.28f
        val frontFaceHeight = h * 0.46f
        val shadowHeight = h - topLipHeight - frontFaceHeight

        // 1. Top flat shelf surface (with soft book contact shadow)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = if (study.isDark) 0.45f else 0.18f),
                    study.woodTop
                ),
                startY = 0f,
                endY = topLipHeight
            ),
            topLeft = Offset.Zero,
            size = Size(w, topLipHeight)
        )

        // Subtle highlight ridge between top surface and front edge
        drawLine(
            color = Color.White.copy(alpha = if (study.isDark) 0.10f else 0.35f),
            start = Offset(0f, topLipHeight),
            end = Offset(w, topLipHeight),
            strokeWidth = 1.dp.toPx()
        )

        // 2. Front wooden plank face
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(study.woodFace, study.woodBottom),
                startY = topLipHeight,
                endY = topLipHeight + frontFaceHeight
            ),
            topLeft = Offset(0f, topLipHeight),
            size = Size(w, frontFaceHeight)
        )

        // Organic woodgrain lines across the front fascia
        val grainY1 = topLipHeight + frontFaceHeight * 0.32f
        val grainY2 = topLipHeight + frontFaceHeight * 0.68f
        drawLine(
            color = study.woodGrain.copy(alpha = 0.5f),
            start = Offset(w * 0.04f, grainY1),
            end = Offset(w * 0.42f, grainY1 + 0.8f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = study.woodGrain.copy(alpha = 0.4f),
            start = Offset(w * 0.48f, grainY1 - 0.5f),
            end = Offset(w * 0.94f, grainY1 + 0.5f),
            strokeWidth = 0.9.dp.toPx()
        )
        drawLine(
            color = study.woodGrain.copy(alpha = 0.45f),
            start = Offset(w * 0.12f, grainY2),
            end = Offset(w * 0.78f, grainY2 - 0.6f),
            strokeWidth = 1.1.dp.toPx()
        )

        // 3. Soft ambient drop shadow cast below the wooden ledge
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = if (study.isDark) 0.55f else 0.22f),
                    Color.Transparent
                ),
                startY = topLipHeight + frontFaceHeight,
                endY = h
            ),
            topLeft = Offset(0f, topLipHeight + frontFaceHeight),
            size = Size(w, shadowHeight)
        )
    }
}

/**
 * Stitched thread / dashed ink divider used inside paper ribbons and notebook cards.
 */
@Composable
fun StitchedPaperDivider(
    modifier: Modifier = Modifier,
    color: Color = LocalStudyColors.current.ribbonBorder
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
    }
}

/**
 * Renders physical hardcover/paperback spine groove and paper page-block thickness edge.
 * Automatically switches spine side for Manga (Right-to-Left) vs Standard Book (Left-to-Right).
 */
@Composable
fun BookSpineOverlay(
    isManga: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Primary spine groove shadow + linen hinge crease
        Box(
            modifier = Modifier
                .align(if (isManga) Alignment.CenterEnd else Alignment.CenterStart)
                .width(14.dp)
                .fillMaxSize()
                .background(
                    brush = if (!isManga) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.42f),
                                Color.White.copy(alpha = 0.22f),
                                Color.Black.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.22f),
                                Color.Black.copy(alpha = 0.42f)
                            )
                        )
                    }
                )
        )

        // Subtle paper-block edge highlight on the fore-edge
        Box(
            modifier = Modifier
                .align(if (isManga) Alignment.CenterStart else Alignment.CenterEnd)
                .width(3.dp)
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.24f))
        )
    }
}

/**
 * Delicate swallowtail fabric ribbon bookmark indicating active reading.
 */
@Composable
fun HangingRibbonBookmark(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 14.dp, height = 24.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h)
            lineTo(w / 2f, h * 0.72f)
            lineTo(0f, h)
            close()
        }
        drawPath(path = path, color = color)
        // Subtle stitched fold shadow on ribbon
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = 0.18f),
            style = Stroke(width = 0.8.dp.toPx())
        )
    }
}

/**
 * Discreet handwritten, ink-on-paper page numbering indicator for the bottom paper ribbon.
 * Uses the bundled Caveat handwritten font with a faint organic fountain-pen flourish beneath.
 */
@Composable
fun HandwrittenPageIndicator(
    currentPageOneBased: Int,
    totalPages: Int,
    percentRead: Int,
    inkColor: Color,
    modifier: Modifier = Modifier
) {
    val safeTotal = totalPages.coerceAtLeast(1)
    val safePage = currentPageOneBased.coerceIn(1, safeTotal)

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "Page $safePage of $safeTotal, $percentRead percent"
            }
            .graphicsLayer { rotationZ = -1.1f }
            .drawBehind {
                // Subtle hand-drawn fountain-pen underline curve directly on the paper ribbon
                val yBase = size.height - 2.dp.toPx()
                val startX = 2.dp.toPx()
                val endX = size.width - 4.dp.toPx()
                val flourishPath = Path().apply {
                    moveTo(startX, yBase)
                    quadraticTo(
                        size.width * 0.48f,
                        yBase - 1.6.dp.toPx(),
                        endX,
                        yBase + 0.6.dp.toPx()
                    )
                }
                drawPath(
                    path = flourishPath,
                    color = inkColor.copy(alpha = 0.28f),
                    style = Stroke(
                        width = 1.1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "p. $safePage / $safeTotal",
            fontFamily = CaveatFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 20.sp,
            color = inkColor.copy(alpha = 0.84f)
        )
        Text(
            text = "($percentRead%)",
            fontFamily = CaveatFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            color = inkColor.copy(alpha = 0.56f),
            modifier = Modifier.padding(bottom = 1.dp)
        )
    }
}
