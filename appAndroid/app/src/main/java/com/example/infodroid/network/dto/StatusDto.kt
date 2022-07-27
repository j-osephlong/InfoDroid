package com.example.infodroid.network.dto

import com.google.gson.annotations.SerializedName

data class StatusDto (
    @SerializedName("status") val status: String
)

data class GenericErrorDto (
    @SerializedName("status") val status: String?,
    @SerializedName("data") val data: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("detail") val detail: String?
)