package com.example.notesapp.viewModels

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.model.UserModel
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    var showAlert by mutableStateOf(false)
    var typeOfLogin by mutableStateOf(0)

    fun loginWithEmailAndPassword(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onSuccess()
                        } else {
                            typeOfLogin = 0
                            showAlert = true
                        }
                    }
            } catch (e: Exception) {
                Log.d("Error en jectpack", "Error: ${e.localizedMessage}")
            }
        }
    }

    private fun saveUser(username: String?) {
        val id = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email

        viewModelScope.launch(Dispatchers.IO) {
            // Extraer el nombre de usuario desde el email si username es nulo o vacío
            val defaultUsername = email?.substringBefore("@") ?: "UsuarioDesconocido"
            val finalUsername = username?.takeIf { it.isNotBlank() } ?: defaultUsername

            val userRef = FirebaseFirestore.getInstance().collection("Users").document(id)

            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d("Usuario", "El usuario ya existe en la base de datos")
                } else {
                    val user = UserModel(
                        userId = id,
                        email = email.toString(),
                        username = finalUsername
                    )

                    userRef.set(user)
                        .addOnSuccessListener {
                            Log.d("Guardo", "Guardó correctamente")
                        }.addOnFailureListener {
                            Log.d("Error al guardar", "Error al guardar en Firestore")
                        }
                }
            }.addOnFailureListener {
                Log.d("Error", "Error al verificar existencia del usuario")
            }
        }
    }

    fun createUser(email: String, password: String, username: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            saveUser(username)
                            onSuccess()
                        } else {
                            showAlert = true
                        }
                    }
            } catch (e: Exception) {
                Log.d("Error en jectpack", "Error: ${e.localizedMessage}")
            }
        }
    }

    fun closeAlert() {
        showAlert = false
    }

    //login with google
    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            saveUser(null)
                            onSuccess()
                        } else {
                            typeOfLogin = 1
                            showAlert = true
                        }
                    }
            } catch (e: Exception) {
                Log.d("Error en Google Auth", "Error: ${e.localizedMessage}")
                onError(e.localizedMessage ?: "Error desconocido")
            }
        }
    }

    fun handleGoogleSignInResult(data: Intent?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val task =
                com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(
                    data
                )
            val account = task.getResult(ApiException::class.java)

            // Google Sign In fue exitoso, autenticar con Firebase
            account?.idToken?.let { idToken ->
                loginWithGoogle(idToken, onSuccess, onError)
            } ?: onError("Token de Google es nulo")

        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "Google sign in failed", e)
            onError("Error en el inicio de sesión con Google: ${e.statusCode}")
        }
    }

}