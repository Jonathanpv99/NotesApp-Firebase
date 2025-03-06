package com.example.notesapp.model

data class NoteModel(
    val userId: String,
    val title: String,
    val note: String,
    val date: String
){
    fun toMap(): MutableMap<String, Any>{
        return mutableMapOf(
            "userId" to this.userId,
            "title" to this.title,
            "note" to this.note,
            "date" to this.date
        )
    }
}
