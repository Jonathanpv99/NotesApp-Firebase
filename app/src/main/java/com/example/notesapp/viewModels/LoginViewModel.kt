package com.example.notesapp.viewModels

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class LoginViewModel: ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    fun loginWithEmailAndPasswprd(email: String, password: String, onSuccess: () -> Unit){
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener{ task ->
                        if(task.isSuccessful){
                            onSuccess()
                        }else{

                        }
                    }
            }catch (e: Exception){
                Log.d("Error en jectpack","Error: ${e.localizedMessage}")
            }
        }
    }

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onSuccess()
                        } else {
                            onError(task.exception?.message ?: "Error desconocido en autenticación con Google")
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
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
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