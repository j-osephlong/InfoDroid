package com.example.infodroid

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.infodroid.network.dto.*
import com.example.infodroid.network.repository.PostSource
import com.example.infodroid.network.repository.RetrofitHelper
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.CountDownLatch

class DroidViewModel: ViewModel() {
    var loggedIn by mutableStateOf(false)
    var token: String? = null
    var isLocationFresh by mutableStateOf(false)
    var lastLocationQuery by mutableStateOf<LocalDateTime?>(null)
    private val limit = 4
    val maxRangeMeters = 10000
    var filterRangePercent by mutableStateOf(1f)
    var localityFilter by mutableStateOf(false)
    lateinit var getLocation: ((Double, Double) -> Unit ) -> Any
    lateinit var storeToken: (String?) -> Unit
    var isRefreshing by mutableStateOf(false)
    var getPostsError by mutableStateOf<String?>(null)
    var postsWaitingOnLocation by mutableStateOf(false)
    var currentLocality by mutableStateOf<String?>(null)

    var titleText by
        mutableStateOf(
            ""
        )
    var urlText by
        mutableStateOf(
            ""
        )
    var contentText by
        mutableStateOf(
            ""
        )
    var privacyRange by
        mutableStateOf(
            .75f
        )
    var extendToLocality by
        mutableStateOf(
            false
        )
    var imgUri by
        mutableStateOf<Uri?>(null)
    var selectedDate by mutableStateOf<LocalDate?>(null)
    var selectedTime by mutableStateOf<LocalTime?>(null)

    var currentEditPost by mutableStateOf<Post?>(null)

    val posts: Flow<PagingData<Post>> = Pager(PagingConfig(pageSize = limit)) {
        PostSource(this) { getPostsError = it }
    }.flow.cachedIn(viewModelScope)

    fun getPost(id: Int, onSuccess: ((Post) -> Unit)?, onError: ((GenericErrorDto?) -> Unit)?) {

        getLocation { lon, lat ->
            viewModelScope.launch(Dispatchers.IO) {
                val postService = RetrofitHelper.getPostService()
                val responseService =
                    postService.get(
                        id = id,
                        "Token $token",
                        lat.toFloat(),
                        lon.toFloat(),
                        range = (filterRangePercent*maxRangeMeters).toInt(),
                    )

                if (responseService.isSuccessful) {
                    responseService.body()?.let { postResponse ->
                        onSuccess?.invoke(postResponse.post)
                    }
                } else {
                    responseService.errorBody()?.let{
                        try {
                            val gson = GsonBuilder().create()
                            val errors = gson.fromJson(it.string(), GenericErrorDto::class.java)
                            it.close()
                            onError?.invoke(errors)
                        } catch (e: JsonSyntaxException) {
                            onError?.invoke(null)
                        }
                    }
                }
            }
        }
    }

