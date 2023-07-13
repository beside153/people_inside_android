package com.beside153.peopleinside.viewmodel.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.beside153.peopleinside.App
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.service.OnBoardingService
import com.beside153.peopleinside.service.ReportService
import com.beside153.peopleinside.service.RetrofitClient
import com.beside153.peopleinside.util.Event
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SplashViewmodel(private val reportService: ReportService, private val onBoardingService: OnBoardingService) :
    BaseViewModel() {

    private val _onBoardingCompletedEvent = MutableLiveData<Event<Boolean>>()
    val onBoardingCompletedEvent: LiveData<Event<Boolean>> get() = _onBoardingCompletedEvent

    private var onBoardingCompleted = true

    fun getAllData() {
        viewModelScope.launch(exceptionHandler) {
            val reportDeferred = async { reportService.getReportList() }
            val reportList = reportDeferred.await()

            if (App.prefs.getUserId() != 0 && App.prefs.getNickname() != "익명의 핍사이더") {
                val onBoardingDeferred = async { onBoardingService.getOnBoardingCompleted(App.prefs.getUserId()) }
                onBoardingCompleted = onBoardingDeferred.await()
            }

            App.prefs.setString(App.prefs.reportListKey, Json.encodeToString(reportList))

            if (onBoardingCompleted) {
                _onBoardingCompletedEvent.value = Event(true)
                return@launch
            }
            _onBoardingCompletedEvent.value = Event(false)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val reportService = RetrofitClient.reportService
                val onBoardingService = RetrofitClient.onBoardingService
                return SplashViewmodel(reportService, onBoardingService) as T
            }
        }
    }
}