package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.AnnotationTool
import com.example.data.model.DrawnStroke
import com.example.data.model.StickyNote
import kotlin.math.roundToInt

@Composable
fun AnnotationCanvas(
    modifier: Modifier = Modifier,
    activeTool: AnnotationTool,
    currentColorHex: String,
    currentStrokeWidth: Float,
    existingStrokes: List<DrawnStroke>,
    existingStickyNotes: List<StickyNote>,
    onStrokeCompleted: (DrawnStroke) -> Unit,
    onStickyNoteDropped: (xPercent: Float, yPercent: Float) -> Unit,
    onStickyNoteClicked: (StickyNote) -> Unit
) {
    // Current live drawing stroke
    var livePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val height = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // 1. Drawing Canvas for Highlighting and Pen
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (activeTool != AnnotationTool.NONE) {
                        Modifier.pointerInput(activeTool, currentColorHex, currentStrokeWidth) {
                            if (activeTool == AnnotationTool.HIGHLIGHT || activeTool == AnnotationTool.PEN) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        val normX = (startOffset.x / width).coerceIn(0f, 1f)
                                        val normY = (startOffset.y / height).coerceIn(0f, 1f)
                                        livePoints = listOf(normX to normY)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val normX = (change.position.x / width).coerceIn(0f, 1f)
                                        val normY = (change.position.y / height).coerceIn(0f, 1f)
                                        livePoints = livePoints + (normX to normY)
                                    },
                                    onDragEnd = {
                                        if (livePoints.size >= 2) {
                                            val completed = DrawnStroke(
                                                pageIndex = 0,
                                                isHighlight = (activeTool == AnnotationTool.HIGHLIGHT),
                                                colorHex = currentColorHex,
                                                strokeWidth = currentStrokeWidth,
                                                points = livePoints
                                            )
                                            onStrokeCompleted(completed)
                                        }
                                        livePoints = emptyList()
                                    },
                                    onDragCancel = {
                                        livePoints = emptyList()
                                    }
                                )
                            } else if (activeTool == AnnotationTool.STICKY_NOTE) {
                                detectTapGestures { tapOffset ->
                                    val normX = (tapOffset.x / width).coerceIn(0f, 1f)
                                    val normY = (tapOffset.y / height).coerceIn(0f, 1f)
                                    onStickyNoteDropped(normX, normY)
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            // Render existing saved strokes
            existingStrokes.forEach { stroke ->
                if (stroke.points.size >= 2) {
                    val path = Path()
                    val firstPt = stroke.points.first()
                    path.moveTo(firstPt.first * size.width, firstPt.second * size.height)

                    for (i in 1 until stroke.points.size) {
                        val pt = stroke.points[i]
                        path.lineTo(pt.first * size.width, pt.second * size.height)
                    }

                    val color = parseHexColor(stroke.colorHex)
                    val strokeColor = if (stroke.isHighlight) {
                        color.copy(alpha = 0.38f)
                    } else {
                        color
                    }

                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = if (stroke.isHighlight) BlendMode.SrcOver else BlendMode.SrcOver
                    )
                }
            }

            // Render active live stroke currently being drawn
            if (livePoints.size >= 2) {
                val livePath = Path()
                val firstPt = livePoints.first()
                livePath.moveTo(firstPt.first * size.width, firstPt.second * size.height)

                for (i in 1 until livePoints.size) {
                    val pt = livePoints[i]
                    livePath.lineTo(pt.first * size.width, pt.second * size.height)
                }

                val color = parseHexColor(currentColorHex)
                val strokeColor = if (activeTool == AnnotationTool.HIGHLIGHT) {
                    color.copy(alpha = 0.38f)
                } else {
                    color
                }

                drawPath(
                    path = livePath,
                    color = strokeColor,
                    style = Stroke(
                        width = currentStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // 2. Render Sticky Note pins on top
        existingStickyNotes.forEach { note ->
            val pinX = (note.xPercent * width) - 16f
            val pinY = (note.yPercent * height) - 28f

            Box(
                modifier = Modifier
                    .offset { IntOffset(pinX.roundToInt(), pinY.roundToInt()) }
                    .size(32.dp)
                    .background(parseHexColor(note.colorHex), CircleShape)
                    .clickable { onStickyNoteClicked(note) }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Sticky Note Pin",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val longVal = clean.toLong(16)
        if (clean.length <= 6) {
            Color(longVal or 0xFF000000)
        } else {
            Color(longVal)
        }
    } catch (e: Exception) {
        Color(0xFFFFEB3B) // Default highlighter yellow
    }
}
