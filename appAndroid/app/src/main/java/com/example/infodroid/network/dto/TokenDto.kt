package com.example.infodroid.network.dto

import com.google.gson.annotations.SerializedName

data class TokenDto(
    @SerializedName("key") val accessTokenVerify: String
)