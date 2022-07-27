package com.example.infodroid.network.repository

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.infodroid.DroidViewModel
import com.example.infodroid.network.dto.Post
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.io.IOException

class PostSource(private val viewModel: DroidViewModel, private val onError: (String) -> Unit) : PagingSource<Int, Post>() {
    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        Log.i("GET REFRESH KEY", "${state.anchorPosition}")
        return 0
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        return try {
            val nextPage = params.key ?: 0
        Log.i("LOAD_POSTS", "load func entered")
            val posts = viewModel.getPosts(page = nextPage, onError = onError)
            Log.i("LOAD_POSTS", "nextPage : ${if (posts.isEmpty()) null else nextPage + 1}")
            LoadResult.Page(
                data = posts,
                prevKey = if (nextPage == 0) null else nextPage - 1,
                nextKey = if (posts.isEmpty()) null else nextPage + 1
            )
        } catch (e: IOException) {
            return  LoadResult.Error(e)
        } catch (e: HttpException) {
            return  LoadResult.Error(e)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("PostSource", "Timeout, retrying (recursive).")
            return load(params)
        }
    }

}