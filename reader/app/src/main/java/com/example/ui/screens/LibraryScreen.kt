package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.BookEntity
import com.example.data.repository.PdfImportDraft
import com.example.ui.components.BookSpineOverlay
import com.example.ui.components.HangingRibbonBookmark
import com.example.ui.components.StitchedPaperDivider
import com.example.ui.components.WoodenShelfPlank
import com.example.ui.components.linenPaperBackground
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalStudyColors
import com.example.ui.viewmodel.LibraryFilter
import com.example.ui.viewmodel.LibraryViewModel
import java.io.File

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenBook: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val studyColors = LocalStudyColors.current
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onPdfSelected(it)
        }
    }

    LaunchedEffect(uiState.importError) {
        uiState.importError?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearImportError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .testTag("import_pdf_fab")
                    .padding(WindowInsets.navigationBars.asPaddingValues()),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add PDF",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add PDF",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .linenPaperBackground(
                    baseColor = MaterialTheme.colorScheme.background,
                    studyColors = studyColors
                )
        ) {
            val shelfColumns = when {
                maxWidth >= 840.dp -> 4
                maxWidth >= 540.dp -> 3
                else -> 2
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 104.dp)
            ) {
                // 1. ARTISAN STUDY HEADER (Bookplate mark, Folio wordmark, Espresso/Cream Toggle, Search)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Bookplate Logo & Editorial Serif Wordmark
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_folio_logo),
                                        contentDescription = "Folio Reader Emblem",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Folio",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontFamily = FrauncesFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Personal Study & Bookshelf",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontStyle = FontStyle.Italic
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Tactile Ink Buttons: Cream/Espresso Toggle & Search
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("toggle_dark_mode_button")
                                        .clickable { onToggleDarkMode() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = if (isDarkMode) "Switch to Soft Cream Light" else "Switch to Espresso Dark",
                                            tint = studyColors.brassAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = if (isSearchExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("toggle_search_button")
                                        .clickable { isSearchExpanded = !isSearchExpanded }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                            contentDescription = "Search Bookshelf",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Expandable Search Slip
                        AnimatedVisibility(visible = isSearchExpanded) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .testTag("search_text_field"),
                                placeholder = {
                                    Text(
                                        text = "Search titles or authors...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }

                // 2. READING LEDGER STRIP (Cotton Paper / Dark Leather Reading Log)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LedgerStatColumn(
                                    label = "VOLUMES",
                                    value = "${uiState.books.size}",
                                    accent = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                LedgerStatColumn(
                                    label = "PAGES READ",
                                    value = "${uiState.totalPagesRead}",
                                    accent = studyColors.brassAccent
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                LedgerStatColumn(
                                    label = "FINISHED",
                                    value = "${uiState.completedBooksCount}",
                                    accent = studyColors.sageBadge
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            StitchedPaperDivider()
                        }
                    }
                }

                // 3. PAPER INDEX TABS (Clean horizontal divider tabs — no emoji clutter)
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(LibraryFilter.values()) { filter ->
                            val selected = uiState.activeFilter == filter
                            val labelText = when (filter) {
                                LibraryFilter.ALL -> "All Books"
                                LibraryFilter.MANGA -> "Manga"
                                LibraryFilter.READING -> "Reading"
                                LibraryFilter.FAVORITES -> "Favorites"
                                LibraryFilter.COMPLETED -> "Finished"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    selected && filter == LibraryFilter.MANGA -> studyColors.waxSeal
                                    selected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.onFilterChanged(filter) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = labelText,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = when {
                                            selected && filter == LibraryFilter.MANGA -> Color.White
                                            selected -> MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. EMPTY STATE
                if (uiState.books.isEmpty() && !uiState.isImporting) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (uiState.searchQuery.isNotBlank()) {
                                        "No volumes matching \"${uiState.searchQuery}\""
                                    } else {
                                        "Your Bookshelf Is Waiting"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap + Add PDF below to catalog your books, illustrated notes, or right-to-left manga.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 5. "ON YOUR DESK" FEATURED IN-PROGRESS STRIP (Only on All + empty search)
                val inProgressBooks = uiState.books.filter { it.percentRead in 1..99 }
                if (inProgressBooks.isNotEmpty() && uiState.activeFilter == LibraryFilter.ALL && uiState.searchQuery.isBlank()) {
                    item {
                        ShelfSectionTitle(
                            title = "On Your Desk",
                            countLabel = "${inProgressBooks.size} open"
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(inProgressBooks, key = { "desk_${it.id}" }) { book ->
                                DeskBookmarkCard(
                                    book = book,
                                    onOpen = { onOpenBook(book.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }

                // 6. CLASSIC WOODEN BOOKSHELF GRID WITH SPINE DETAILS
                val displayBooks = uiState.books
                if (displayBooks.isNotEmpty()) {
                    item {
                        val sectionTitle = when (uiState.activeFilter) {
                            LibraryFilter.ALL -> "Bookshelf"
                            LibraryFilter.MANGA -> "Manga Collection"
                            LibraryFilter.READING -> "Currently Reading"
                            LibraryFilter.FAVORITES -> "Favorite Volumes"
                            LibraryFilter.COMPLETED -> "Completed Archive"
                        }
                        ShelfSectionTitle(
                            title = sectionTitle,
                            countLabel = "${displayBooks.size} volumes"
                        )
                    }

                    val shelfRows = displayBooks.chunked(shelfColumns)
                    items(shelfRows.size, key = { idx -> "shelf_row_$idx" }) { rowIndex ->
                        val rowBooks = shelfRows[rowIndex]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp)
                        ) {
                            // Row of standing hardcover/paperback books
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                rowBooks.forEach { book ->
                                    BookshelfVolumeCard(
                                        book = book,
                                        modifier = Modifier.weight(1f),
                                        onOpen = { onOpenBook(book.id) },
                                        onToggleFavorite = { viewModel.toggleFavorite(book) },
                                        onDelete = { bookToDelete = book }
                                    )
                                }
                                // Keep uniform width on partially filled final shelf row
                                repeat(shelfColumns - rowBooks.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            // Physical wooden shelf ledge directly beneath the books
                            WoodenShelfPlank(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                height = 20.dp
                            )

                            // Clean paper catalog labels tucked under the plank
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                rowBooks.forEach { book ->
                                    ShelfCatalogCaption(
                                        book = book,
                                        modifier = Modifier.weight(1f),
                                        onOpen = { onOpenBook(book.id) },
                                        onToggleFavorite = { viewModel.toggleFavorite(book) },
                                        onDelete = { bookToDelete = book }
                                    )
                                }
                                repeat(shelfColumns - rowBooks.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // PDF Inspection / Import Loading Overlay
            if (uiState.isImporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Cataloging Volume...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Preparing crisp offline pages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = studyColors.waxSeal
                )
            },
            title = {
                Text(
                    text = "Remove from Shelf?",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Remove \"${book.title}\" along with all highlights, sticky notes, and bookmarks from this device?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBook(book)
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = studyColors.waxSeal,
                        contentColor = Color.White
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Keep Book")
                }
            }
        )
    }

    // Catalog Slip Import Dialog (asks Standard Book vs Manga before adding)
    uiState.pendingDraft?.let { draft ->
        PdfImportTypeDialog(
            draft = draft,
            onDismiss = { viewModel.dismissImportDraft() },
            onConfirm = { isManga, title, author ->
                viewModel.confirmImport(draft, isManga, title, author) { newBookId ->
                    onOpenBook(newBookId)
                }
            }
        )
    }
}

@Composable
private fun LedgerStatColumn(
    label: String,
    value: String,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShelfSectionTitle(
    title: String,
    countLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = countLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Standing tactile hardcover/paperback book volume on a wooden shelf plank.
 * Features spine groove, ribbon bookmark, and clean horizontal stamps.
 */
@Composable
fun BookshelfVolumeCard(
    book: BookEntity,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val studyColors = LocalStudyColors.current
    val coverShape = if (book.isManga) {
        RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 3.dp, bottomEnd = 3.dp)
    } else {
        RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 10.dp, bottomEnd = 10.dp)
    }

    Box(
        modifier = modifier
            .testTag("book_card_${book.id}")
            .shadow(elevation = 6.dp, shape = coverShape)
            .clip(coverShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.8.dp, MaterialTheme.colorScheme.outline, coverShape)
            .clickable { onOpen() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
        ) {
            // Cover artwork or linen cloth fallback cover
            if (book.coverPath != null && File(book.coverPath).exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(book.coverPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (book.isManga) {
                                    listOf(Color(0xFF3E2626), Color(0xFF231515))
                                } else {
                                    listOf(Color(0xFF2D423A), Color(0xFF1A2823))
                                }
                            )
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (book.isManga) Icons.Default.AutoStories else Icons.Default.Book,
                            contentDescription = null,
                            tint = Color(0xFFEBD9B4),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFAF5EC),
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Physical spine groove & paper fore-edge highlight
            BookSpineOverlay(isManga = book.isManga)

            // Hanging fabric bookmark ribbon for books currently being read
            if (book.percentRead in 1..99) {
                HangingRibbonBookmark(
                    color = if (book.isManga) studyColors.waxSeal else studyColors.brassAccent,
                    modifier = Modifier
                        .align(if (book.isManga) Alignment.TopStart else Alignment.TopEnd)
                        .padding(horizontal = 12.dp)
                )
            }

            // Top row: Manga horizontal stamp + Favorite toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (book.isManga) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = studyColors.waxSeal
                    ) {
                        Text(
                            text = "MANGA",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (book.isFavorite) Color(0xFFFCA5A5) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Bottom horizontal metadata badges (single-line, never vertical)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xCC1E1814)
                ) {
                    Text(
                        text = "${book.pageCount}p",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color(0xFFF5EFE6),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (book.percentRead >= 100) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = studyColors.sageBadge
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "DONE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean catalog nameplate sitting right beneath the wooden shelf plank for each book.
 */
@Composable
private fun ShelfCatalogCaption(
    book: BookEntity,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val studyColors = LocalStudyColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clickable { onOpen() }
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Book options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = { Text(if (book.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                        onClick = {
                            menuExpanded = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove Book", color = studyColors.waxSeal) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }

        Text(
            text = book.author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { (book.percentRead / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (book.percentRead >= 100) studyColors.sageBadge else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (book.currentPage > 0) "p.${book.currentPage + 1}/${book.pageCount}" else "Unread",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${book.percentRead}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (book.percentRead >= 100) studyColors.sageBadge else MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Compact paper bookmark card on the "On Your Desk" horizontal strip.
 */
@Composable
fun DeskBookmarkCard(
    book: BookEntity,
    onOpen: () -> Unit
) {
    val studyColors = LocalStudyColors.current
    Surface(
        modifier = Modifier
            .width(272.dp)
            .clickable { onOpen() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(88.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(0.8.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (book.coverPath != null && File(book.coverPath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(book.coverPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (book.isManga) Icons.Default.AutoStories else Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                BookSpineOverlay(isManga = book.isManga)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (book.isManga) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = studyColors.waxSeal
                        ) {
                            Text(
                                text = "MANGA",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "p.${book.currentPage + 1} / ${book.pageCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (book.percentRead / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${book.percentRead}% read",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Resume",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Archival Catalog Slip Dialog asking whether an imported PDF is a Standard Book (LTR) or Manga (RTL).
 */
@Composable
fun PdfImportTypeDialog(
    draft: PdfImportDraft,
    onDismiss: () -> Unit,
    onConfirm: (isManga: Boolean, title: String, author: String) -> Unit
) {
    val studyColors = LocalStudyColors.current
    var selectedIsManga by remember { mutableStateOf(draft.isManga) }
    var titleText by remember { mutableStateOf(draft.suggestedTitle) }
    var authorText by remember { mutableStateOf(draft.suggestedAuthor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "Catalog New Volume",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${draft.pageCount} PAGES  •  ${draft.fileSizeFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                StitchedPaperDivider()
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select reading format:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Option 1: Standard Book (Left-to-Right, Full Color Diagrams)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (!selectedIsManga) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                    border = BorderStroke(
                        width = if (!selectedIsManga) 1.5.dp else 1.dp,
                        color = if (!selectedIsManga) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedIsManga = false }
                        .testTag("select_normal_book_type")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (!selectedIsManga) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = if (!selectedIsManga) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Standard Book / PDF",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Left-to-Right • Full color diagrams & illustrations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!selectedIsManga) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Option 2: Manga / Comic (Right-to-Left)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedIsManga) studyColors.waxSeal.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background,
                    border = BorderStroke(
                        width = if (selectedIsManga) 1.5.dp else 1.dp,
                        color = if (selectedIsManga) studyColors.waxSeal else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedIsManga = true }
                        .testTag("select_manga_type")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (selectedIsManga) studyColors.waxSeal else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = if (selectedIsManga) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manga / Comic",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Right-to-Left (RTL) • Webtoon scroll option",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedIsManga) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = studyColors.waxSeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_title_field")
                )

                OutlinedTextField(
                    value = authorText,
                    onValueChange = { authorText = it },
                    label = { Text(if (selectedIsManga) "Mangaka / Artist" else "Author") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_author_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedIsManga, titleText, authorText) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedIsManga) studyColors.waxSeal else MaterialTheme.colorScheme.primary,
                    contentColor = if (selectedIsManga) Color.White else MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_import_button")
            ) {
                Text(
                    text = if (selectedIsManga) "Add as Manga (RTL)" else "Add as Book (LTR)",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_import_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
