package com.example.infodroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.infodroid.ui.AppUI
import com.example.infodroid.ui.theme.InfoDroidTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.function.Consumer


class MainActivity : ComponentActivity() {

    private lateinit var imageResultLauncher: ActivityResultLauncher<Intent>
    private var currentImageCallback: ((Uri) -> Unit)? = null

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmStoreToken: (String?) -> Unit = {
            getSharedPreferences("MY_APP", MODE_PRIVATE).edit().putString("TOKEN", it).apply()
        }

        imageResultLauncher = setupImageChooser()

        setContent {
            val viewModel by viewModels<DroidViewModel>()

            val getLocation: ((Double, Double) -> Unit) -> Unit = { callback ->

                val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                if (viewModel.lastLocationQuery != null)
                    if (LocalDateTime.now().isAfter(viewModel.lastLocationQuery!!.plusMinutes(15)))
                        viewModel.isLocationFresh = false

                if (viewModel.isLocationFresh) {
                    if (gpsEnabled){
                        val location = locationManager.getBestProvider(Criteria(), false)?.let {
                            Log.i("GetLocation", "Getting last known location, because it's fresh.")
                            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        }
                        if (location == null)
                        {
                            Log.i("GetLocation", "Getting current location because there is no last known location, despite location being fresh.")
                            locationManager.getCurrentLocation(
                                LocationManager.GPS_PROVIDER,
                                null,
                                application.mainExecutor
                            ) {
                                callback(it.longitude, it.latitude)
                            }
                        } else
                            callback(location.longitude, location.latitude)
                    } else
                        Toast.makeText(this, "Please enable location! :)", Toast.LENGTH_LONG).show()
                } else {
                    Log.i("GetLocation", "Getting current location because last known location isn't fresh.")
                    locationManager.getCurrentLocation(
                        LocationManager.GPS_PROVIDER,
                        null,
                        application.mainExecutor
                    ) {
                        callback(it.longitude, it.latitude)
                    }
                    viewModel.isLocationFresh = true
                    viewModel.lastLocationQuery = LocalDateTime.now()
                }

            }

            viewModel.token = getSharedPreferences("MY_APP", MODE_PRIVATE).getString("TOKEN", null)
            viewModel.storeToken = vmStoreToken
            viewModel.getLocation = getLocation
            val navController = rememberNavController()
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current
            viewModel.testAuth {
                if (!it) {
                    coroutineScope.launch(Dispatchers.Main) {
                        withContext(Dispatchers.Main) {
                            navController.navigate("login") {
                                popUpTo("posts") { inclusive = true }
                            }
                        }
                    }
                }
            }

            LaunchedEffect(key1 = true) {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    val permissions = arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    requestPermissions(permissions, 0)
                }
            }
//
            InfoDroidTheme (dynamicColor = true) {
                // A surface container using the 'background' color from the theme
                AppUI(getLocation, this::onChooseImage, navController, viewModel)
            }
        }
    }

    private fun setupImageChooser() =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Uri = result.data?.data ?: return@registerForActivityResult
                val destFile = File(
                    applicationContext.filesDir.absolutePath,
                    data.lastPathSegment!! + data.scheme
                )
                    .also { it.createNewFile() }
                val outStream = FileOutputStream(destFile)
                val inStream = contentResolver.openInputStream(data)?.also {
                    it.copyTo(outStream)
                }
                inStream?.close()
                outStream.close()

                currentImageCallback?.invoke(Uri.fromFile(destFile))
                currentImageCallback = null
            }
        }

    private fun onChooseImage(callback: (Uri) -> Unit) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        currentImageCallback = callback
        imageResultLauncher.launch(intent)
    }

}