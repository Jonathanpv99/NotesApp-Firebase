package com.example.notesapp.model

data class NoteModel(
    val userId: String,
    val title: String,
    val note: String,
    val imageUrl: String,
    val audioUrl: String,
    val reminder: String,
    val createAt: String
){
    fun toMap(): MutableMap<String, Any>{
        return mutableMapOf(
            "userId" to this.userId,
            "title" to this.title,
            "note" to this.note,
            "imageUrl" to this.imageUrl,
            "audioUrl" to this.audioUrl,
            "reminder" to this.reminder,
            "createAt" to this.createAt
        )
    }
}
