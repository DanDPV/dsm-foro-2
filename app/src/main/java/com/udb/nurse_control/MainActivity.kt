package com.udb.nurse_control

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.udb.nurse_control.ui.theme.DSMForo2Theme

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("595030947142-rjatc4vfl333thmkkcil7rs5eqjeia2i.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            DSMForo2Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    MyApp(
                        auth = auth,
                        googleSignInClient = googleSignInClient,
                        onGoogleSignInClick = { signInWithGoogle() })
                    // Show Error Dialog if errorMessage is not null
                    errorMessage?.let {
                        AlertDialog(
                            onDismissRequest = { errorMessage = null },
                            title = { Text("Error") },
                            text = { Text(it) },
                            confirmButton = {
                                Button(onClick = { errorMessage = null }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Google Sign-In result launcher
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task)
        }

    // Start Google Sign-In
    fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun showErrorDialog(message: String) {
        errorMessage = message
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account,
                onSuccess = {
                    // Navigate to HomeScreen or update UI to show HomeScreen
                    setContent {
                        DSMForo2Theme {
                            // A surface container using the 'background' color from the theme
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                MyApp(
                                    auth = auth,
                                    googleSignInClient = googleSignInClient,
                                    onGoogleSignInClick = { signInWithGoogle() })
                            }
                        }
                    }
                },
                onError = { errorMessage ->
                    // Show an error dialog with the provided error message
                    showErrorDialog(errorMessage)
                }
            )
        } catch (e: ApiException) {
            if (e.statusCode == 12501) {
                showErrorDialog("Hubo un error al iniciar sesión con google.")
            }

            Log.d("TEST", "Google sign-in failed: ${e.statusCode}")
        }
    }

    private fun firebaseAuthWithGoogle(
        account: GoogleSignInAccount?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign-in successful, proceed to HomeScreen
                    onSuccess()
                } else {
                    // Handle failed sign-in
                    val errorMessage =
                        task.exception?.localizedMessage ?: "Hubo un error en el inicio de sesión con google"
                    onError(errorMessage)
                }
            }
    }
}

@Composable
fun MyApp(
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    onGoogleSignInClick: () -> Unit
) {
    var isLoggedIn by remember { mutableStateOf(false) }

    // Observe authentication state in a LaunchedEffect and DisposableEffect
    LaunchedEffect(auth) {
        val currentUser = auth.currentUser
        isLoggedIn = currentUser != null
    }

    DisposableEffect(auth) {
        // Add an auth state listener to update isLoggedIn on changes
        val authStateListener = FirebaseAuth.AuthStateListener {
            isLoggedIn = it.currentUser != null
            if (it.currentUser != null) {
                Log.d("TEST", "Email: ${it.currentUser?.email}")
            }
        }
        auth.addAuthStateListener(authStateListener)

        // Remove the listener when the composable is removed from composition
        onDispose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    // Show the correct screen based on authentication state
    if (isLoggedIn) {
        HomeScreen(auth = auth, onSignOut = {

            FirebaseAuth.getInstance().signOut()
            googleSignInClient.signOut() // Ensure Google client signs out
            isLoggedIn = false
        })
    } else {
        Login(
            // Callback for successful login (update state if necessary)
            onLoginSuccess = { isLoggedIn = true },
            onGoogleSignInClick = onGoogleSignInClick
        )
    }
}

@Composable
fun HomeScreen(auth: FirebaseAuth, onSignOut: () -> Unit) {
    val currentUser = auth.currentUser
    // Home screen UI for logged-in users
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            "Bienvenido",
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "${currentUser?.email}", fontSize = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onSignOut()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp),
        ) {
            Text("Cerrar Sesión", modifier = Modifier.padding(vertical = 5.dp))
        }
    }
}


@Composable
fun Login(onLoginSuccess: () -> Unit, onGoogleSignInClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    val openDialog = remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Hubo un error al iniciar sesión") }

    fun validarEmail() {
        emailError = !isValidEmail(email)
    }

    fun validarPass() {
        passwordError = password.isEmpty()
    }

    Column(
        modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_title_black),
            contentDescription = "Nurse control logo",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Text(
            text = "Login",
            textAlign = TextAlign.Center,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                validarEmail()
            },
            label = { Text("Correo") },
            isError = emailError,
            supportingText = {
                if (emailError) {
                    Text("Escriba un correo válido")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                validarPass()
            },
            label = { Text("Contraseña") },
            isError = passwordError,
            supportingText = {
                if (passwordError) {
                    Text("Contraseña es requerida")
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) R.drawable.visibility
                else R.drawable.visibility_off

                // Localized description for accessibility services
                val description = if (passwordVisible) "Hide password" else "Show password"

                // Toggle button to hide or display password
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Image(
                        painter = painterResource(id = image),
                        contentDescription = description,
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp)
        )

        Button(
            onClick = {
                validarEmail()
                validarPass()

                if (!emailError && !passwordError) {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                errorMessage =
                                    when (val exception = task.exception) {
                                        is FirebaseAuthInvalidCredentialsException -> "Credenciales incorrectar o vencidas."
                                        else -> exception?.message
                                            ?: "Hubo un error la iniciar sesión."
                                    }
                                openDialog.value = true
                            }
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp),
        ) {
            Text("Iniciar Sesión", modifier = Modifier.padding(vertical = 5.dp))
        }

        OutlinedButton(
            onClick = onGoogleSignInClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_g_logo),
                contentDescription = "Google logo",
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text("Iniciar con Google", modifier = Modifier.padding(vertical = 5.dp))
        }

        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = {
                    // Dismiss the dialog when the user clicks outside the dialog or on the back
                    // button. If you want to disable that functionality, simply use an empty
                    // onDismissRequest.
                    openDialog.value = false
                },
                title = { Text(text = "Error") },
                text = { Text(text = errorMessage) },
                confirmButton = {
                    TextButton(onClick = { openDialog.value = false }) { Text("Ok") }
                },
                dismissButton = {
                    TextButton(onClick = { openDialog.value = false }) { Text("Cerrar") }
                }
            )
        }
    }
}