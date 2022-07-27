package com.example.infodroid.network.dto

import com.google.gson.annotations.SerializedName

data class Post (
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("ref_url") val refURL: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("created_on") val createdOn: String? = null,
    @SerializedName("coord_longitude") val coordLongitude: Float? = null,
    @SerializedName("coord_latitude") val coordLatitude: Float? = null,
    @SerializedName("range_meters") val rangeMeters: Int? = null,
    @SerializedName("extend_to_locality") val extendToLocality: Boolean? = null,
    @SerializedName("locality") val locality: LocalityDto? = null,
    @SerializedName("distance") val distance: Float? = null,
    @SerializedName("is_my_post") val isMyPost: Boolean? = null,
    @SerializedName("image") val responseImage: String? = null,
    @SerializedName("image_base64") val requestImageBase64: String? = null,
    @SerializedName("end_time") val endTime: String? = null
)

data class ManyPostResponseDto (
    @SerializedName("status") val status: String,
    @SerializedName("data") val posts: List<Post>
)

data class PostResponseDto (
    @SerializedName("status") val status: String,
    @SerializedName("data") val post: Post
)

data class NewPostErrorDto (
    @SerializedName("non_field_errors") val non_field_errors: List<String>?,
    @SerializedName("title") val title: List<String>?,
    @SerializedName("content") val content: List<String>?,
    @SerializedName("ref_url") val refURL: List<String>?,
    @SerializedName("author") val author: List<String>?,
    @SerializedName("created_on") val createdOn: List<String>?,
    @SerializedName("coord_longitude") val coordLongitude: List<String>?,
    @SerializedName("coord_latitude") val coordLatitude: List<String>?,
    @SerializedName("range_meters") val rangeMeters: List<String>?,
    @SerializedName("extend_to_locality") val extendToLocality: List<String>?,
    @SerializedName("locality") val locality: List<String>?,
    @SerializedName("image") val responseImage: List<String>?,
    @SerializedName("end_time") val endTime: List<String>?
)
