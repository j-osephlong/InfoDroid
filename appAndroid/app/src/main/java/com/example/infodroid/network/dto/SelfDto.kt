package com.example.infodroid.network.dto

import com.google.gson.annotations.SerializedName

data class SelfDto(
    @SerializedName("status") val status: String,
    @SerializedName("user") val user: User,
    @SerializedName("locality") val locality: LocalityDto?,
)

data class UserPostsDto (
    @SerializedName("posts") val posts: List<Post>?
)

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("last_login") val lastLogin: String,
    @SerializedName("is_superuser") val isSuperuser: Boolean,
    @SerializedName("username") val username: String,
    @SerializedName("date_joined") val dateJoined: String,
)

/**
 * "user": {
"id": 2,
"password": "pbkdf2_sha256$320000$bJfM9ppphDvzDB2jLQhYp7$gp388pfObzJfj0axnk1zAF11eG/7IcSjAVC2Q3BVozA=",
"last_login": "2022-01-19T17:51:01.660067Z",
"is_superuser": false,
"username": "testUser1",
"first_name": "",
"last_name": "",
"email": "",
"is_staff": false,
"is_active": true,
"date_joined": "2022-01-16T23:33:03.420369Z",
"groups": [],
"user_permissions": []
}
 */