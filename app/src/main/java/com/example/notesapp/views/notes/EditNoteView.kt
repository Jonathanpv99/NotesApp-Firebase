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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun EditNoteView(
    navController: NavController,
    notesVM: NotesViewModel,
    idNote: String
) {

    LaunchedEffect(Unit) {
        notesVM.getNoteById(idNote)
    }
    //estado
    val state = notesVM.state
    //estados para mostrar la imagen completa
    var showImage by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<Any?>(null) }

    var imageUri by remember(state.imageUrl) {
        mutableStateOf(
            state.imageUrl.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
        )
    }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // Estados de grabación y reproducción
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var audioFilePath by remember(state.audioUrl) {
        mutableStateOf(state.audioUrl)
    }

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

    val takePicture =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
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
             notesVM.onValue("$dayOfMonth/${month + 1}/$year", "reminder")
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
            mediaPlayer.reset() // 👈 importante: reseteamos antes de usar

            mediaPlayer.setDataSource(audioFilePath)
            mediaPlayer.setOnPreparedListener {
                it.start()
                isPlaying = true
            }
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
            }
            mediaPlayer.setOnErrorListener { mp, what, extra ->
                Log.e("AddNoteView", "Error en reproducción: what=$what, extra=$extra")
                isPlaying = false
                true
            }
            mediaPlayer.prepare() // para archivo local
            mediaPlayer.start()
            isPlaying = true

        } catch (e: IOException) {
            Log.e("AddNoteView", "Error al reproducir audio: ${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("AddNoteView", "MediaPlayer en estado ilegal: ${e.message}")
        }
    }


    val stopPlayback: () -> Unit = {
        mediaPlayer.apply {
            stop()
            reset()
        }
        isPlaying = false
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Nota") },
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
                value = state.title,
                onValueChange = { notesVM.onValue( it, "title") },
                label = { Text("Título") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp)
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = { notesVM.onValue( it, "note") },
                label = { Text("Nota") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .size(250.dp)
                    .verticalScroll(scrollNote)
            )

            Spacer(modifier = Modifier.height(15.dp))
            Text("Imagen", modifier = Modifier
                .padding(start = 30.dp)
                .graphicsLayer(alpha = 0.5f))
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
            Row(modifier = Modifier.padding(start = 60.dp)) {
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
                            modifier = Modifier
                                .size(250.dp)
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
            Text("Audio", modifier = Modifier
                .padding(start = 30.dp)
                .graphicsLayer(alpha = 0.5f))

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

                if (audioFilePath != "" && !isRecording) {
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
                            audioFilePath = ""
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
                value = state.reminder,
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
                    notesVM.editNoteWithMedia(
                        idNote = idNote,
                        imageUri = imageUri,
                        imageBitmap = imageBitmap,
                        audioFilePath = audioFilePath,
                        context = context,
                        onSuccess = {
                            Toast.makeText(context, "Nota editada con éxito", Toast.LENGTH_SHORT)
                                .show()
                            navController.navigate("Home")
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC99705),
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, start = 30.dp, end = 30.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Editar Nota", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    notesVM.deleteNoteWithMedia(
                        idNote = idNote,
                        onSuccess = {
                            Toast.makeText(context, "Nota eliminada con éxito", Toast.LENGTH_SHORT)
                                .show()
                            navController.navigate("Home")
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFA1506),
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Eliminar Nota", fontWeight = FontWeight.Bold)
            }
        }
    }
}


