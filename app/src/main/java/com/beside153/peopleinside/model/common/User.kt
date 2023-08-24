package com.beside153.peopleinside.model.common

data class User(
    val userId: Int,
    val nickname: String,
    val mbti: String,
    val birth: String,
    val gender: String,
    val email: String? = "",
    val isMember: Boolean
)
