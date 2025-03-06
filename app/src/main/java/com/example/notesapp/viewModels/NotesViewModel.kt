package com.example.notesapp.viewModels

import android.icu.util.Calendar
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.model.NoteModel
import com.example.notesapp.model.NotesState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class NotesViewModel: ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _notesData = MutableStateFlow<List<NotesState>>(emptyList())
    val notesData: StateFlow<List<NotesState>> = _notesData

    fun fetchNotes(){
        val userId = auth.currentUser?.uid
        firestore.collection("Notes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { querySnapshot, error ->
                if(error != null){
                    return@addSnapshotListener
                }
                val documents = mutableListOf<NotesState>()
                if(querySnapshot != null){
                    for(document in querySnapshot){
                        val myDocumet = document.toObject(NotesState::class.java)
                            .copy(idNote = document.id)
                        documents.add(myDocumet)
                    }
                }
                _notesData.value = documents
            }
    }

    fun saveNewNote(title: String, note: String, onSuccess: () -> Unit){
        val userId = auth.currentUser?.uid
        viewModelScope.launch(Dispatchers.IO) {
            try{
                val newNote = NoteModel(
                    userId = userId ?: "1234567",
                    title= title,
                    note = note,
                    date = formatDate(),
                )
                firestore.collection("Notes").add(newNote)
                    .addOnSuccessListener {
                        onSuccess()
                    }
            }catch (e: Exception){
                Log.d("Error Save", "Error al guardar ${e.localizedMessage}")
            }
        }
    }

    private fun formatDate(): String{
        val currentDate: java.util.Date = Calendar.getInstance().time
        val res = SimpleDateFormat("dd/mm/yyyy", Locale.getDefault())
        return res.format(currentDate)
    }

    fun logOut(){
        auth.signOut()
    }
}