package com.example.notesapp.model

data class NotesState(
    val userId: String = "",
    val title: String = "",
    val note: String = "",
    val createAt: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val reminder: String = "",
    val idNote: String = ""
)
