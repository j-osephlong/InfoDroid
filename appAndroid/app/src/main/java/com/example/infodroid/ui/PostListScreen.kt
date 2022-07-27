package com.example.infodroid.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.example.infodroid.DroidViewModel
import com.example.infodroid.network.dto.LocalityDto
import com.example.infodroid.network.dto.Post
import com.example.infodroid.ui.theme.InfoDroidTheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostListScreen(
    navController: NavHostController,
    viewModel: DroidViewModel = viewModel()
) {
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val scrollBehavior = remember(decayAnimationSpec) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(decayAnimationSpec)
    }
    val postListItems = viewModel.posts.collectAsLazyPagingItems()
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar (
                title = {
                    Text("Local Posts")
                },
                actions = {
                    IconButton(onClick = {
                        postListItems.refresh()
                    }) {
                        Icon(Icons.Rounded.Refresh, null)
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo("posts") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Rounded.Logout, null)
                    }

                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.loggedIn)
                            navController.navigate("self")
                    }) {
                        Icon(Icons.Rounded.AccountCircle, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.clearNewPostFields()
                navController.navigate("newPost")
            }) {
                Icon(Icons.Rounded.Edit, null)
            }
        }
    ) {
        if (viewModel.loggedIn)
            PostList(postListItems, viewModel) {navController.navigate("post/$it")}
        else
            Text("Not logged in yet...")
    }

    if (viewModel.getPostsError != null)
        AlertDialog(
            onDismissRequest = { viewModel.getPostsError = null },
            confirmButton = { Button (onClick = {viewModel.getPostsError = null}){
                Text("Okay")
            } }
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPostListScreen(
    navController: NavHostController,
    viewModel: DroidViewModel = viewModel()
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
    var postListItems by remember { mutableStateOf<List<Post>>(listOf()) }
    viewModel.getUserPosts({
        postListItems = it
    })

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar (
                title = {
                    Text("My Posts")
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo("posts") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Rounded.Logout, null)
                    }

                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Rounded.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("newPost") }) {
                Icon(Icons.Rounded.Edit, null)
            }
        }
    ) {
        if (viewModel.loggedIn)
            LazyColumn(Modifier.padding(12.dp, 0.dp)) {
                item {
                    Spacer(Modifier.height(12.dp))
                }
                items(postListItems) { post ->
                    post?.let {
                        Column {
                            PostComponent(post = it, onClick = { navController.navigate("post/${it.id}") })
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(12.dp))
                }
            }

        else
            Text("Not logged in yet...")
    }

    if (viewModel.getPostsError != null)
        AlertDialog(
            onDismissRequest = { viewModel.getPostsError = null },
            confirmButton = { Button (onClick = {viewModel.getPostsError = null}){
                Text("Okay")
            } }
        )
}

