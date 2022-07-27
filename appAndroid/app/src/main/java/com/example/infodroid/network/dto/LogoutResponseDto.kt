package com.example.infodroid.network.dto

import com.google.gson.annotations.SerializedName

data class LogoutResponseDto (
    @SerializedName("detail") val detail: String
)