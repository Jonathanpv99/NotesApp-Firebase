package com.example.notesapp.views.notes

import android.Manifest
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.notesapp.components.ImageCompletly
import com.example.notesapp.viewModels.NotesViewModel
import java.io.File
import java.io.IOException
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteView(
    navController: NavController,
    notesVM: NotesViewModel
) {
    //estados para mostrar la imagen completa
    var showImage by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<Any?>(null) }

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // Estados de grabación y reproducción
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var audioFilePath by remember { mutableStateOf<String?>(null) }

    val mediaRecorder = remember { MediaRecorder() }
    val mediaPlayer = remember { MediaPlayer() }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scrollNote = rememberScrollState()

    // Permiso de grabación
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permiso de grabación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Activity result launchers
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

    // Date picker setup
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

    val startRecording: () -> Unit = {
        val audioFile = File(context.externalCacheDir, "note_audio.3gp")
        audioFilePath = audioFile.absolutePath

        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(audioFilePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }
            isRecording = true
            Toast.makeText(context, "Grabando...", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("AddNoteView", "Error al grabar audio: ${e.message}")
        }
    }

    val stopRecording: () -> Unit = {
        try {
            mediaRecorder.apply {
                stop()
                release()
            }
            isRecording = false
            Toast.makeText(context, "Grabación finalizada", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("AddNoteView", "Error al detener la grabación: ${e.message}")
        }
    }

    val startPlayback: () -> Unit = {
        try {
            mediaPlayer.apply {
                setDataSource(audioFilePath)
                prepare()
                start()
                isPlaying = true
                setOnCompletionListener {
                    isPlaying = false
                }
            }
        } catch (e: IOException) {
            Log.e("AddNoteView", "Error al reproducir audio: ${e.message}")
        }
    }

    val stopPlayback: () -> Unit = {
        mediaPlayer.apply {
            stop()
            reset()
        }
        isPlaying = false
    }

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
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp)
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
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                    imagePicker.launch("image/*")
                }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.photo),
                        contentDescription = "Seleccionar Imagen",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Galería", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Button(onClick = {
                    takePicture.launch(null)
                }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.camera),
                        contentDescription = "Tomar Foto",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Cámara", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Display selected image
            Row (modifier = Modifier.padding(start = 60.dp)) {
                when {
                    imageBitmap != null -> {
                        Image(
                            bitmap = imageBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(250.dp)
                                .clickable {
                                    selectedImage = imageBitmap
                                    showImage = true
                                }
                        )
                        IconButton(
                            onClick = { imageBitmap = null },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar imagen",
                                tint = Color.Red
                            )
                        }
                    }

                    imageUri != null -> {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.size(250.dp)
                                .padding(start = 10.dp)
                                .clickable {
                                    selectedImage = imageUri
                                    showImage = true
                                }
                        )
                        IconButton(
                            onClick = { imageUri = null },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar imagen",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }

            if (showImage) {
                ImageCompletly(
                    image = selectedImage,
                    onDismiss = { showImage = false }
                )
            }

            Spacer(modifier = Modifier.height(17.dp))
            Text("Audio", modifier = Modifier.padding(start = 30.dp).graphicsLayer(alpha = 0.5f))

            // Audio recording section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
            ) {
                // Audio recording button
                Button(
                    onClick = {
                        if (isRecording) {
                            stopRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            startRecording()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.mic),
                        contentDescription = "Micro",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        if (isRecording) "Detener Grabación" else "Grabar Audio",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (audioFilePath != null && !isRecording) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Audio grabado", color = Color.Green)
                            IconButton(onClick = {
                                if (isPlaying) {
                                    stopPlayback()
                                } else {
                                    startPlayback()
                                }
                            }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Clear else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Detener" else "Reproducir",
                                    tint = Color.Green
                                )
                            }
                        }
                        IconButton(onClick = {
                            // Detener reproducción si está activa
                            if (isPlaying) {
                                //audioPlayer.stop()
                                isPlaying = false
                            }
                            // Eliminar el archivo
                            try {
                                val file = File(audioFilePath ?: "")
                                if (file.exists()) {
                                    file.delete()
                                }
                            } catch (e: Exception) {
                                Log.e("AddNoteView", "Error al eliminar archivo: ${e.message}")
                            }
                            // Resetear estado
                            audioFilePath = null
                        }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar audio",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = selectedDate,
                onValueChange = {},
                enabled = false,
                label = { Text("Fecha limite") },
                supportingText = { Text("** Se mandaran recordatorios antes de esta fecha") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp)
                    .clickable { datePicker.show() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Handle save with all media files
                    notesVM.saveNewNoteWithMedia(
                        title = title,
                        note = note,
                        imageUri = imageUri,
                        imageBitmap = imageBitmap,
                        audioFilePath = audioFilePath,
                        reminder = selectedDate,
                        context = context,
                        onSuccess = {
                            Toast.makeText(context, "Nota guardada con éxito", Toast.LENGTH_SHORT).show()
                            navController.navigate("Home")
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0B8F26),
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Guardar", tint = Color.White)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Guardar Nota", fontWeight = FontWeight.Bold)
            }
        }
    }
}


