package com.notedown.markpad.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val updatedAt: Long
)
