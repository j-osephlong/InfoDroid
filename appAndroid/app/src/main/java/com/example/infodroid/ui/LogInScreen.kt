package com.example.infodroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.infodroid.DroidViewModel
import com.example.infodroid.network.dto.LoginRegisterErrorDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LogInScreen(navController: NavHostController, viewModel: DroidViewModel = viewModel()) {
    val (usernameText, onSetUsername) = remember { mutableStateOf("") }
    val (passwordText, onSetPassword) = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val onLogout = {viewModel.logout()}
    var showError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    var showLoginLoadingIndicator by remember { mutableStateOf(false) }
    var showRegisterLoadingIndicator by remember { mutableStateOf(false) }
    var isUsernameFieldError by remember { mutableStateOf(false) }
    var isPasswordFieldError by remember { mutableStateOf(false) }
    var usernameFieldError by remember { mutableStateOf<String?>(null) }
    var passwordFieldError by remember { mutableStateOf<String?>(null) }
    var otherError by remember { mutableStateOf<String?>(null) }

    val onError: (LoginRegisterErrorDto?) -> Unit = {
        it?.also{
            if (it.non_field_errors != null) {
                otherError = it.non_field_errors[0]
                showError = true
            }
            if (it.username_error != null) {
                usernameFieldError = it.username_error[0]
                isUsernameFieldError = true
            }
            if (it.password_error != null) {
                passwordFieldError = it.password_error[0]
                isPasswordFieldError = true
            }
        }
        showLoginLoadingIndicator = false
        showRegisterLoadingIndicator = false
    }
    val onSuccess: () -> Unit = {
        coroutineScope.launch (Dispatchers.Main){
            navController.navigate("posts") { popUpTo("login") { inclusive = true } }
        }
        showLoginLoadingIndicator = false
        showRegisterLoadingIndicator = false
    }
    val onLogin = {
        viewModel.login(usernameText, passwordText, onSuccess, onError)
        showLoginLoadingIndicator = true
    }
    val onRegister = {
        viewModel.register(usernameText, passwordText, onSuccess, onError)
        showRegisterLoadingIndicator = true
    }

    val clearErrors = {
        isUsernameFieldError = false
        isPasswordFieldError = false
        usernameFieldError = null
        passwordFieldError = null
        otherError = null
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column (
            Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Droid Login",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Enter your username and password."
            )
            Spacer(Modifier.height(18.dp))
            CustomTextField(
                value = usernameText,
                onValueChange = {
                    onSetUsername(it)
                    clearErrors()
                },
                label = {
                    Text("Username")
                },
                isError = isUsernameFieldError,
                error = usernameFieldError?:"",
                maxLines = 1
            )
            Spacer(Modifier.height(12.dp))

            CustomTextField(
                value = passwordText,
                onValueChange = {
                    onSetPassword(it)
                    clearErrors()
                },
                label = {
                    Text("Password")
                },
                isError = isPasswordFieldError,
                error = passwordFieldError?:"",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                maxLines = 1
            )
            AnimatedVisibility(visible = !viewModel.loggedIn){
                Column {
                    Spacer(Modifier.height(12.dp))
                    Row {
                        OutlinedButton(onClick = onRegister) {
                            Crossfade(targetState = showRegisterLoadingIndicator) {
                                if (it)
                                    Box (contentAlignment = Alignment.Center){
                                        Text("Register", color = Color.Transparent)
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    }
                                else
                                    Text("Register")
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        FilledTonalButton(
                            onClick = onLogin,
                        ) {
                            Crossfade(targetState = showLoginLoadingIndicator) {
                                if (it)
                                    Box (contentAlignment = Alignment.Center){
                                        Text("Login", color = Color.Transparent)
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    }
                                else
                                    Text("Login", color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = viewModel.loggedIn) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = onLogout
                    ) {
                        Text("Logout")
                    }
                }
            }
        }
    }
    if (showError) {
        AlertDialog(
            onDismissRequest = {showError = false},
            title = { Text("Login Error") },
            text = { Text(otherError?:"") },
            confirmButton = {
                TextButton(
                    onClick = {showError = false}
                ) {
                    Text("Okay")
                }
            }
        )
    }
}