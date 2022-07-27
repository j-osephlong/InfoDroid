package com.example.infodroid.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.example.infodroid.DroidViewModel
import com.example.infodroid.network.dto.NewPostErrorDto
import com.example.infodroid.network.dto.Post
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.*
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun NewPostScreen(
    getLocation: ( (Double, Double) -> Unit ) -> Any,
    onChooseImage: ( (Uri) -> Unit ) -> Unit,
    navController: NavHostController,
    viewModel: DroidViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val dateDialogState = rememberMaterialDialogState()
    val timeDialogState = rememberMaterialDialogState()

    val rangeMax = 10000
    val rangeRange = 0.001f..1f //0.001*10,000 = 10m, our min

    var contentError by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var refURLError by remember { mutableStateOf<String?>(null) }
    var otherErrors by remember { mutableStateOf<String?>(null) }

    var showLoadingIndicator by remember { mutableStateOf(false) }

    val onSuccess: () -> Unit = {
        coroutineScope.launch (Dispatchers.Main){
            navController.navigate("posts") { popUpTo("login") { inclusive = true } }
        }
        showLoadingIndicator = false
    }
    val onError: (NewPostErrorDto?) -> Unit = {
        it?.also{
            if (it.content != null)
                contentError = it.content[0]
            if (it.title != null)
                titleError = it.title[0]
            if (it.refURL != null)
                refURLError = it.refURL[0]
            if (it.non_field_errors != null)
                otherErrors = it.non_field_errors[0]
        }
        showLoadingIndicator = false
    }

    val imgPainter: Painter? = if (viewModel.imgUri == null)
        null
    else
        rememberImagePainter(
            data = viewModel.imgUri,
            builder = {
                crossfade(true)
            }
        )
    val onSetImage: (Uri) -> Unit= {
        viewModel.imgUri = it
    }
    val onPost: () -> Unit = {
        if (viewModel.contentText.split("\\s+".toRegex()).size > 420) {
            contentError = "Post body must be 420 or fewer words long."
        } else {
            showLoadingIndicator = true

            val imgBase64: String? = viewModel.imgUri?.let {
                val imageStream: InputStream? = context.contentResolver.openInputStream(it)
                val selectedImage = BitmapFactory.decodeStream(imageStream)
                encodeImage(selectedImage)
            }

            if (viewModel.currentEditPost == null)
                getLocation { lon, lat ->
                    viewModel.submitPost(
                        Post(
                            title = viewModel.titleText,
                            content = viewModel.contentText,
                            refURL = viewModel.urlText,
                            rangeMeters = (viewModel.privacyRange * rangeMax).toInt(),
                            extendToLocality = viewModel.extendToLocality,
                            coordLongitude = lon.toFloat(),
                            coordLatitude = lat.toFloat(),
                            requestImageBase64 = imgBase64,
                            endTime = if (viewModel.selectedTime != null) LocalDateTime.of(viewModel.selectedDate, viewModel.selectedTime).atOffset(
                                ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) else null
                        ),
                        onSuccess,
                        onError
                    )
                }
            else
                viewModel.editPost(
                    viewModel.currentEditPost?.id!!,
                    Post(
                        title = viewModel.titleText,
                        content = viewModel.contentText,
                        refURL = viewModel.urlText,
                        rangeMeters = (viewModel.privacyRange * rangeMax).toInt(),
                        extendToLocality = viewModel.extendToLocality,
                        requestImageBase64 = imgBase64,
                        endTime = if (viewModel.selectedTime != null) LocalDateTime.of(viewModel.selectedDate, viewModel.selectedTime).atOffset(
                            ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) else null
                    ),
                    onSuccess,
                    onError
                )
        }
    }
    val clearErrors = {
        contentError = null
        titleError = null
        refURLError = null
        otherErrors = null
    }
    LaunchedEffect(viewModel.currentEditPost) {
        if (viewModel.currentEditPost != null) {
            viewModel.titleText = viewModel.currentEditPost?.title!!
            viewModel.contentText = viewModel.currentEditPost?.content!!
            viewModel.urlText = viewModel.currentEditPost?.refURL!!
            viewModel.privacyRange = (viewModel.maxRangeMeters/viewModel.currentEditPost?.rangeMeters!!.toFloat())
            viewModel.extendToLocality = viewModel.currentEditPost?.extendToLocality!!

            if (viewModel.currentEditPost?.endTime != null) {
                val formatter = ZonedDateTime.parse(
                    viewModel.currentEditPost?.endTime!!,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                ).toInstant().atZone(ZoneId.systemDefault())
                viewModel.selectedDate = formatter.toLocalDate()
                viewModel.selectedTime = formatter.toLocalTime()
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = {
                        Text("New Post")
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Rounded.ArrowBack, null)
                        }
                    }
                )
            }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp, 0.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                CustomTextField(
                    value = viewModel.titleText,
                    onValueChange = {
                        viewModel.titleText = it
                        clearErrors()
                    },
                    maxLines = 3,
                    label = {
                        Text("Post Title")
                    },
                    error = titleError ?: "",
                    isError = titleError != null
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedCard (
                    Modifier
                        .fillMaxWidth()
                        .clickable { onChooseImage(onSetImage) }, shape = RoundedCornerShape(8.dp)){
                    if (viewModel.imgUri == null){
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.Image, null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Add Image")
                        }
                    } else
                        imgPainter?.also {
                            Image(
                                painter = it,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .height(260.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                }
                Spacer(modifier = Modifier.height(12.dp))
                CustomOutlinedTextField(
                    value = viewModel.urlText,
                    onValueChange = {
                        viewModel.urlText = it
                        clearErrors()
                    },
                    label = {
                        Text("Article URL (Optional)")
                    },
                    maxLines = 1,
                    error = refURLError ?: "",
                    isError = refURLError != null
                )
                Spacer(modifier = Modifier.height(12.dp))
                CustomOutlinedTextField(
                    value = viewModel.contentText,
                    onValueChange = {
                        viewModel.contentText = it
                        clearErrors()
                    },
                    label = {
                        Text("Post Content${if (viewModel.contentText.isNotBlank()) " ${viewModel.contentText.split("\\s+".toRegex()).size}/420" else ""}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(.5f),
                    error = contentError ?: "",
                    isError = contentError != null
                )
                if (viewModel.selectedTime != null) {
                    val endTime = LocalDateTime.of(viewModel.selectedDate, viewModel.selectedTime).atZone(ZoneId.systemDefault())

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        Modifier
                            .fillMaxWidth(),
                        color = if (endTime.isBefore(ZonedDateTime.now().minusHours(4)))
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
                    "${(viewModel.privacyRange * rangeMax).toInt()}m",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Slider(
                    value = viewModel.privacyRange,
                    onValueChange = { viewModel.privacyRange = it },
                    valueRange = rangeRange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Allow users to find post by community? (Less private)",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = viewModel.extendToLocality,
                        onCheckedChange = { viewModel.extendToLocality = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.background,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        if (viewModel.selectedTime == null)
                            dateDialogState.show()
                        else {
                            viewModel.selectedDate = null
                            viewModel.selectedTime = null
                        }
                    }) {
                        Text(if (viewModel.selectedTime == null) "Add Expiration Date" else "Remove Expiration Date")
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onPost) {
                        Crossfade(targetState = showLoadingIndicator) {
                            if (it)
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Submit", color = Color.Transparent)
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            else
                                Text("Submit")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (otherErrors!=null) {
        AlertDialog(
            onDismissRequest = { otherErrors = null },
            title = { Text("Login Error") },
            text = { Text(otherErrors ?: "") },
            confirmButton = {
                TextButton(
                    onClick = { otherErrors = null }
                ) {
                    Text("Okay")
                }
            }
        )
    }

    MaterialDialog(
        dialogState = dateDialogState,
        buttons = {
            positiveButton("Ok")
            negativeButton("Cancel")
        }
    ) {
        datepicker {
            viewModel.selectedDate = it
            timeDialogState.show()
        }
    }
    MaterialDialog(
        dialogState = timeDialogState,
        buttons = {
            positiveButton("Ok")
            negativeButton("Cancel") {
                viewModel.selectedDate = null
            }
        },
        onCloseRequest = {
            viewModel.selectedDate = null
        }
    ) {
        timepicker {
            viewModel.selectedTime = it
        }
    }
}

private fun encodeImage(bm: Bitmap): String? {
    val baos = ByteArrayOutputStream()
    bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    val b: ByteArray = baos.toByteArray()
    return Base64.encodeToString(b, Base64.DEFAULT)
}