package com.example.infodroid.network.dto

import com.google.gson.annotations.SerializedName

data class TestAuthResponseDto(
    @SerializedName("status") val status : String,
    @SerializedName("detail") val detail : String
)