@Composable
fun PostList (
    postListItems: LazyPagingItems<Post>,
    viewModel: DroidViewModel,
    onClickPost: (Int) -> Unit
) {
    LaunchedEffect(key1 = true) {
        postListItems.refresh()
    }

    SwipeRefresh(state = rememberSwipeRefreshState(isRefreshing = postListItems.loadState.refresh is LoadState.Loading), onRefresh = { postListItems.refresh() }, Modifier.fillMaxSize()){
        LazyColumn(
            Modifier
                .fillMaxSize()) {
            item {
                Spacer(Modifier.height(12.dp))
            }
            if (!viewModel.postsWaitingOnLocation) {
                item {
                    FilterOptionsCard(viewModel)
                }
                item {
                    Spacer(Modifier.height(12.dp))
                }
                items(postListItems) { post ->
                    post?.let {
                        Column {
                            PostComponent(post = it, onClick = {
                                onClickPost(it.id!!)
                            })
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            } else {
                item {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Spacer(modifier = Modifier.height(72.dp))
                        Icon (Icons.Rounded.LocationSearching, null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text ("Getting Location", style = MaterialTheme.typography.titleMedium)
                        Text ("This might take a few seconds...")
                    }
                }
            }
            postListItems.apply {
                when {
                    loadState.refresh is LoadState.Loading -> {

                    }
                    loadState.append is LoadState.Loading -> {
                        //You can add modifier to manage load state when next response page is loading
                    }
                    loadState.append is LoadState.Error -> {
                        //You can use modifier to show error message
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun PostComponent (
    post: Post,
    onClick: () -> Unit
) {
    val parsedDateTime =
        ZonedDateTime.parse(post.createdOn).format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm"));

    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)) {
            Text(
                "${post.author ?: "No author? Lol."} - $parsedDateTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .85f)
            )
            if (post.refURL != null && post.refURL.isNotBlank())
                Text(
                    post.refURL,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .85f)
                )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                post.title?:"No title? Lol.",
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column{
                    if (post.locality?.name != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            post.locality.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        "${post.distance?.toInt()} meter${if (post.distance?.toInt() != 1) "s" else ""} away",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Row {
                    if (post.refURL?.isNotBlank() == true)
                        Icon(Icons.Rounded.Link, null, tint = MaterialTheme.colorScheme.tertiary)
                    if (post.responseImage?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.secondary)
                    }
                    if (post.endTime != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PostPreview() {
    val post = Post(
        0,
        "Post Name",
        "Invisible RN",
        "https://wow.gov",
        "jlong",
        null,
//        "2018-06-05T12:42:48.545140Z",
        0.0f,
        0.0f,
        0,
        false,
        LocalityDto(
            "Rothesay",
            "id"
        ),
        distance = 0.0f
    )
    Surface() {
        PostComponent(post = post) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterOptionsCard (
    viewModel: DroidViewModel
) {
    val rangeRange = 0.001f..1f
    InfoDroidTheme{
//        Surface {
//            Surface(
//                modifier = Modifier
//                    .fillMaxWidth(),
//                shape = RoundedCornerShape(22.dp),
//                color = MaterialTheme.colorScheme.surface,
//                contentColor = MaterialTheme.colorScheme.onSurface,
//                tonalElevation = 3.dp
//            ) {
//                Column (modifier = Modifier
//                    .padding(12.dp)
//                    .fillMaxWidth())
//                {
//                    Text("Filter Options", style = MaterialTheme.typography.headlineSmall)
//                    Row (Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
//                        Text("Filter Range", style = MaterialTheme.typography.bodyLarge)
//                        Text("${(viewModel.filterRangePercent*viewModel.maxRangeMeters).toInt()} meter${if ((viewModel.filterRangePercent*viewModel.maxRangeMeters).toInt() != 1) "s" else ""}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
//                    }
//                    Slider(
//                        value = viewModel.filterRangePercent,
//                        onValueChange = {viewModel.filterRangePercent = it},
//                        valueRange = rangeRange,
//                        modifier = Modifier
//                            .fillMaxWidth(),
//                        colors = SliderDefaults.colors(
//                            thumbColor = MaterialTheme.colorScheme.secondary,
//                            activeTrackColor = MaterialTheme.colorScheme.secondary,
//                            inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
//                        ),
//                        enabled = !viewModel.localityFilter
//                    )
//                    Row (Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
//                        Text("Locality Mode", style = MaterialTheme.typography.bodyLarge)
//                        Text(if (viewModel.localityFilter) "ON" else "OFF", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
//                    }
//                    Text(
//                        "By using locality, we'll ignore your filter range and just search for posts which we think are in your community.",
//                        style = MaterialTheme.typography.bodySmall
//                    )
//                    Switch(
//                        checked = viewModel.localityFilter,
//                        onCheckedChange = {viewModel.localityFilter=it},
//                        colors = SwitchDefaults.colors(
//                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
//                            checkedTrackColor = MaterialTheme.colorScheme.secondary,
//                            uncheckedThumbColor = MaterialTheme.colorScheme.background,
//                            uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    )
//                }
                val localityChipBg by animateColorAsState(targetValue =
                    if (viewModel.localityFilter)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
                val localityChipFg by animateColorAsState(targetValue =
                    if (viewModel.localityFilter)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                val localityChipTonalElevation by animateDpAsState(targetValue =
                    if (viewModel.localityFilter)
                        1.dp
                    else
                        4.dp
                )
                val rangeChipBg by animateColorAsState(targetValue =
                    if (!viewModel.localityFilter)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
                val rangeChipFg by animateColorAsState(targetValue =
                    if (!viewModel.localityFilter)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                val rangeChipTonalElevation by animateDpAsState(targetValue =
                    if (!viewModel.localityFilter)
                        1.dp
                    else
                        4.dp
                )

                var showRangeDialog by remember { mutableStateOf(false) }


                Row (
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp, 4.dp)) {
                    AnimatedVisibility(visible = viewModel.currentLocality != null, enter = slideInHorizontally(), exit = slideOutHorizontally()) {
                        Row {
                            TonalChipButton(
                                onClick = {
                                    viewModel.localityFilter = !viewModel.localityFilter
                                },
                                text = viewModel.currentLocality ?: "Locality",
                                leadingIcon = Icons.Rounded.LocationCity,
                                color = localityChipBg,
                                contentColor = localityChipFg,
                                tonalElevation = localityChipTonalElevation
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    TonalChipButton(
                        onClick = { showRangeDialog = true },
                        text = "${(viewModel.filterRangePercent * viewModel.maxRangeMeters).toInt()} meter${if ((viewModel.filterRangePercent*viewModel.maxRangeMeters).toInt() != 1) "s" else ""}",
                        leadingIcon = Icons.Rounded.Explore,
                        color = rangeChipBg,
                        contentColor = rangeChipFg,
                        tonalElevation = rangeChipTonalElevation
                    )
                }
        if (showRangeDialog) {
            var tempRange by remember { mutableStateOf("${(viewModel.filterRangePercent * viewModel.maxRangeMeters).toInt()}") }
            var errorText by remember {
                mutableStateOf("")
            }
            AlertDialog(
                onDismissRequest = { showRangeDialog = false },
                confirmButton = {
                    Button(onClick = {
                        viewModel.filterRangePercent = tempRange.toFloat()/viewModel.maxRangeMeters
                        showRangeDialog = false
                    }) {
                        Text("Set Range")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRangeDialog = false }) {
                        Text("Cancel")
                    }
                },
                icon = {
                    Icon(Icons.Rounded.Explore, null)
                },
                title = {
                    Text("Filter Range")
                },
                text = {
                    Row (Modifier.fillMaxWidth()){
                        CustomOutlinedTextField(
                            value = tempRange,
                            onValueChange = {
                                if (!it.isDigitsOnly())
                                    errorText = "Must be a number."
                                else if (it.toInt() !in 9..10001)
                                    errorText = "Must be < 10m and > 10km"
                                else {
                                    errorText = ""
                                    tempRange = it
                                }
                            },
                            error = errorText,
                            isError = errorText.isNotBlank(),
                            label = {
                                Text("Meters")
                            }
                        )
                    }
                }
            )
        }
//            }
//        }
    }
}