package com.example.infodroid.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavHostController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.example.infodroid.DroidViewModel
import com.example.infodroid.network.dto.GenericErrorDto
import com.example.infodroid.network.dto.Post
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun PostScreen(
    viewModel: DroidViewModel,
    navController: NavHostController,
    postId: Int
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }
    val uiSettings by remember { mutableStateOf(MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false, compassEnabled = false)) }

    var post by remember { mutableStateOf<Post?>(null) }
    var errors by remember { mutableStateOf<GenericErrorDto?>(null) }
    viewModel.getPost(
        postId,
        {
            post = it
        },
        {
            errors = it
        }
    )

    val deleteOnSuccess: () -> Unit = {
        coroutineScope.launch(Dispatchers.Main) { Toast.makeText(context, "Deleted post.", Toast.LENGTH_LONG).show() }
        coroutineScope.launch (Dispatchers.Main){ navController.navigateUp() }
    }
    val deleteOnError: (String) -> Unit = {
        coroutineScope.launch(Dispatchers.Main) { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar (
                title = {
                    Text("Post")
                },
                actions = {
                    if (post?.isMyPost == true)
                        IconButton(onClick = { viewModel.currentEditPost = post; navController.navigate("newPost") }) {
                            Icon(Icons.Rounded.Edit, null)
                        }
                    if (post?.isMyPost == true)
                        IconButton(onClick = { viewModel.deletePost(post?.id!!, deleteOnSuccess, deleteOnError) }) {
                            Icon(Icons.Rounded.Delete, null)
                        }
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Rounded.ArrowBack, null)
                    }
                }
            )
        }
    ) {

        Surface(
            Modifier
                .fillMaxSize()) {
            if (post == null)
            {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            } else {
                Column (
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())){
                    AnimatedVisibility(visible = post?.responseImage != null, modifier = Modifier.fillMaxWidth()) {
                        Image(
                            contentDescription = null,
                            painter = rememberImagePainter("https://imgur.com/${post?.responseImage}.png"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clickable { navController.navigate("image/${post?.responseImage}") },
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(12.dp, 0.dp)
                    )
                    {
                        val parsedDateTime =
                            ZonedDateTime.parse(post?.createdOn ?: "")
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm"));


                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            post?.title ?: "No title? Probably an error.",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (post?.refURL != null && post?.refURL?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val browserIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(post!!.refURL)
                                        )
                                        startActivity(context, browserIntent, null)
                                    },
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Link, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${post?.refURL}")
                                }

                            }
                        }
                        if (post?.endTime != null) {
                            val endTime = ZonedDateTime.parse(post?.endTime!!, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                .withZoneSameInstant(ZoneId.systemDefault())

                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                Modifier
                                    .fillMaxWidth(),
                                color = if (endTime.isBefore(ZonedDateTime.now().plusHours(4)))
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Icon(Icons.Rounded.Timer, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Post expires on ${endTime.format(DateTimeFormatter.ofPattern("MMMM d 'at' h:mm a"))}")
                                }

                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            post?.content ?: "No content? Probably an error.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Posted by ${post?.author ?: "No author? Probably an error."} on $parsedDateTime.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        val postLocationMarker = LatLng(
                            post?.coordLatitude?.toDouble() ?: 0.0,
                            post?.coordLongitude?.toDouble() ?: 0.0
                        )
                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(postLocationMarker, 14f)
                        }
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(
                                    RoundedCornerShape(12.dp)
                                ),
                            uiSettings = uiSettings,
                            properties = mapProperties,
                            cameraPositionState = cameraPositionState,
                            onMapClick = {navController.navigate("postMap/${postLocationMarker.latitude},${postLocationMarker.longitude}")}
                        ) {
                            Marker(
                                position = postLocationMarker,
                                title = "Post",
                                snippet = "Post was made here."
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${post?.distance ?: 0} meters away${if (post?.locality == null) "." else " in ${post?.locality?.name}"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "(${post?.coordLatitude ?: 0.0}, ${post?.coordLongitude ?: 0.0})",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "Privacy range - ${post?.rangeMeters} meters.",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (errors != null)
        AlertDialog(
            onDismissRequest = { errors = null },
            confirmButton = {
                Button ({errors = null}) {
                    Text("Ok")
                }
            },
            title = {
                Text("${errors?.code}")
            },
            text = {
                Text("${errors?.data}")
            }
        )
}