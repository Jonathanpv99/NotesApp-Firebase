package com.example.notesapp.viewModels

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.model.NoteModel
import com.example.notesapp.model.NotesState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NotesViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _notesData = MutableStateFlow<List<NotesState>>(emptyList())
    val notesData: StateFlow<List<NotesState>> = _notesData

    var state by mutableStateOf(NotesState())
        private set

    fun onValue(value: String, text: String){
        when(text){
            "title" -> state = state.copy(title = value)
            "note" -> state = state.copy(note = value)
            "reminder" -> state = state.copy(reminder = value)
        }
    }

    fun getNoteById( documentId: String ){
        firestore.collection("Notes")
            .document(documentId)
            .addSnapshotListener { snapshot, _ -> //el guin bajo sirve para evitar el codigo del error
                if(snapshot != null){
                    val note = snapshot.toObject(NotesState::class.java)
                    state = state.copy(
                        title = note?.title ?: "",
                        note = note?.note ?: "",
                        createAt = note?.createAt ?: "",
                        imageUrl = note?.imageUrl ?: "",
                        audioUrl = note?.audioUrl ?: "",
                        reminder = note?.reminder ?: ""
                    )
                }
            }
    }

    fun fetchNotes() {
        val userId = auth.currentUser?.uid
        firestore.collection("Notes")
            .whereEqualTo("userId", userId)
            .orderBy("createAt", Query.Direction.DESCENDING)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val documents = mutableListOf<NotesState>()
                if (querySnapshot != null) {
                    for (document in querySnapshot) {
                        val myDocument = document.toObject(NotesState::class.java)
                            .copy(idNote = document.id)
                        documents.add(myDocument)
                    }
                }
                _notesData.value = documents
            }
    }

    // Método para guardar una nota con archivos multimedia localmente
    fun saveNewNoteWithMedia(
        title: String,
        note: String,
        imageUri: Uri?,
        imageBitmap: Bitmap?,
        audioFilePath: String?,
        reminder: String = "",
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: "1234567"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Guardar imágenes localmente
                val imageLocalPath = when {
                    imageUri != null -> saveUriToLocalStorage(imageUri, "images", userId, context)
                    imageBitmap != null -> saveBitmapToLocalStorage(imageBitmap, "images", userId, context)
                    else -> ""
                }

                // Si hay un audio grabado, copiarlo a la carpeta de la aplicación
                val audioLocalPath = if (audioFilePath != null) {
                    copyAudioToLocalStorage(audioFilePath, "audios", userId, context)
                } else {
                    ""
                }

                // Crear y guardar la nota con las rutas locales
                val newNote = NoteModel(
                    userId = userId,
                    title = title,
                    note = note,
                    imageUrl = imageLocalPath,
                    audioUrl = audioLocalPath,
                    reminder = reminder,
                    createAt = formatDate()
                )

                // Guardar en Firestore
                firestore.collection("Notes").add(newNote.toMap())
                    .addOnSuccessListener {
                        viewModelScope.launch(Dispatchers.Main) {
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { e ->
                        viewModelScope.launch(Dispatchers.Main) {
                            onError("Error al guardar la nota: ${e.message}")
                        }
                    }

            } catch (e: Exception) {
                Log.e("Error Save", "Error al guardar ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    onError("Error al guardar: ${e.localizedMessage}")
                }
            }
        }
    }

    // Función para eliminar un archivo local si existe
    private fun deleteLocalFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    fun editNoteWithMedia(
        idNote: String,
        imageUri: Uri?,
        imageBitmap: Bitmap?,
        audioFilePath: String?,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: "1234567"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Detectar cambios en imagen
                val imageLocalPath = when {
                    imageUri != null -> {
                        val imageUrlParsed = state.imageUrl.let { Uri.parse(it) }

                        if (imageUrlParsed?.toString() != imageUri.toString()) {
                            saveUriToLocalStorage(imageUri, "images", userId, context)
                        } else {
                            state.imageUrl
                        }
                    }
                    imageBitmap != null -> saveBitmapToLocalStorage(imageBitmap, "images", userId, context)
                    else -> {
                        // Si se eliminó la imagen, eliminar el archivo local
                        state.imageUrl.let { deleteLocalFile(it) }
                        ""
                    }
                }

                // Detectar cambios en audio
                val audioLocalPath = when {
                    audioFilePath.isNullOrEmpty() -> {
                        // Si se eliminó el audio, eliminar el archivo local
                        state.audioUrl.let { deleteLocalFile(it) }
                        ""
                    }
                    audioFilePath != state.audioUrl -> copyAudioToLocalStorage(audioFilePath, "audios", userId, context)
                    else -> state.audioUrl // No hubo cambio
                }


                // Crear nota editada
                val editNote = NoteModel(
                    userId = userId,
                    title = state.title,
                    note = state.note,
                    imageUrl = imageLocalPath,
                    audioUrl = audioLocalPath,
                    reminder = state.reminder,
                    createAt = state.createAt
                )

                // Actualizar en Firestore
                firestore.collection("Notes").document(idNote).set(editNote.toMap())
                    .addOnSuccessListener {
                        viewModelScope.launch(Dispatchers.Main) {
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { e ->
                        viewModelScope.launch(Dispatchers.Main) {
                            onError("Error al editar la nota: ${e.message}")
                        }
                    }

            } catch (e: Exception) {
                Log.e("Error Edit", "Error al editar ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    onError("Error al geditar: ${e.localizedMessage}")
                }
            }
        }
    }

    fun deleteNoteWithMedia(
        idNote: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
               //eliminar archivos locales
                state.imageUrl.let { deleteLocalFile(it) }
                state.audioUrl.let { deleteLocalFile(it) }

                // Eliminar en Firestore
                firestore.collection("Notes").document(idNote).delete()
                    .addOnSuccessListener {
                        viewModelScope.launch(Dispatchers.Main) {
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { e ->
                        viewModelScope.launch(Dispatchers.Main) {
                            onError("Error al eliminar la nota: ${e.message}")
                        }
                    }

            } catch (e: Exception) {
                Log.e("Error Delete", "Error al eliminar ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    onError("Error al eliminar: ${e.localizedMessage}")
                }
            }
        }
    }


    // Función para guardar un Uri en almacenamiento local
    private suspend fun saveUriToLocalStorage(uri: Uri, folderName: String, userId: String, context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                // Crear directorios si no existen
                val directory = File(context.getExternalFilesDir(null), "$userId/$folderName")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                // Crear nombre de archivo único
                val filename = "${UUID.randomUUID()}.jpg"
                val file = File(directory, filename)

                // Copiar contenido desde Uri a archivo local
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(file)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                // Devolver la ruta del archivo
                file.absolutePath
            } catch (e: Exception) {
                Log.e("Storage", "Error guardando archivo: ${e.message}")
                throw e
            }
        }
    }

    // Función para guardar un Bitmap en almacenamiento local
    private suspend fun saveBitmapToLocalStorage(bitmap: Bitmap, folderName: String, userId: String, context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                // Crear directorios si no existen
                val directory = File(context.getExternalFilesDir(null), "$userId/$folderName")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                // Crear nombre de archivo único
                val filename = "${UUID.randomUUID()}.jpg"
                val file = File(directory, filename)

                // Guardar bitmap a archivo
                FileOutputStream(file).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                }

                // Devolver la ruta del archivo
                file.absolutePath
            } catch (e: Exception) {
                Log.e("Storage", "Error guardando bitmap: ${e.message}")
                throw e
            }
        }
    }

    // Función para copiar archivo de audio a almacenamiento local
    private suspend fun copyAudioToLocalStorage(filePath: String, folderName: String, userId: String, context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(filePath)

                // Crear directorios si no existen
                val directory = File(context.getExternalFilesDir(null), "$userId/$folderName")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                // Crear nombre de archivo único
                val extension = sourceFile.name.substringAfterLast(".", "3gp")
                val filename = "${UUID.randomUUID()}.$extension"
                val destinationFile = File(directory, filename)

                // Copiar archivo
                sourceFile.inputStream().use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Devolver la ruta del archivo
                destinationFile.absolutePath
            } catch (e: Exception) {
                Log.e("Storage", "Error copiando audio: ${e.message}")
                throw e
            }
        }
    }

    // Formatear fecha para timestamp de creación de nota
    private fun formatDate(): String {
        val currentDate = Date()
        val res = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return res.format(currentDate)
    }

    fun logOut() {
        auth.signOut()
    }
}