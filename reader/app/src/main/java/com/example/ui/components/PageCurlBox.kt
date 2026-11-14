package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.model.PageTurnMode
import com.example.data.model.ReadingDirection
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * High-performance 3D book page curl and horizontal swipe gesture container.
 *
 * Supports:
 * 1. Realistic 3D book page curl animation following the user's finger.
 * 2. Authentic Manga Right-to-Left (RTL) reading (bound on the right, swiping rightward turns to next page).
 * 3. Standard Western Left-to-Right (LTR) reading (bound on the left, swiping leftward turns to next page).
 * 4. Tap gestures: Left/right edge taps turn pages, center tap toggles reader controls.
 * 5. Smooth spring fallback when released halfway.
 */
@Composable
fun PageCurlBox(
    modifier: Modifier = Modifier,
    pageTurnMode: PageTurnMode = PageTurnMode.CURL_3D,
    readingDirection: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
    canGoNext: Boolean,
    canGoPrev: Boolean,
    onNextPage: () -> Unit,
    onPrevPage: () -> Unit,
    isZoomed: Boolean = false,
    isAnnotationModeActive: Boolean = false,
    onCenterTap: () -> Unit = {},
    currentPageContent: @Composable () -> Unit,
    peekPageContent: (@Composable (isNext: Boolean) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val isRtl = (readingDirection == ReadingDirection.RIGHT_TO_LEFT)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isZoomed, isAnnotationModeActive, isRtl, canGoNext, canGoPrev) {
                    if (!isZoomed && !isAnnotationModeActive) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var isDragStarted = false
                                val touchSlop = viewConfiguration.touchSlop

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                    if (change.changedToUp()) {
                                        if (isDragStarted) {
                                            val currentVal = dragOffset.value
                                            val threshold = totalWidth * 0.18f

                                            scope.launch {
                                                isDragging = false
                                                if (!isRtl) {
                                                    // Standard Western LTR
                                                    if (currentVal < -threshold && canGoNext) {
                                                        dragOffset.animateTo(
                                                            targetValue = -totalWidth,
                                                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                                        )
                                                        onNextPage()
                                                        dragOffset.snapTo(0f)
                                                    } else if (currentVal > threshold && canGoPrev) {
                                                        dragOffset.animateTo(
                                                            targetValue = totalWidth,
                                                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                                        )
                                                        onPrevPage()
                                                        dragOffset.snapTo(0f)
                                                    } else {
                                                        dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                    }
                                                } else {
                                                    // Manga RTL: positive drag (swiping left-to-right) turns forward!
                                                    if (currentVal > threshold && canGoNext) {
                                                        dragOffset.animateTo(
                                                            targetValue = totalWidth,
                                                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                                        )
                                                        onNextPage()
                                                        dragOffset.snapTo(0f)
                                                    } else if (currentVal < -threshold && canGoPrev) {
                                                        dragOffset.animateTo(
                                                            targetValue = -totalWidth,
                                                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                                        )
                                                        onPrevPage()
                                                        dragOffset.snapTo(0f)
                                                    } else {
                                                        dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                    }
                                                }
                                            }
                                        } else {
                                            // Tap without drag
                                            val tapX = change.position.x
                                            if (tapX < totalWidth * 0.25f) {
                                                if (isRtl) onNextPage() else onPrevPage()
                                            } else if (tapX > totalWidth * 0.75f) {
                                                if (isRtl) onPrevPage() else onNextPage()
                                            } else {
                                                onCenterTap()
                                            }
                                        }
                                        break
                                    } else if (change.isConsumed) {
                                        if (isDragStarted) {
                                            scope.launch { dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                        }
                                        break
                                    } else {
                                        val dx = change.position.x - down.position.x
                                        val dy = change.position.y - down.position.y

                                        if (!isDragStarted) {
                                            if (abs(dx) > touchSlop && abs(dx) > abs(dy) * 1.1f) {
                                                isDragStarted = true
                                                isDragging = true
                                                change.consume()
                                            }
                                        } else {
                                            change.consume()
                                            val delta = change.position.x - change.previousPosition.x
                                            val current = dragOffset.value + delta

                                            val clamped = if (isRtl) {
                                                when {
                                                    current > 0 && !canGoNext -> current * 0.15f
                                                    current < 0 && !canGoPrev -> current * 0.15f
                                                    else -> current
                                                }
                                            } else {
                                                when {
                                                    current < 0 && !canGoNext -> current * 0.15f
                                                    current > 0 && !canGoPrev -> current * 0.15f
                                                    else -> current
                                                }
                                            }.coerceIn(-totalWidth, totalWidth)

                                            scope.launch { dragOffset.snapTo(clamped) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val progress = (dragOffset.value / totalWidth).coerceIn(-1f, 1f)
            val absProgress = abs(progress)
            val isTurningNext = if (isRtl) progress > 0f else progress < 0f

            // 1. Reveal Layer: The underlying page (Next page if turning forward; Current page if turning backward)
            if (absProgress > 0.005f && peekPageContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            // Soft shadow cast onto the revealed page by the turning flap
                            if (pageTurnMode == PageTurnMode.CURL_3D && absProgress > 0.02f) {
                                val shadowAlpha = (absProgress * 0.45f).coerceIn(0f, 0.45f)
                                if (!isRtl) {
                                    // Western LTR: revealed page is on the right
                                    val leftEdge = size.width * (1f - absProgress * 1.05f).coerceIn(0f, 1f)
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = shadowAlpha),
                                                Color.Black.copy(alpha = shadowAlpha * 0.4f),
                                                Color.Transparent
                                            ),
                                            startX = leftEdge,
                                            endX = (leftEdge + 50.dp.toPx()).coerceAtMost(size.width)
                                        )
                                    )
                                } else {
                                    // Manga RTL: revealed page is on the left
                                    val rightEdge = (size.width * absProgress * 1.05f).coerceIn(0f, size.width)
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = shadowAlpha * 0.4f),
                                                Color.Black.copy(alpha = shadowAlpha)
                                            ),
                                            startX = (rightEdge - 50.dp.toPx()).coerceAtLeast(0f),
                                            endX = rightEdge
                                        )
                                    )
                                }
                            }
                        }
                ) {
                    if (isTurningNext) {
                        peekPageContent(true)
                    } else {
                        currentPageContent()
                    }
                }
            }

            // 2. Active Flap Layer (Current page curling away forward, or Previous page curling in backward)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        when (pageTurnMode) {
                            PageTurnMode.CURL_3D -> {
                                if (absProgress > 0f) {
                                    cameraDistance = 16f * density

                                    if (isTurningNext) {
                                        if (!isRtl) {
                                            // Western LTR: Turning to Next Page -> Left spine is the anchor
                                            transformOrigin = TransformOrigin(0f, 0.5f)
                                            rotationY = -progress * 75f
                                            translationX = dragOffset.value * 0.12f
                                        } else {
                                            // Manga RTL: Turning to Next Page -> Right spine is the anchor
                                            transformOrigin = TransformOrigin(1f, 0.5f)
                                            rotationY = -progress * 75f
                                            translationX = dragOffset.value * 0.12f
                                        }
                                        shadowElevation = (absProgress * 20f).dp.toPx()
                                    } else {
                                        if (!isRtl) {
                                            // Western LTR: Turning to Previous Page -> Comes in from Left spine
                                            transformOrigin = TransformOrigin(0f, 0.5f)
                                            rotationY = (1f - absProgress) * 75f
                                            translationX = (dragOffset.value - totalWidth) * 0.12f
                                        } else {
                                            // Manga RTL: Turning to Previous Page -> Comes in from Right spine
                                            transformOrigin = TransformOrigin(1f, 0.5f)
                                            rotationY = -(1f - absProgress) * 75f
                                            translationX = (dragOffset.value + totalWidth) * 0.12f
                                        }
                                        shadowElevation = ((1f - absProgress) * 20f).dp.toPx()
                                    }
                                }
                            }
                            PageTurnMode.SLIDE -> {
                                translationX = dragOffset.value
                                shadowElevation = if (absProgress > 0f) (absProgress * 14f).dp.toPx() else 0f
                            }
                            PageTurnMode.FADE -> {
                                alpha = (1f - absProgress).coerceIn(0.2f, 1f)
                            }
                        }
                    }
                    .drawWithContent {
                        drawContent()

                        // 3D Specular Highlight Sheen and Page Crease Shadow across the curved cylinder of the paper
                        if (absProgress > 0.02f && pageTurnMode == PageTurnMode.CURL_3D) {
                            val sheenAlpha = (absProgress * 0.32f).coerceIn(0f, 0.32f)
                            val creaseAlpha = (absProgress * 0.40f).coerceIn(0f, 0.40f)

                            if (!isRtl) {
                                // Western LTR: Curl runs towards the left spine
                                val curlX = size.width * (1f - absProgress * 0.85f).coerceIn(0f, 1f)
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = sheenAlpha),
                                            Color.Black.copy(alpha = creaseAlpha),
                                            Color.Black.copy(alpha = creaseAlpha * 0.5f)
                                        ),
                                        startX = (curlX - 40.dp.toPx()).coerceAtLeast(0f),
                                        endX = size.width
                                    )
                                )
                            } else {
                                // Manga RTL: Curl runs towards the right spine
                                val curlX = (size.width * absProgress * 0.85f).coerceIn(0f, size.width)
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = creaseAlpha * 0.5f),
                                            Color.Black.copy(alpha = creaseAlpha),
                                            Color.White.copy(alpha = sheenAlpha),
                                            Color.Transparent
                                        ),
                                        startX = 0f,
                                        endX = (curlX + 40.dp.toPx()).coerceAtMost(size.width)
                                    )
                                )
                            }
                        }

                        // Realistic Book Spine Shadow (Left for LTR, Right for Manga RTL)
                        val spineWidth = 26.dp.toPx()
                        if (!isRtl) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.28f),
                                        Color.Black.copy(alpha = 0.10f),
                                        Color.Transparent
                                    ),
                                    startX = 0f,
                                    endX = spineWidth
                                )
                            )
                        } else {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.10f),
                                        Color.Black.copy(alpha = 0.28f)
                                    ),
                                    startX = size.width - spineWidth,
                                    endX = size.width
                                )
                            )
                        }
                    }
            ) {
                if (absProgress > 0.005f && !isTurningNext && peekPageContent != null) {
                    peekPageContent(false)
                } else {
                    currentPageContent()
                }
            }
        }
    }
}
