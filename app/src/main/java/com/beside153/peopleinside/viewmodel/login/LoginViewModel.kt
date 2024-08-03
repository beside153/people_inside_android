package com.beside153.peopleinside.viewmodel.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.common.exception.ApiException
import com.beside153.peopleinside.repository.UserRepository
import com.beside153.peopleinside.service.UserService
import com.beside153.peopleinside.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginEvent {
    object KakaoLoginClick : LoginEvent
    object WithoutLoginClick : LoginEvent
    data class GoToSignUp(val authToken: String) : LoginEvent
    data class OnBoardingCompleted(val isCompleted: Boolean) : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userService: UserService,
    private val userRepository: UserRepository
) : BaseViewModel() {

    private val _loginEvent = MutableLiveData<Event<LoginEvent>>()
    val loginEvent: LiveData<Event<LoginEvent>> = _loginEvent

    private var authToken = ""

    fun setAuthToken(token: String) {
        authToken = token
    }

    fun onKakaoLoginClick() {
        _loginEvent.value = Event(LoginEvent.KakaoLoginClick)
    }

    fun login(email: String) {
        val ceh = CoroutineExceptionHandler { context, t ->
            when (t) {
                is ApiException -> {
                    if (t.error.statusCode == 401) {
                        _loginEvent.value = Event(LoginEvent.GoToSignUp(authToken))
                    } else {
                        exceptionHandler.handleException(context, t)
                    }
                }

                else -> exceptionHandler.handleException(context, t)
            }
        }

        viewModelScope.launch(ceh) {
            val user = userRepository.loginWithKakao(authToken, email)
            val onBoardingCompleted = userService.getOnBoardingCompleted(user.userId)
            if (onBoardingCompleted) {
                _loginEvent.value = Event(LoginEvent.OnBoardingCompleted(true))
            } else {
                _loginEvent.value = Event(LoginEvent.OnBoardingCompleted(false))
            }
        }
    }

    fun onWithoutLoginClick() {
        _loginEvent.value = Event(LoginEvent.WithoutLoginClick)
    }
}
