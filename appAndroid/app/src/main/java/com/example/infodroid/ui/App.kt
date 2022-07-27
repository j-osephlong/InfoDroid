package com.example.infodroid.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.example.infodroid.DroidViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUI(
    getLocation: ( (Double, Double) -> Unit ) -> Any,
    onChooseImage: ( (Uri) -> Unit ) -> Unit,
    navController: NavHostController,
    viewModel: DroidViewModel = viewModel()
) {
    NavigationRoot(getLocation, onChooseImage, navController, viewModel)
}

@Composable 
fun NavigationRoot(
    getLocation: ( (Double, Double) -> Unit ) -> Any,
    onChooseImage: ( (Uri) -> Unit ) -> Unit,
    navController: NavHostController,
    viewModel: DroidViewModel = viewModel()
) {
    NavHost(navController = navController, startDestination = "posts") {
        composable("posts") {
            if (viewModel.loggedIn)
                PostListScreen(navController, viewModel)
            else
                LoadingPlaceholderScreen()
        }
        composable("login") {
            LogInScreen(navController, viewModel)
        }
        composable("newPost") {
            NewPostScreen(getLocation, onChooseImage, navController, viewModel)
        }
        composable("post/{postId}", arguments = listOf(navArgument("postId") { type = NavType.IntType })) {
            PostScreen(viewModel = viewModel, navController = navController, postId = it.arguments?.getInt("postId")?:0)
        }
        composable("image/{imgurHash}", arguments = listOf(navArgument("imgurHash") { type = NavType.StringType })) {
            ImageScreen(imgurHash = it.arguments?.getString("imgurHash")?:"", navController)
        }
        composable("self") {
            UserPostListScreen(navController = navController, viewModel = viewModel)
        }
        composable(
            "postMap/{latitude},{longitude}",
            arguments = listOf(
                navArgument("latitude") { type = NavType.FloatType },
                navArgument("longitude") { type = NavType.FloatType })
        ) {
            MapScreen(latitude = it.arguments?.getFloat("latitude")?:0f, longitude = it.arguments?.getFloat("longitude")?:0f, navController = navController)
        }
    }
}

@Composable
fun LoadingPlaceholderScreen() {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    latitude: Float,
    longitude: Float,
    navController: NavHostController
) {
    val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }
    val uiSettings by remember { mutableStateOf(MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = true, compassEnabled = true)) }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        SmallTopAppBar(title = { Text("Post Location") }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) {
            Icon(Icons.Rounded.Close, null)
        }
        })
    }) {
        Surface (Modifier.fillMaxSize()) {
            Column (Modifier.fillMaxSize()){
                val postLocationMarker = LatLng(
                    latitude.toDouble(),
                    longitude.toDouble()
                )
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(postLocationMarker, 14f)
                }
                GoogleMap(
                    modifier = Modifier
                        .fillMaxSize(),
                    uiSettings = uiSettings,
                    properties = mapProperties,
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        position = postLocationMarker,
                        title = "Post",
                        snippet = "Post was made here."
                    )
                }
            }
        }
    }
}
