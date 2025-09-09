import androidx.compose.foundation.layout.padding // Import padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier // Import Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mine.ui.screens.ChatScreen
import com.example.mine.ui.screens.CommunicationProofScreen
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Proof : Screen("proof")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Scaffold { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(paddingValues) // Apply padding here
        ) {
            composable(Screen.Chat.route) {
                ChatScreen(
                    onBack = { /* maybe exit app? */ },
                    onShowCommunicationProof = { navController.navigate(Screen.Proof.route) }
                )
            }

            composable(Screen.Proof.route) {
                CommunicationProofScreen(
                    proofs = emptyList(), // later pass ViewModel proofs
                    dateFormat = dateFormat
                )
            }
        }
    }
}
