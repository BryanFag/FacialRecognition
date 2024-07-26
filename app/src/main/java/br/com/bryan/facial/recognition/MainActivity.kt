package br.com.bryan.facial.recognition

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.*
import br.com.bryan.facial.recognition.ui.theme.FacialRecognitionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions() // Chama a função para verificar permissões
        setContent {
            FacialRecognitionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1)
        } else {
            Log.d(TAG, "All permissions granted")
        }
    }
}

@Composable
fun App() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("cam") { CamActivity() }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    var valorCPF by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .background(color = Color(0xFF3D3D3D))
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val image: Painter = painterResource(id = R.drawable.logo_app)
        Spacer(modifier = Modifier.height(50.dp))
        Image(
            painter = image,
            contentDescription = "Logo",
            modifier = Modifier
                .width(300.dp)  // Define a largura da imagem
                .height(100.dp)
        )

        Text(
            text = "Facial Recognition",
            style = TextStyle(
                color = Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )

        TextField(
            value = valorCPF,
            onValueChange = {
                if (it.length <= 11) {
                    valorCPF = it
                }
            },
            label = { Text("CPF") },
            shape = RoundedCornerShape(20.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Icon",
                    tint = Color.Black
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier.width(300.dp)
        )

        Text(
            text = login,
            style = TextStyle(
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Button(
            onClick = {
                if (valorCPF == "12345678909") {
                    login = "Login efetuado com sucesso!"
                    navController.navigate("cam") // navega para br.com.bryan.facial.recognition.CamActivity
                } else {
                    login = "CPF inválido"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.size(width = 200.dp, height = 60.dp)
        ) {
            Text(
                text = "LOGIN"
            )
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    FacialRecognitionTheme {
        App()
    }
}