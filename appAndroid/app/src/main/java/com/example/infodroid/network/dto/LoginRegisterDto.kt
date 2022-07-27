package com.example.infodroid.network.dto

import com.google.gson.annotations.SerializedName

data class LoginRegisterDto(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginRegisterErrorDto(
    @SerializedName("non_field_errors") val non_field_errors: List<String>?,
    @SerializedName("password") val password_error: List<String>?,
    @SerializedName("username") val username_error: List<String>?,
)