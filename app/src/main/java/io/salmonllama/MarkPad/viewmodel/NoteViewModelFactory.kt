package io.salmonllama.MarkPad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.salmonllama.MarkPad.data.ExternalFileGateway
import io.salmonllama.MarkPad.data.InMemoryExternalFileGateway
import io.salmonllama.MarkPad.data.InMemoryUserPreferencesRepository
import io.salmonllama.MarkPad.data.NoteRepository
import io.salmonllama.MarkPad.data.UserPreferencesRepository

class NoteViewModelFactory(
    private val repository: NoteRepository,
    private val preferencesRepository: UserPreferencesRepository = InMemoryUserPreferencesRepository(),
    private val externalFileGateway: ExternalFileGateway = InMemoryExternalFileGateway()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            return NoteViewModel(
                repository = repository,
                preferencesRepository = preferencesRepository,
                externalFileGateway = externalFileGateway
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
