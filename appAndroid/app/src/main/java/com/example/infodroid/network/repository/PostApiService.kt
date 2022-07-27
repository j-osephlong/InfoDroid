package com.example.infodroid.network.repository

import com.example.infodroid.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface PostApiService {

    @POST("api/post/")
    suspend fun post(@Header("Authorization") token: String, @Body post: Post) : Response<PostResponseDto>

    @GET("api/post/{id}")
    suspend fun get(
        @Path("id") id: Int,
        @Header("Authorization") token: String,
        @Query("lat") latitude: Float? = null,
        @Query("lon") longitude: Float? = null,
        @Query("range") range: Int? = null
    ) : Response<PostResponseDto>

    @GET("api/post/")
    suspend fun getMany(
        @Header("Authorization") token: String,
        @Query("lat") latitude: Float? = null,
        @Query("lon") longitude: Float? = null,
        @Query("range") range: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("page") page: Int? = null
    ) : Response<ManyPostResponseDto>

    @DELETE("api/post/{id}/")
    suspend fun delete(
        @Path("id") id: Int,
        @Header("Authorization") token: String,
    ) : Response<StatusDto>

    @PATCH("api/post/{id}/")
    suspend fun edit(
        @Path("id") id: Int,
        @Header("Authorization") token: String,
        @Body post: Post
    ) : Response<PostResponseDto>
}