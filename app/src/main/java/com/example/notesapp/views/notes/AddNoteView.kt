package com.example.notesapp.views.notes

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.notesapp.R
import com.example.notesapp.viewModels.NotesViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteView(
    navController: NavController,
    notesVM: NotesViewModel
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showRecordingModal by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scrollNote = rememberScrollState()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let {
            imageUri = it
            imageBitmap = null // Reset imageBitmap if a new image is picked
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        it?.let {
            imageBitmap = it
            imageUri = null // Reset imageUri if a new photo is taken
        }
    }

    val calendar = Calendar.getInstance()
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Nota") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth().padding(30.dp)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Nota") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .size(250.dp)
                    .verticalScroll(scrollNote)
            )

            Spacer(modifier = Modifier.height(15.dp))
            Text("Imagen", modifier = Modifier.padding(start = 30.dp).graphicsLayer(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { imagePicker.launch("image/*") }) {
                    Icon(painter = painterResource(id = R.drawable.photo), contentDescription = "Seleccionar Imagen", tint = Color.White)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Galería", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Button(onClick = { takePicture.launch(null) }) {
                    Icon(painter = painterResource(id = R.drawable.camera), contentDescription = "Tomar Foto", tint = Color.White)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Cámara", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            imageBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(250.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
            imageUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(250.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(17.dp))
            Text("Audio",modifier = Modifier.padding(start = 30.dp).graphicsLayer(alpha = 0.5f))
            Button(
                onClick = { showRecordingModal = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.mic), contentDescription = "micro", tint = Color.White)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Grabar Audio", fontWeight = FontWeight.Bold, color = Color.White)
            }

            if (showRecordingModal) {
                AudioRecordingDialog(onDismiss = { showRecordingModal = false })
            }
            OutlinedTextField(
                value = selectedDate,
                onValueChange = {},
                enabled = false,
                label = { Text("Fecha limite") },
                supportingText = { Text("** Se Ejecutaran notificaciones anes de la fecha limite") },
                modifier = Modifier.fillMaxWidth().padding(30.dp).clickable { datePicker.show() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    notesVM.saveNewNote(title, note) {
                        Toast.makeText(context, "Guardado", Toast.LENGTH_SHORT).show()
                    }
                    navController.navigate("Home")
                },
                colors = ButtonColors(
                    containerColor = Color(0xFF0B8F26),
                    contentColor = Color.White,
                    disabledContentColor = Color.Gray,
                    disabledContainerColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth().padding(30.dp),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Guardar", tint = Color.White)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Guardar Nota", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AudioRecordingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Grabación de Audio") },
        text = { Text("Presiona 'Grabar' para iniciar.") },
        confirmButton = {
            Button(onClick = { /* Implementar grabación */ }) {
                Text("Grabar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancelar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}
