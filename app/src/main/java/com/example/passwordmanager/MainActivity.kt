@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)

package com.example.passwordmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material.IconButton as IconButton
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment.Companion.Center
import kotlinx.coroutines.withContext


data class PasswordItem(
    val accountName: String,
    val username: String,
    val password: String
)

@Composable
fun Home(passwordDb: PasswordDb) {
    val scope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden
    )
    val detailsBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden
    )
    var accountName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var selectedItem by remember { mutableStateOf<PasswordItem?>(null) }

    var passwordVisible by remember { mutableStateOf(false) }
    var snackbarVisible by remember { mutableStateOf(false) }
    val passwordStrength = calculatePasswordStrength(password)
    var passwordList by remember { mutableStateOf(listOf<PasswordItem>()) }

    LaunchedEffect(Unit) {
        passwordList = passwordDb.getPasswords()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Manager") },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 0.dp
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch { modalBottomSheetState.show() }
            }) {
                Icon(Icons.Default.Add,
                    tint = Color.White,
                    contentDescription = "Add Account")
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() },
                modifier = Modifier.padding(16.dp)
            ) {
                Snackbar(
                    action = {
                        TextButton(
                            onClick = { snackbarVisible = false },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text("Dismiss")
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("One or more fields are empty!")
                }
            }
        },
        content = {
            LazyColumn(modifier = Modifier.padding(it)) {
                items(passwordList) { item ->
                    PasswordListItem(item, onIconClick = {
                        selectedItem = item
                        scope.launch { detailsBottomSheetState.show() }
                    })
                }
            }
        }
    )

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetShape = RoundedCornerShape(topStartPercent = 15, topEndPercent = 15),
        sheetContent = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(16.dp),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Divider(
                    color = Color.Gray,
                    thickness = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.2f)
                        .height(4.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))
                TextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Account Name") },
                    isError = accountName.isEmpty()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / Email") },
                    isError = username.isEmpty()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else
                            Icons.Filled.VisibilityOff

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    isError = password.isEmpty()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Strength: $passwordStrength")
                    TextButton(onClick = { password = generateRandomPassword() }) {
                        Text("Generate Password")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (accountName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                            snackbarVisible = true
                        } else {
                            if (selectedItem != null){

                                // Update the selected item in the list
                                val updatedList = passwordList.map { if (it == selectedItem) PasswordItem(accountName, username, password) else it }
                                passwordList = updatedList

                                // Update the password in the database
                                passwordDb.updatePassword(PasswordItem(accountName, username, password))

                                // Dismiss the bottom sheet
                                scope.launch { modalBottomSheetState.hide() }
                            }


                            else { // Insert the password into the database
                                val newItem = PasswordItem(accountName, username, password)


                                passwordList = passwordList + newItem

                                passwordDb.insertPassword(newItem)
                                scope.launch { modalBottomSheetState.hide() }
                            }

                            accountName = ""
                            username = ""
                            password = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(if (selectedItem != null)"Update" else "Add New Account")
                }
            }
        },
        content = {}
    )

    selectedItem?.let { item ->
        ModalBottomSheetLayout(
            sheetState = detailsBottomSheetState,
            sheetShape = RoundedCornerShape(topStartPercent = 15, topEndPercent = 15),
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(horizontal = 35.dp, vertical = 16.dp),

                    horizontalAlignment = Alignment.Start
                ) {
                    Divider(
                        color = Color.Gray,
                        thickness = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.2f)
                            .height(4.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Account Details", style = MaterialTheme.typography.h6)

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Account Name: \n ${item.accountName}", style = MaterialTheme.typography.body1)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Username:\n ${item.username}", style = MaterialTheme.typography.body1)
                    Spacer(modifier = Modifier.height(8.dp))
                    var passwordVisible by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Password:\n ${if (passwordVisible) item.password else "*".repeat(item.password.length)}")
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            Icon(imageVector = icon, contentDescription = null)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center)
                    {
                        Button(onClick = {
                            accountName = item.accountName
                            username = item.username
                            password = item.password
                            // Show the first bottom sheet for editing
                            scope.launch { modalBottomSheetState.show() }
                            // Dismiss the second bottom sheet
                            scope.launch { detailsBottomSheetState.hide() }
                            // Implement action for the Edit button
                            // For example: navigate to edit password screen
                        },
                            modifier = Modifier.width(125.dp)

                        ) {
                            Text("Edit")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            passwordList = passwordList.filter { it != item }
                            // Delete the item from the database
                            passwordDb.deletePassword(item)
                            // Dismiss the bottom sheet
                            scope.launch { detailsBottomSheetState.hide() }
                        },
                            modifier = Modifier.width(125.dp)
                        ) {
                            Text("Delete")
                        }
                    }

                }

            },
            content = {}
        )
    }
}

@Composable
fun PasswordListItem(item: PasswordItem, onIconClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(percent = 50),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.accountName,
                modifier = Modifier.padding(start = 8.dp),
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "*".repeat(item.password.length),
                style = TextStyle(color = Color.Gray)
            )
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.clickable { onIconClick() }
            )
        }
    }
}

@Composable
fun calculatePasswordStrength(password: String): String {
    return when {
        password.length > 10 -> "Strong"
        password.length > 6 -> "Medium"
        password.isNotEmpty() -> "Weak"
        else -> "Very Weak"
    }
}

fun generateRandomPassword(): String {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#\$%^&*()_+"
    return (1..12)
        .map { chars.random() }
        .joinToString("")
}

class MainActivity : ComponentActivity() {
    private lateinit var passwordDb: PasswordDb
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        passwordDb = PasswordDb(this)
        setContent {
            Home(passwordDb = passwordDb)
        }
    }
}