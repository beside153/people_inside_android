package com.beside153.peopleinside.viewmodel.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.BuildConfig
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.common.exception.ApiException
import com.beside153.peopleinside.model.common.User
import com.beside153.peopleinside.repository.UserRepository
import com.beside153.peopleinside.service.AppVersionService
import com.beside153.peopleinside.service.ReportService
import com.beside153.peopleinside.service.UserService
import com.beside153.peopleinside.util.Event
import com.beside153.peopleinside.util.PreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

sealed interface SplashEvent {
    object UpdateApp : SplashEvent
    object GoToPlayStore : SplashEvent
    object NoUserInfo : SplashEvent
    data class OnBoardingCompleted(val isOnBoardingCompleted: Boolean, val isMember: Boolean) : SplashEvent
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val appVersionService: AppVersionService,
    private val reportService: ReportService,
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val prefs: PreferenceUtil
) : BaseViewModel() {

    private val _splashEvent = MutableLiveData<Event<SplashEvent>>()
    val splashEvent: LiveData<Event<SplashEvent>> = _splashEvent

    private var requiredAppVersion = BuildConfig.VERSION_NAME
    private lateinit var user: User

    init {
        viewModelScope.launch(Dispatchers.Default) {
            userRepository.userFlow.collectLatest { user = it }
        }
    }

    fun getAllData() {
        val ceh = CoroutineExceptionHandler { context, t ->
            when (t) {
                is ApiException -> {
                    if (t.error.statusCode == 404) {
                        _splashEvent.value = Event(SplashEvent.NoUserInfo)
                    } else {
                        exceptionHandler.handleException(context, t)
                    }
                }

                else -> {
                    exceptionHandler.handleException(context, t)
                }
            }
        }
        viewModelScope.launch(ceh) {
            val allReportList = reportService.getReportList()
            if (prefs.getJwtToken().isNotEmpty()) {
                requiredAppVersion = appVersionService.getAppVersionLatest("android").requiredVersionName
            }

            val currentAppVersion = BuildConfig.VERSION_NAME
            if (isNeedUpdate(currentAppVersion, requiredAppVersion)) {
                _splashEvent.value = Event(SplashEvent.UpdateApp)
                return@launch
            }

            var onBoardingCompleted = true
            if (user.userId != 0 && user.isMember) {
                val onBoardingDeferred = async { userService.getOnBoardingCompleted(user.userId) }
                onBoardingCompleted = onBoardingDeferred.await()
            }

            prefs.setReportList(Json.encodeToString(allReportList))

            if (onBoardingCompleted) {
                _splashEvent.value = Event(SplashEvent.OnBoardingCompleted(true, user.isMember))
                return@launch
            }
            _splashEvent.value = Event(SplashEvent.OnBoardingCompleted(false, user.isMember))
        }
    }

    fun onGoToPlayStoreButtonClick() {
        _splashEvent.value = Event(SplashEvent.GoToPlayStore)
    }

    private fun isNeedUpdate(currentAppVersion: String, requiredAppVersion: String): Boolean {
        var isNeedUpdate = false
        val arrX = currentAppVersion.split(".")
        val arrY = requiredAppVersion.split(".")

        val length = maxOf(arrX.size, arrY.size)

        for (i in 0 until length) {
            val x: Int = try {
                arrX.getOrNull(i)?.toInt() ?: 0
            } catch (e: NumberFormatException) {
                0
            }
            val y: Int = try {
                arrY.getOrNull(i)?.toInt() ?: 0
            } catch (e: NumberFormatException) {
                0
            }

            if (x > y) {
                // 앱 버전이 큼
                isNeedUpdate = false
                break
            } else if (x < y) {
                // 비교 버전이 큼
                isNeedUpdate = true
                break
            } else {
                // 버전 동일
                isNeedUpdate = false
            }
        }
        return isNeedUpdate
    }
}
