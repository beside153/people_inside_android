package com.beside153.peopleinside.repository

import com.beside153.peopleinside.App
import com.beside153.peopleinside.model.auth.AuthRegisterRequest
import com.beside153.peopleinside.model.common.User
import com.beside153.peopleinside.service.AuthService
import com.beside153.peopleinside.util.PreferenceUtil
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val prefs: PreferenceUtil,
    private val authService: AuthService
) {

    private val _userFlow = MutableSharedFlow<User>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val userFlow = _userFlow.asSharedFlow()

    init {
        // 프리퍼런스 데이터를 여기서 한번 가져와서 userFlow 갱신
        val userId = prefs.getUserId()
        val nickname = prefs.getNickname()
        val mbti = prefs.getMbti()
        val birth = prefs.getBirth()
        val gender = prefs.getGender()
        val email = prefs.getEmail()

        if (prefs.getIsMember()) {
            // 지금 앱에 회원상태로 되어있으면 유저정보 업데이트
            val user = User(userId, nickname, mbti, birth, gender, email, isMember = true)
            updateUser(user)
        } else {
            // 비회원(게스트) 상태일 경우, 게스트 유저정보로 업데이트
            updateUserToGuest(mbti)
        }
    }

    fun updateUser(user: User) {
        _userFlow.tryEmit(user)
    }

    fun updateUserToGuest(mbti: String) {
        val guestUser = User(guestUserId, "익명의 핍사이더", mbti, "", "", isMember = false)
        _userFlow.tryEmit(guestUser)

        // TODO: 앱 전체 userRepository로 수정 후 아래 코드를 삭제
        App.prefs.setJwtToken(guestUserJwtToken)
        App.prefs.setUserId(guestUserId)
        App.prefs.setNickname("익명의 핍사이더")
        App.prefs.setMbti(mbti)
        App.prefs.setIsMember(false)
    }

    suspend fun loginWithKakao(authToken: String, email: String): User {
        val response = authService.postLoginKakao("Bearer $authToken")

        val jwtToken = response.jwtToken
        val userInfo = response.user
        val userId = userInfo.userId
        val nickname = userInfo.nickname
        val mbti = userInfo.mbti
        val birth = userInfo.birth
        val gender = userInfo.sex

        // TODO: 앱 전체 userRepository로 수정 후 아래 코드를 삭제
        prefs.setJwtToken(jwtToken)
        prefs.setUserId(userInfo.userId)
        prefs.setNickname(userInfo.nickname)
        prefs.setMbti(userInfo.mbti)
        prefs.setBirth(userInfo.birth)
        prefs.setGender(userInfo.sex)
        prefs.setEmail(email)
        prefs.setIsMember(true)

        val user = User(userId, nickname, mbti, birth, gender, email, isMember = true)
        updateUser(user)

        return user
    }

    suspend fun postAuthRegister(
        authToken: String,
        selectedNickname: String,
        selectedMbti: String,
        selectedBirth: String,
        selectedGender: String
    ): User {
        val response = authService.postAuthRegister(
            "Bearer $authToken",
            AuthRegisterRequest("kakao", selectedNickname, selectedMbti, selectedBirth, selectedGender)
        )

        val jwtToken = response.jwtToken
        val userInfo = response.user
        val userId = userInfo.userId
        val nickname = userInfo.nickname
        val mbti = userInfo.mbti
        val birth = userInfo.birth
        val gender = userInfo.sex
        val email = prefs.getEmail()

        // TODO: 앱 전체 userRepository로 수정 후 아래 코드를 삭제
        prefs.setJwtToken(jwtToken)
        prefs.setUserId(userInfo.userId)
        prefs.setNickname(userInfo.nickname)
        prefs.setMbti(userInfo.mbti)
        prefs.setBirth(userInfo.birth)
        prefs.setGender(userInfo.sex)
        prefs.setIsMember(true)

        val user = User(userId, nickname, mbti, birth, gender, email, isMember = true)
        updateUser(user)

        return user
    }

    companion object {
        private const val guestUserId = 1
        private const val guestUserJwtToken =
            "eyJhbGciOiJIUzI1NiJ9" +
                ".eyJ1c2VyX2lkIjoxLCJzb2NpYWwiOiJzcGFjZSIsIm5pY2tuYW1lI" +
                "joiZ29kIiwibWJ0aSI6IkFCQ0QiLCJiaXJ0aCI6Ijk5OTkiLCJzZXgiOiJhbGllbiJ9" +
                ".q0DbIef5nxoJGJTmbF9m0JaLgLK17CzO49iMYMXkEbo"
    }
}
