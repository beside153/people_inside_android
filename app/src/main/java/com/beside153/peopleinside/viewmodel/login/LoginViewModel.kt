package com.beside153.peopleinside.viewmodel.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.App
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.common.exception.ApiException
import com.beside153.peopleinside.repository.UserRepository
import com.beside153.peopleinside.service.AuthService
import com.beside153.peopleinside.service.UserService
import com.beside153.peopleinside.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: AuthService,
    private val userService: UserService,
    private val userRepository: UserRepository
) : BaseViewModel() {

    private val _kakaoLoginClickEvent = MutableLiveData<Event<Unit>>()
    val kakaoLoginClickEvent: LiveData<Event<Unit>> get() = _kakaoLoginClickEvent

    private val _goToSignUpEvent = MutableLiveData<Event<String>>()
    val goToSignUpEvent: LiveData<Event<String>> get() = _goToSignUpEvent

    private val _onBoardingCompletedEvent = MutableLiveData<Event<Boolean>>()
    val onBoardingCompletedEvent: LiveData<Event<Boolean>> get() = _onBoardingCompletedEvent

    private val _withoutLoginClickEvent = MutableLiveData<Event<Unit>>()
    val withoutLoginClickEvent: LiveData<Event<Unit>> get() = _withoutLoginClickEvent

    private var authToken = ""

    init {
        viewModelScope.launch {
            userRepository.userFlow.collectLatest {

            }
        }

    }

    fun setAuthToken(token: String) {
        authToken = token
    }

    fun onKakaoLoginClick() {
        _kakaoLoginClickEvent.value = Event(Unit)
    }

    fun peopleInsideLogin() {
        val ceh = CoroutineExceptionHandler { context, t ->
            when (t) {
                is ApiException -> {
                    if (t.error.statusCode == 401) {
                        _goToSignUpEvent.value = Event(authToken)
                    } else {
                        exceptionHandler.handleException(context, t)
                    }
                }

                else -> exceptionHandler.handleException(context, t)
            }
        }

        viewModelScope.launch(ceh) {
            val user = userRepository.loginWithKakao(authToken)
            val onBoardingCompleted = userService.getOnBoardingCompleted(user.userId)
            if (onBoardingCompleted) {
                _onBoardingCompletedEvent.value = Event(true)
            } else {
                _onBoardingCompletedEvent.value = Event(false)
            }
        }
    }

    fun onWithoutLoginClick() {
        _withoutLoginClickEvent.value = Event(Unit)
    }
}
