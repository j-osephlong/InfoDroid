package com.example.infodroid.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavHostController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageScreen(
    imgurHash: String,
    navController: NavHostController
) {
    val url = "https://imgur.com/$imgurHash.png"

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        SmallTopAppBar(title = { Text("Image") }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) {
            Icon(Icons.Rounded.Close, null)
        }
        })
    }) {
        Surface (Modifier.fillMaxSize()) {
            Column (Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally){
                Image(
                    painter = rememberImagePainter(url),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}
