package com.example.notesapp.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

@Composable
fun ImageCompletly(image: Any?, onDismiss: () -> Unit){
    Dialog(
        onDismissRequest = { onDismiss() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (image is Bitmap) {
                val highResBitmap = scaleBitmap(image, 4f) // Duplicamos la resolución
                ZoomableImage(highResBitmap)
            } else if (image is Uri) {
                ZoomableImageUri(uri = image)
            }

            IconButton(
                onClick = { onDismiss() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White
                )
            }
        }
    }
}

//Imagen con zoom (para Bitmap)
@Composable
fun ZoomableImage(bitmap: Bitmap) {
    var scale by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale *= zoom
                }
            }
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(), // ✅ Convertimos Bitmap a ImageBitmap
            contentDescription = "Imagen ampliada",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun ZoomableImageUri(uri: Uri) {
    var scale by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale *= zoom
                }
            }
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Imagen ampliada",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

fun scaleBitmap(bitmap: Bitmap, scaleFactor: Float): Bitmap {
    val width = (bitmap.width * scaleFactor).toInt()
    val height = (bitmap.height * scaleFactor).toInt()
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}