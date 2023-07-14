package com.beside153.peopleinside.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.beside153.peopleinside.R
import com.beside153.peopleinside.service.RetrofitClient
import com.beside153.peopleinside.util.Event
import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber

open class BaseViewModel : ViewModel() {

    val backButtonClickEvent = MutableLiveData<Event<Unit>>()

    private val _error = MutableLiveData<Event<Int>>()
    val error: LiveData<Event<Int>> get() = _error

    fun onBackButtonClick() {
        backButtonClickEvent.value = Event(Unit)
    }

    protected val exceptionHandler = CoroutineExceptionHandler { _, t ->
        when (t) {
            is RetrofitClient.ApiException -> {
                Timber.e(t.error.toString())
                _error.value = Event(R.string.kakao_user_info_load_failed)
                Timber.e(t)
            }

            is Exception -> {
                _error.value = Event(R.string.not_found_page)
                Timber.e(t)
            }
        }
    }
}