    fun getUserPosts(onSuccess: ((List<Post>) -> Unit)? = null, onError: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val authApiService = RetrofitHelper.getAuthService()
            val responseService =
                authApiService.getSelfPosts("Token $token")

            if (responseService.isSuccessful) {
                responseService.body()?.let { posts ->
                    onSuccess?.invoke(posts.posts?:listOf())
                }
            } else
                onError?.invoke()
        }
    }

    suspend fun getPosts(page: Int, onError: ((String) -> Unit)?) : List<Post> {
        Log.i("GET POSTS", "posts get, page $page, limit $limit")
        val latch = CountDownLatch(1)
        var lat: Float? = null
        var lon: Float? = null
        postsWaitingOnLocation = true
        getLocation { lo, la ->
            lat = la.toFloat()
            lon = lo.toFloat()
            Log.e("GET LOCATION", "WOW $lat $lon")
            postsWaitingOnLocation = false
            latch.countDown()
        }

        return withContext (Dispatchers.IO) {

            latch.await()
            Log.i("GET POSTS", "PAST WHILE")
            var errorMessage: String? = null
            val postService = RetrofitHelper.getPostService()
            val responseService = try {
                postService.getMany(
                    "Token $token",
                    lat,
                    lon,
                    range = if (localityFilter) null else (filterRangePercent*maxRangeMeters).toInt(),
                    limit = limit,
                    page = page
                )
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
                errorMessage = e.message
                null
            }
            if (responseService?.isSuccessful == true) {
                if (lat != null && lon != null)
                    getLocality(lat!!, lon!!)
                else
                    Log.e("GetLocality", "vars null")
                responseService.body()?.let { postResponse ->
                    return@withContext postResponse.posts
                }
            } else {
                errorMessage?.let {
                    onError?.invoke(errorMessage)
                }
                responseService?.errorBody()?.let{
                    return@withContext listOf<Post>()
                }
            }
            isRefreshing = false
        } as List<Post>
    }

    fun getLocality(lat: Float, lon: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val authService = RetrofitHelper.getAuthService()
            val responseService = authService.getSelf(
                "Token $token",
                lon,
                lat
            )
            if (responseService.isSuccessful) {
                responseService.body()?.let { selfDto ->
                    selfDto.locality?.also {
                        if (it.googlePlaceID != "0")
                        currentLocality = it.name
                    }
                }
            }
        }
    }

    fun submitPost(
        post: Post,
        onSuccess: (() -> Unit)? = null,
        onError: ((NewPostErrorDto?) -> Unit)? = null
    ) {
        Log.i("POST", "post submission started")

        viewModelScope.launch(Dispatchers.IO) {
            val postService = RetrofitHelper.getPostService()
            val responseService = postService.post(
                "Token $token",
                post
            )

            if (responseService.isSuccessful) {
                clearNewPostFields()
                onSuccess?.invoke()
            } else {
                responseService.errorBody()?.let{
                    try {
                        val gson = GsonBuilder().create()
                        val errors = gson.fromJson(it.string(), NewPostErrorDto::class.java)
                        it.close()
                        onError?.invoke(errors)
                    } catch (e: JsonSyntaxException) {
                        onError?.invoke((null))
                    }
                }
            }
        }
    }

    fun editPost(
        id: Int,
        post: Post,
        onSuccess: (() -> Unit)? = null,
        onError: ((NewPostErrorDto?) -> Unit)? = null
    ) {
        Log.i("EDIT", "post edit submission started")

        viewModelScope.launch(Dispatchers.IO) {
            val postService = RetrofitHelper.getPostService()
            val responseService = postService.edit(
                id,
                "Token $token",
                post
            )

            if (responseService.isSuccessful) {
                clearNewPostFields()
                onSuccess?.invoke()
            } else {
                responseService.errorBody()?.let{
                    try {
                        val gson = GsonBuilder().create()
                        val errors = gson.fromJson(it.string(), NewPostErrorDto::class.java)
                        it.close()
                        onError?.invoke(errors)
                    } catch (e: JsonSyntaxException) {
                        onError?.invoke((null))
                    }
                }
            }
        }
    }

    fun deletePost (
        id: Int,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val postService = RetrofitHelper.getPostService()
            val responseService = postService.delete(
                id,
                "Token $token"
            )

            if (responseService.isSuccessful) {
                onSuccess?.invoke()
            } else {
                responseService.errorBody()?.let {
                    val gson = GsonBuilder().create()
                    val errors = gson.fromJson(it.string(), GenericErrorDto::class.java)
                    onError?.invoke(errors.detail.toString())
                }
            }
        }
    }

    fun login(
        username: String,
        password: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((LoginRegisterErrorDto?) -> Unit)? = null
    ) {
        Log.i("LOGIN", "login started")


        viewModelScope.launch(Dispatchers.IO) {
            val authService = RetrofitHelper.getAuthService()
            val responseService = authService.login(
                LoginRegisterDto(username, password)
            )
            if (responseService.isSuccessful) {
                responseService.body()?.let { tokenDto ->
                    token = tokenDto.accessTokenVerify
                    Log.i("tokenR", tokenDto.accessTokenVerify)
                    Log.i("token", "$token")
                }
                storeToken(token)
                onSuccess?.invoke()
                loggedIn = true
            } else {
                responseService.errorBody()?.let{
                    try {
                        val gson = GsonBuilder().create()
                        val errors = gson.fromJson(it.string(), LoginRegisterErrorDto::class.java)
                        it.close()
                        onError?.invoke(errors)
                    } catch (e: JsonSyntaxException) {
                        e.printStackTrace()
                        onError?.invoke(null)
                    }
                }
                Log.i("LOGIN", "login failed")

            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            val authService = RetrofitHelper.getAuthService()
            val responseService = authService.logout("Token $token")

            if (responseService.isSuccessful) {
                token = null
                storeToken(token)
                loggedIn = false
            }
        }
    }

    fun testAuth(onResponse: (Boolean) -> Unit) {
        viewModelScope.launch (Dispatchers.IO) {
            val authService = RetrofitHelper.getAuthService()
            try {
                val responseService = authService.testAuth("Token $token")

                if (!responseService.isSuccessful) {
                    token = null
                    loggedIn = false
                    storeToken(token)
                }
                else
                    loggedIn = true

                onResponse(responseService.isSuccessful)
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
                testAuth(onResponse)
            }
        }
    }

    fun register(
        username: String,
        password: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((LoginRegisterErrorDto?) -> Unit)? = null
    ) {
        var success = false
        viewModelScope.launch(Dispatchers.IO) {
            val authService = RetrofitHelper.getAuthService()
            val responseService = authService.register(
                LoginRegisterDto(username, password)
            )
            if (!responseService.isSuccessful) {
                responseService.errorBody()?.let{
                    try {
                        val gson = GsonBuilder().create()
                        val errors = gson.fromJson(it.string(), LoginRegisterErrorDto::class.java)
                        Log.e("ERROR_JLONG", errors!!.toString())
                        it.close()
                        onError?.invoke(errors)
                    } catch (e: JsonSyntaxException) {
                        onError?.invoke(null)
                    }
                }
            } else {
                login(username, password, onSuccess)
            }
        }
    }

    fun clearNewPostFields() {
        titleText = ""
        contentText = ""
        urlText = ""
        imgUri = null
        privacyRange = .75f
        extendToLocality = false
        currentEditPost = null
        selectedDate = null
        selectedTime = null
    }

}