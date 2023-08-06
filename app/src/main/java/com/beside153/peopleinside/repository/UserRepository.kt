package com.beside153.peopleinside.repository

import com.beside153.peopleinside.App
import com.beside153.peopleinside.model.user.UserInfo
import com.beside153.peopleinside.service.AuthService
import com.beside153.peopleinside.util.PreferenceUtil
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class User(val id: Long)

@Singleton
class UserRepository @Inject constructor(
    private val authService: AuthService,
    private val prefs: PreferenceUtil
) {

    private val _userFlow = MutableSharedFlow<User>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val userFlow = _userFlow.asSharedFlow()

    init {
        // 지금 앱에 회원상태로 되어있으면 유저정보 업데이트
        // 아니면 게스트
        // 프리퍼런스 데이터를 여기서 한번 가져와서 userFlow 갱신
    }

    fun updateUser(user: User) {
        _userFlow.tryEmit(user)
    }

    suspend fun loginWithKakao(authToken: String): UserInfo {
        val response = authService.postLoginKakao("Bearer $authToken")
        val jwtToken = response.jwtToken
        val user = response.user

        prefs.setJwtToken(jwtToken)
        prefs.setUserId(user.userId)
        prefs.setNickname(user.nickname)
        prefs.setMbti(user.mbti)
        prefs.setBirth(user.birth)
        prefs.setGender(user.sex)
        prefs.setIsMember(true)

        return user
    }
}
