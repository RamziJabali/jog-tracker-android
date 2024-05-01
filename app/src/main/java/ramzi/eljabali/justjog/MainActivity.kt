package ramzi.eljabali.justjog

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.core.content.ContextCompat
import com.jaikeerthick.composable_graphs.composables.line.model.LineData
import ramzi.eljabali.justjog.ui.design.JustJogTheme
import ramzi.eljabali.justjog.ui.views.JoggingFAB
import ramzi.eljabali.justjog.ui.views.StatisticsPage

class MainActivity : ComponentActivity() {
    private val permissionList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        listOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        listOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        checkPermissionStatus(permissionList)
        val data = listOf(
            LineData(x = "Mon", y = 40),
            LineData(x = "Tues", y = 60),
            LineData(x = "Wed", y = 70),
            LineData(x = "Thurs", y = 120),
            LineData(x = "Fri", y = 80),
            LineData(x = "Sat", y = 60),
            LineData(x = "Sun", y = 150),
        )
        setContent {
            JustJogTheme(true) {
                Scaffold(
//                bottomBar = bottomBar,
//                snackbarHost = snackbarHost,
                    floatingActionButton = { JoggingFAB() },
                    floatingActionButtonPosition = FabPosition.EndOverlay,
                ) {
                    it
                    StatisticsPage(
                        motivationalQuote = "Awareness is the only density, the only guarantee of affirmation.",
                        data = data
                    )
//                    CalendarPage()
                }
            }
        }
    }
    //Best way to check which permission status you are on
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermissionStatus(permissions: List<String>) {
        for (permission in permissions){
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("Log.d", "$permission Permission Already Granted")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d("Log.d", "Showing Request Rationale for $permission")
                }

                else -> {
                    Log.d("Log.d", "Asking user to grant permission for $permission")
                    askForPermission(permission)
                }
            }
        }
    }

    // You are asking for permission
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun askForPermission(permission: String): Boolean {
        var isGrantedPermission = false
        val permissionRequestResult = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Log.d", "$permission has been granted")
            } else {
                Log.d("Log.d", "$permission has been denied")
            }
            isGrantedPermission = isGranted
        }
        permissionRequestResult.launch(permission)
        return isGrantedPermission
    }

    //Making an open detailed settings request
    private fun Activity.openSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).also(::startActivity)
    }
}
