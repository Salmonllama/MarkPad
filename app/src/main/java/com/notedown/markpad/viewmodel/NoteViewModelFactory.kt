package com.notedown.markpad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.notedown.markpad.data.InMemoryUserPreferencesRepository
import com.notedown.markpad.data.NoteRepository
import com.notedown.markpad.data.UserPreferencesRepository

class NoteViewModelFactory(
    private val repository: NoteRepository,
    private val preferencesRepository: UserPreferencesRepository = InMemoryUserPreferencesRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            return NoteViewModel(repository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
