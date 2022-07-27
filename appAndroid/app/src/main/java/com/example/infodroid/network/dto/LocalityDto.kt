package com.example.infodroid.network.dto

import com.google.gson.annotations.SerializedName

data class LocalityDto(
    @SerializedName("name") val name: String,
    @SerializedName("google_place_id") val googlePlaceID: String
)