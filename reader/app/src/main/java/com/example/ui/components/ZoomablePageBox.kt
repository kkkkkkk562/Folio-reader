package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * High-performance container providing pinch-to-zoom, pan, and double-tap zoom for PDF page.
 */
@Composable
fun ZoomablePageBox(
    modifier: Modifier = Modifier,
    isAnnotationModeActive: Boolean = false,
    onZoomChanged: (Float) -> Unit = {},
    content: @Composable (scale: Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()


    val isZoomed = scale > 1.05f

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isZoomed && !isAnnotationModeActive) {
                    Modifier.pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4.5f)
                            scale = newScale
                            onZoomChanged(newScale)

                            if (newScale > 1f) {
                                val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                val maxOffsetY = (size.height * (newScale - 1f)) / 2f

                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
            .pointerInput(isAnnotationModeActive) {
                if (!isAnnotationModeActive) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            scope.launch {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                    onZoomChanged(1f)
                                } else {
                                    val targetScale = 2.4f
                                    scale = targetScale
                                    onZoomChanged(targetScale)
                                    val targetOffsetX = (size.width / 2f - tapOffset.x) * (targetScale - 1f)
                                    val targetOffsetY = (size.height / 2f - tapOffset.y) * (targetScale - 1f)
                                    val maxOffsetX = (size.width * (targetScale - 1f)) / 2f
                                    val maxOffsetY = (size.height * (targetScale - 1f)) / 2f
                                    offset = Offset(
                                        x = targetOffsetX.coerceIn(-maxOffsetX, maxOffsetX),
                                        y = targetOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                }
                            }
                        }
                    )
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        content(scale)
    }
}
