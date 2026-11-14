package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AnnotationTool
import com.example.data.model.PageTurnMode
import com.example.data.model.ReadingDirection
import com.example.data.model.ReadingTheme
import com.example.ui.components.AnnotationCanvas
import com.example.ui.components.HandwrittenPageIndicator
import com.example.ui.components.PageCurlBox
import com.example.ui.components.ReadingThemeController
import com.example.ui.components.StitchedPaperDivider
import com.example.ui.components.ZoomablePageBox
import com.example.ui.components.parseHexColor
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.LocalStudyColors
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val studyColors = LocalStudyColors.current

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var stickyNoteInputText by remember { mutableStateOf("") }

    BackHandler {
        if (uiState.annotationTool != AnnotationTool.NONE) {
            viewModel.setAnnotationTool(AnnotationTool.NONE)
        } else {
            onBack()
        }
    }

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    LaunchedEffect(uiState.editingStickyNote) {
        stickyNoteInputText = uiState.editingStickyNote?.text ?: ""
    }

    LaunchedEffect(uiState.isCreatingStickyNote) {
        if (uiState.isCreatingStickyNote) {
            stickyNoteInputText = ""
        }
    }

    val theme = uiState.readingTheme
    val pageBgColor = ReadingThemeController.getPageBackgroundColor(theme)
    val surfaceBgColor = ReadingThemeController.getSurfaceBackgroundColor(theme)
    val primaryTextColor = ReadingThemeController.getPrimaryTextColor(theme)

    // Classic Paper Ribbon colors harmonize with Espresso/Cream study or dark reading modes
    val isDarkReaderSurface = theme == ReadingTheme.DARK_SLATE || theme == ReadingTheme.MIDNIGHT_OLED || studyColors.isDark
    val ribbonBg = if (theme == ReadingTheme.DARK_SLATE || theme == ReadingTheme.MIDNIGHT_OLED) {
        surfaceBgColor
    } else {
        studyColors.ribbonPaper
    }
    val ribbonText = if (theme == ReadingTheme.DARK_SLATE || theme == ReadingTheme.MIDNIGHT_OLED) {
        primaryTextColor
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val ribbonBorder = if (isDarkReaderSurface) Color(0xFF3E3129) else studyColors.ribbonBorder
    val ribbonAccent = if (isDarkReaderSurface) Color(0xFFD4A373) else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceBgColor)
    ) {
        // MAIN READING VIEWPORT: Continuous Webtoon OR 3D PageCurl
        if (uiState.readingDirection == ReadingDirection.VERTICAL_WEBTOON) {
            WebtoonReaderView(
                pageCount = uiState.pageCount,
                currentPage = uiState.currentPage,
                theme = theme,
                onPageChanged = { page -> viewModel.goToPage(page) },
                onCenterTap = { viewModel.toggleControls() },
                loadPage = { pageIndex -> viewModel.getPageBitmap(pageIndex) }
            )
        } else {
            PageCurlBox(
                modifier = Modifier.fillMaxSize(),
                pageTurnMode = uiState.pageTurnMode,
                readingDirection = uiState.readingDirection,
                canGoNext = uiState.currentPage + 1 < uiState.pageCount,
                canGoPrev = uiState.currentPage > 0,
                onNextPage = { viewModel.nextPage() },
                onPrevPage = { viewModel.prevPage() },
                isZoomed = uiState.zoomScale > 1.05f,
                isAnnotationModeActive = uiState.annotationTool != AnnotationTool.NONE,
                onCenterTap = { viewModel.toggleControls() },
                currentPageContent = {
                    ZoomablePageBox(
                        modifier = Modifier.fillMaxSize(),
                        isAnnotationModeActive = uiState.annotationTool != AnnotationTool.NONE,
                        onZoomChanged = { viewModel.setZoomScale(it) }
                    ) { _ ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(pageBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.currentPageBitmap != null && !uiState.currentPageBitmap!!.isRecycled) {
                                Image(
                                    bitmap = uiState.currentPageBitmap!!.asImageBitmap(),
                                    contentDescription = "Page ${uiState.currentPage + 1}",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ReadingThemeController.getColorFilter(theme),
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = primaryTextColor)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Turning to page ${uiState.currentPage + 1}...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = primaryTextColor
                                    )
                                }
                            }

                            // Annotation Overlay Canvas
                            AnnotationCanvas(
                                modifier = Modifier.fillMaxSize(),
                                activeTool = uiState.annotationTool,
                                currentColorHex = uiState.activeColorHex,
                                currentStrokeWidth = uiState.activeStrokeWidth,
                                existingStrokes = uiState.pageStrokes,
                                existingStickyNotes = uiState.pageStickyNotes,
                                onStrokeCompleted = { stroke -> viewModel.onStrokeDrawn(stroke) },
                                onStickyNoteDropped = { xNorm, yNorm -> viewModel.onStickyNoteDropped(xNorm, yNorm) },
                                onStickyNoteClicked = { note -> viewModel.onStickyNoteClicked(note) }
                            )
                        }
                    }
                },
                peekPageContent = { isNext ->
                    val peekBitmap = if (isNext) uiState.nextPageBitmap else uiState.prevPageBitmap
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(pageBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (peekBitmap != null && !peekBitmap.isRecycled) {
                            Image(
                                bitmap = peekBitmap.asImageBitmap(),
                                contentDescription = "Adjacent Page",
                                contentScale = ContentScale.Fit,
                                colorFilter = ReadingThemeController.getColorFilter(theme),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            )
        }

        // FLOATING ZOOM RESET STAMP (when zoomed in)
        AnimatedVisibility(
            visible = uiState.zoomScale > 1.05f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 74.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ribbonBg.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, ribbonBorder),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .testTag("reset_zoom_chip")
                    .clickable { viewModel.resetZoom() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOutMap,
                        contentDescription = "Reset Zoom",
                        tint = ribbonAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(uiState.zoomScale * 100).toInt()}% • Reset",
                        style = MaterialTheme.typography.labelSmall,
                        color = ribbonText
                    )
                }
            }
        }

        // CLASSIC TOP PAPER RIBBON
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ribbonBg.copy(alpha = 0.97f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Bookshelf",
                                tint = ribbonText
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.book?.title ?: "Reading",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = ribbonText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Page ${uiState.currentPage + 1} of ${uiState.pageCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ribbonText.copy(alpha = 0.72f)
                                )
                                if (uiState.isMangaMode) {
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = studyColors.waxSeal
                                    ) {
                                        Text(
                                            text = uiState.readingDirection.shortLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.toggleMangaMode() },
                            modifier = Modifier.testTag("toggle_manga_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = "Toggle Manga Mode",
                                tint = if (uiState.isMangaMode) studyColors.waxSeal else ribbonText
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleBookmark() },
                            modifier = Modifier.testTag("toggle_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark Page",
                                tint = if (uiState.isBookmarked) studyColors.brassAccent else ribbonText
                            )
                        }

                        IconButton(
                            onClick = {
                                if (uiState.annotationTool == AnnotationTool.NONE) {
                                    viewModel.setAnnotationTool(AnnotationTool.HIGHLIGHT)
                                } else {
                                    viewModel.setAnnotationTool(AnnotationTool.NONE)
                                }
                            },
                            modifier = Modifier.testTag("toggle_annotation_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.annotationTool != AnnotationTool.NONE) Icons.Default.Draw else Icons.Default.Highlight,
                                contentDescription = "Annotation Stationery",
                                tint = if (uiState.annotationTool != AnnotationTool.NONE) ribbonAccent else ribbonText
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setDrawerOpen(true, 0) },
                            modifier = Modifier.testTag("open_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Contents & Reading Style",
                                tint = ribbonText
                            )
                        }
                    }
                    StitchedPaperDivider(color = ribbonBorder)
                }
            }
        }

        // CLASSIC BOTTOM PAPER RIBBON (Page Scrubber OR Stationery Markup Tray)
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ribbonBg.copy(alpha = 0.97f),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    StitchedPaperDivider(color = ribbonBorder)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        if (uiState.annotationTool == AnnotationTool.NONE) {
                            var sliderValue by remember(uiState.currentPage) {
                                mutableFloatStateOf(uiState.currentPage.toFloat())
                            }
                            val displayPageIndex = sliderValue.toInt().coerceIn(0, (uiState.pageCount - 1).coerceAtLeast(0))

                            // NORMAL READING PAPER RIBBON
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.prevPage() },
                                    enabled = uiState.currentPage > 0,
                                    modifier = Modifier.testTag("prev_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous Page",
                                        tint = if (uiState.currentPage > 0) ribbonText else ribbonText.copy(alpha = 0.3f)
                                    )
                                }

                                Slider(
                                    value = sliderValue,
                                    onValueChange = { sliderValue = it },
                                    onValueChangeFinished = { viewModel.goToPage(sliderValue.toInt()) },
                                    valueRange = 0f..(uiState.pageCount - 1).coerceAtLeast(1).toFloat(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp)
                                        .testTag("page_scrubber_slider"),
                                    colors = SliderDefaults.colors(
                                        thumbColor = ribbonAccent,
                                        activeTrackColor = ribbonAccent,
                                        inactiveTrackColor = ribbonBorder
                                    )
                                )

                                IconButton(
                                    onClick = { viewModel.nextPage() },
                                    enabled = uiState.currentPage + 1 < uiState.pageCount,
                                    modifier = Modifier.testTag("next_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next Page",
                                        tint = if (uiState.currentPage + 1 < uiState.pageCount) ribbonText else ribbonText.copy(alpha = 0.3f)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val pct = ((displayPageIndex.toFloat() / (uiState.pageCount - 1).coerceAtLeast(1)) * 100).toInt()
                                HandwrittenPageIndicator(
                                    currentPageOneBased = displayPageIndex + 1,
                                    totalPages = uiState.pageCount,
                                    percentRead = pct,
                                    inkColor = ribbonText,
                                    modifier = Modifier.testTag("handwritten_page_indicator")
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, ribbonBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.setDrawerOpen(true, 2) }
                                        .testTag("theme_settings_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = ribbonAccent
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = uiState.readingTheme.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ribbonText
                                        )
                                    }
                                }
                            }
                        } else {
                            // STATIONERY MARKUP TRAY
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        item {
                                            FilterChip(
                                                selected = uiState.annotationTool == AnnotationTool.HIGHLIGHT,
                                                onClick = { viewModel.setAnnotationTool(AnnotationTool.HIGHLIGHT) },
                                                leadingIcon = { Icon(Icons.Default.Highlight, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                                label = { Text("Highlight") },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("tool_highlight_chip")
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = uiState.annotationTool == AnnotationTool.PEN,
                                                onClick = { viewModel.setAnnotationTool(AnnotationTool.PEN) },
                                                leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                                label = { Text("Ink Pen") },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("tool_pen_chip")
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = uiState.annotationTool == AnnotationTool.STICKY_NOTE,
                                                onClick = { viewModel.setAnnotationTool(AnnotationTool.STICKY_NOTE) },
                                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                                label = { Text("Note") },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("tool_note_chip")
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { viewModel.undoLastStroke() },
                                            enabled = uiState.pageStrokes.isNotEmpty()
                                        ) {
                                            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = ribbonText)
                                        }
                                        IconButton(
                                            onClick = { showClearConfirmDialog = true },
                                            enabled = uiState.pageStrokes.isNotEmpty() || uiState.pageStickyNotes.isNotEmpty()
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Clear Page", tint = ribbonText)
                                        }
                                        IconButton(
                                            onClick = { viewModel.setAnnotationTool(AnnotationTool.NONE) },
                                            modifier = Modifier.testTag("close_annotation_mode")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Done", tint = ribbonAccent)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val colors = if (uiState.annotationTool == AnnotationTool.HIGHLIGHT) {
                                        listOf("#FFF200", "#86EFAC", "#FDA4AF", "#7DD3FC", "#D8B4FE")
                                    } else {
                                        listOf("#231C16", "#A63A3A", "#2F5D50", "#1D4ED8", "#B45309")
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        colors.forEach { hex ->
                                            val isSelected = uiState.activeColorHex.equals(hex, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(parseHexColor(hex))
                                                    .clickable { viewModel.setAnnotationColor(hex) }
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = if (isSelected) ribbonAccent else ribbonBorder,
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val widths = if (uiState.annotationTool == AnnotationTool.HIGHLIGHT) {
                                            listOf(12f to "Fine", 20f to "Med", 32f to "Broad")
                                        } else {
                                            listOf(3f to "Fine", 6f to "Med", 12f to "Bold")
                                        }

                                        widths.forEach { (w, label) ->
                                            val isSelected = uiState.activeStrokeWidth == w
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) ribbonAccent else Color.Transparent,
                                                border = BorderStroke(1.dp, if (isSelected) ribbonAccent else ribbonBorder),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { viewModel.setStrokeWidth(w) }
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) {
                                                        if (isDarkReaderSurface) Color(0xFF1F1712) else MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        ribbonText
                                                    },
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // BOOKMARKS, NOTES & READING STYLE DRAWER
        if (uiState.showDrawer) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setDrawerOpen(false) },
                sheetState = bottomSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    TabRow(
                        selectedTabIndex = uiState.drawerTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = uiState.drawerTab == 0,
                            onClick = { viewModel.setDrawerOpen(true, 0) },
                            text = { Text("Bookmarks (${uiState.bookmarks.size})") }
                        )
                        Tab(
                            selected = uiState.drawerTab == 1,
                            onClick = { viewModel.setDrawerOpen(true, 1) },
                            text = { Text("Notes (${uiState.allAnnotations.size})") }
                        )
                        Tab(
                            selected = uiState.drawerTab == 2,
                            onClick = { viewModel.setDrawerOpen(true, 2) },
                            text = { Text("Reading Style") }
                        )
                    }

                    when (uiState.drawerTab) {
                        0 -> {
                            if (uiState.bookmarks.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No bookmarks placed yet.\nTap the ribbon bookmark icon above to mark any page.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .padding(16.dp)
                                ) {
                                    items(uiState.bookmarks) { bookmark ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    viewModel.goToPage(bookmark.pageIndex)
                                                    viewModel.setDrawerOpen(false)
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Bookmark,
                                                    contentDescription = null,
                                                    tint = studyColors.brassAccent
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "Page ${bookmark.pageIndex + 1}",
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                    Text(
                                                        text = bookmark.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Open →",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (uiState.allAnnotations.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No highlights or margin notes yet.\nSelect the pen tool in the top ribbon to annotate pages.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(320.dp)
                                        .padding(16.dp)
                                ) {
                                    items(uiState.allAnnotations) { ann ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    viewModel.goToPage(ann.pageIndex)
                                                    viewModel.setDrawerOpen(false)
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(parseHexColor(ann.colorHex))
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "Page ${ann.pageIndex + 1} • ${ann.type}",
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                    if (ann.noteText.isNotBlank()) {
                                                        Text(
                                                            text = ann.noteText,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Open →",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // READING STYLE, MANGA MODE & PAPER TONES
                            Column(modifier = Modifier.padding(16.dp)) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("manga_mode_menu_card"),
                                    color = if (uiState.isMangaMode) studyColors.waxSeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (uiState.isMangaMode) studyColors.waxSeal else MaterialTheme.colorScheme.outline),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.AutoStories,
                                                    contentDescription = null,
                                                    tint = if (uiState.isMangaMode) studyColors.waxSeal else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Manga Mode",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = if (uiState.isMangaMode) studyColors.waxSeal else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Right-to-Left binding and panel navigation",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = uiState.isMangaMode,
                                            onCheckedChange = { viewModel.toggleMangaMode() },
                                            modifier = Modifier.testTag("manga_mode_switch"),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = studyColors.waxSeal
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                StitchedPaperDivider()
                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Reading Direction",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ReadingDirection.values().forEach { dir ->
                                        val isSel = uiState.readingDirection == dir
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { viewModel.setReadingDirection(dir) },
                                            label = {
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text(dir.displayName, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                                    Text(dir.description, style = MaterialTheme.typography.labelSmall)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Paper Tone & Color Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(ReadingTheme.values().toList()) { th ->
                                        val isSel = uiState.readingTheme == th
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { viewModel.setReadingTheme(th) },
                                            label = { Text(th.displayName) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Page Turn Effect",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(PageTurnMode.values().toList()) { mode ->
                                        val isSel = uiState.pageTurnMode == mode
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { viewModel.setPageTurnMode(mode) },
                                            label = { Text(mode.displayName) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // STICKY NOTE DIALOG
        if (uiState.isCreatingStickyNote || uiState.editingStickyNote != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissStickyNoteDialog() },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PushPin, contentDescription = null, tint = studyColors.brassAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.editingStickyNote != null) "Edit Margin Note" else "New Margin Note",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Page ${uiState.currentPage + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = stickyNoteInputText,
                            onValueChange = { stickyNoteInputText = it },
                            placeholder = { Text("Jot down a thought or passage note...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .testTag("sticky_note_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (stickyNoteInputText.isNotBlank()) {
                                viewModel.saveStickyNote(stickyNoteInputText)
                            }
                        },
                        enabled = stickyNoteInputText.isNotBlank()
                    ) {
                        Text("Save Note")
                    }
                },
                dismissButton = {
                    Row {
                        if (uiState.editingStickyNote != null) {
                            TextButton(
                                onClick = { viewModel.deleteStickyNote(uiState.editingStickyNote!!.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = studyColors.waxSeal)
                            ) {
                                Text("Delete")
                            }
                        }
                        TextButton(onClick = { viewModel.dismissStickyNoteDialog() }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        // CLEAR PAGE CONFIRMATION DIALOG
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Clear Page Annotations?", style = MaterialTheme.typography.titleLarge) },
                text = { Text("Remove all highlights, ink strokes, and margin notes on Page ${uiState.currentPage + 1}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearCurrentPageAnnotations()
                            showClearConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = studyColors.waxSeal, contentColor = Color.White)
                    ) {
                        Text("Clear Page")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun WebtoonReaderView(
    pageCount: Int,
    currentPage: Int,
    theme: ReadingTheme,
    onPageChanged: (Int) -> Unit,
    onCenterTap: () -> Unit,
    loadPage: suspend (Int) -> Bitmap?
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { page ->
                onPageChanged(page)
            }
    }

    ZoomablePageBox(
        modifier = Modifier.fillMaxSize(),
        isAnnotationModeActive = false
    ) { _ ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(ReadingThemeController.getPageBackgroundColor(theme))
                .clickable { onCenterTap() },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(pageCount) { pageIndex ->
                var pageBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

                LaunchedEffect(pageIndex) {
                    pageBitmap = loadPage(pageIndex)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (pageBitmap != null && !pageBitmap!!.isRecycled) {
                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "Page ${pageIndex + 1}",
                            contentScale = ContentScale.FillWidth,
                            colorFilter = ReadingThemeController.getColorFilter(theme),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .background(Color.DarkGray.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}
