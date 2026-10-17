package com.notedown.markpad

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.notedown.markpad.data.AndroidExternalFileGateway
import com.notedown.markpad.data.RoomNoteRepository
import com.notedown.markpad.data.SharedPreferencesUserPreferencesRepository
import com.notedown.markpad.data.local.MarkPadDatabase
import com.notedown.markpad.model.AppFont
import com.notedown.markpad.model.AppTheme
import com.notedown.markpad.ui.MarkPadScreen
import com.notedown.markpad.ui.theme.MarkPadTheme
import com.notedown.markpad.viewmodel.NoteUiState
import com.notedown.markpad.viewmodel.NoteViewModel
import com.notedown.markpad.viewmodel.NoteViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: NoteViewModel by viewModels {
        val database = MarkPadDatabase.getInstance(applicationContext)
        val repository = RoomNoteRepository(database.noteDao())
        val preferencesRepository = SharedPreferencesUserPreferencesRepository(applicationContext)
        val externalFileGateway = AndroidExternalFileGateway(applicationContext)
        NoteViewModelFactory(repository, preferencesRepository, externalFileGateway)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIncomingFileIntent(intent)
        }
        setContent {
            val state by viewModel.uiState.collectAsState()
            val (currentTheme, currentUiFont) = when (val s = state) {
                is NoteUiState.Ready -> s.appTheme to s.uiFont
                else -> AppTheme.DARK to AppFont.SANS_SERIF
            }

            MarkPadTheme(appTheme = currentTheme, uiFont = currentUiFont) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MarkPadScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingFileIntent(intent)
    }

    private fun handleIncomingFileIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_EDIT) {
            val uri = intent.data ?: return
            viewModel.openExternalFile(uri.toString())
        }
    }
}
