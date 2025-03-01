package com.example.notesapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.notesapp.viewModels.LoginViewModel
import com.example.notesapp.viewModels.NotesViewModel
import com.example.notesapp.views.login.TabsView
import com.example.notesapp.views.notes.HomeView

@Composable
fun NavManager(
    loginVM: LoginViewModel,
    notesVM: NotesViewModel
){
    val navController = rememberNavController()
    NavHost(
        navController= navController,
        startDestination = "Login"
    ) {
        composable("Login"){
            TabsView(navController, loginVM)
        }
        composable("Home"){
            HomeView()
        }
    }
}