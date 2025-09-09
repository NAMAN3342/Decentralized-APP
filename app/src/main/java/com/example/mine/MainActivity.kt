package com.example.mine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mine.ui.screens.ModernMainScreen
import com.example.mine.ui.screens.CommunicationProofScreen
import com.example.mine.ui.theme.MineTheme
import com.example.mine.viewmodel.SecureChatViewModel
import com.example.mine.callback.ConnectionCallback
import com.example.mine.data.CommunicationProof
import com.example.mine.data.ProofType

class MainActivity : ComponentActivity(), ConnectionCallback {

    private lateinit var secureChatViewModelInstance: SecureChatViewModel

    // Permission request launcher using the modern ActivityResult API
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewModel using applicationContext to avoid potential memory leaks with 'this'
        secureChatViewModelInstance = SecureChatViewModel(this)
        
        // Register this activity as the connection callback
        secureChatViewModelInstance.setConnectionCallback(this)
        
        // Reset app state to ensure clean start, using the initialized ViewModel
        secureChatViewModelInstance.resetAppState()
        
        // Check and request permissions if needed
        checkAndRequestPermissions()
        
        // Note: State observation is now handled in the ViewModel's init block
        // to avoid coroutine scope issues in the Activity
        
        // Handle back button press
        enableEdgeToEdge()
        setContent {
            MineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            ModernMainScreen(
                                viewModel = secureChatViewModelInstance,
                                onNavigateToProofs = {
                                    navController.navigate("proofs")
                                }
                            )
                        }

                        composable("proofs") {
                            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                            val sampleProofs = listOf(
                                CommunicationProof(
                                    proofType = ProofType.WIFI,
                                    deviceName = "Pixel 7",
                                    deviceId = 12345,
                                    connectionType = "wifi",
                                    latency = 42,
                                    timestamp = java.util.Date(),
                                    success = true,
                                    errorMessage = null
                                ),
                                CommunicationProof(
                                    proofType = ProofType.BLUETOOTH,
                                    deviceName = "Galaxy S21",
                                    deviceId = 67890,
                                    connectionType = "bluetooth",
                                    latency = 58,
                                    timestamp = java.util.Date(),
                                    success = false,
                                    errorMessage = "Timeout"
                                )
                            )

                            CommunicationProofScreen(
                                proofs = sampleProofs,
                                dateFormat = dateFormat
                            )
                        }
                    }
                }
            }
        }

        // System back behavior is handled inside Compose via BackHandler in ModernMainScreen
    }
    
    override fun onResume() {
        super.onResume()
        
        Log.d("MainActivity", "onResume() called")
        
        // Only check for connections if we're in discovery mode
        // This prevents the app from jumping to connection screen on startup
        Log.d("MainActivity", "Checking if in discovery mode before calling checkConnectionStatus()")
        secureChatViewModelInstance.checkConnectionStatus() // Use the initialized ViewModel
        Log.d("MainActivity", "checkConnectionStatus() call completed")
    }
    
    // Implementation of ConnectionCallback
    override fun onConnectionDetected(nodeName: String) {
        try {
            Log.d("MainActivity", "=== Connection detected: $nodeName ===")
            
            // Bring app to foreground
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            startActivity(intent)
            
            // Set window flags to ensure app is visible
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }

            Log.d("MainActivity", "App brought to foreground successfully")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to bring app to foreground", e)
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>()
        
        // Camera permission for QR code scanning
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.CAMERA)
        }
        
        // Location permission for Bluetooth discovery
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        // Bluetooth permissions for Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            // Legacy Bluetooth permissions for Android 11 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
        
        if (requiredPermissions.isNotEmpty()) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }
    
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val grantedPermissions = mutableListOf<String>()
        val deniedPermissions = mutableListOf<String>()
        
        permissions.forEach { (permission, isGranted) ->
            if (isGranted) {
                grantedPermissions.add(permission)
            } else {
                deniedPermissions.add(permission)
            }
        }
        
        if (grantedPermissions.isNotEmpty()) {
            Log.d("MainActivity", "Granted permissions: $grantedPermissions")
        }
        
        if (deniedPermissions.isNotEmpty()) {
            Log.w("MainActivity", "Denied permissions: $deniedPermissions")
            
            // Check if any critical permissions were denied
            val criticalPermissions = deniedPermissions.filter { permission ->
                permission == Manifest.permission.CAMERA
            }
            
            if (criticalPermissions.isNotEmpty()) {
                Log.e("MainActivity", "Critical permissions denied: $criticalPermissions")
                // You might want to show a dialog explaining why these permissions are needed
                // and guide the user to settings
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MineTheme {
        // Note: This is just a preview, so we'll use a mock ViewModel
        // In the actual app, the ViewModel is created with context in MainActivity
        ModernMainScreen(
            viewModel = SecureChatViewModel(LocalContext.current),
            onNavigateToProofs = { /* For preview we can leave this empty */ }
        )
    }
}
