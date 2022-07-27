package com.example.infodroid.network.repository

import com.example.infodroid.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("api/login/")
    suspend fun login(@Body loginDto: LoginRegisterDto) : Response<TokenDto>

    @POST("api/logout/")
    suspend fun logout(@Header("Authorization") token: String) : Response<LogoutResponseDto>

    @GET("api/test-auth/")
    suspend fun testAuth(@Header("Authorization") token: String) : Response<TestAuthResponseDto>

    @POST("api/register/")
    suspend fun register(@Body registerDto: LoginRegisterDto) : Response<StatusDto>

    @GET("api/self/")
    suspend fun getSelf(
        @Header("Authorization") token: String,
        @Query("lon") longitude: Float? = null,
        @Query("lat") latitude: Float? = null
    ) : Response<SelfDto>

    @GET("api/self/posts/")
    suspend fun  getSelfPosts(
        @Header("Authorization") token: String
    ) : Response<UserPostsDto>
}