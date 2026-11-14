package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val libraryViewModel: LibraryViewModel = viewModel()
            val libraryUiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
            val systemIsDark = isSystemInDarkTheme()
            val isDark = libraryUiState.isDarkMode ?: systemIsDark

            MyApplicationTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FolioApp(
                        libraryViewModel = libraryViewModel,
                        isDarkMode = isDark,
                        onToggleDarkMode = { libraryViewModel.toggleDarkMode(systemIsDark) }
                    )
                }
            }
        }
    }
}

@Composable
fun FolioApp(
    libraryViewModel: LibraryViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var activeBookId by rememberSaveable { mutableStateOf<Long?>(null) }
    val readerViewModel: ReaderViewModel = viewModel()

    AnimatedContent(
        targetState = activeBookId,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width / 3 } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "AppScreenTransition"
    ) { bookId ->
        if (bookId == null) {
            LibraryScreen(
                viewModel = libraryViewModel,
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onOpenBook = { id -> activeBookId = id }
            )
        } else {
            ReaderScreen(
                bookId = bookId,
                viewModel = readerViewModel,
                onBack = { activeBookId = null }
            )
        }
    }
}
