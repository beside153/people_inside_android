package com.beside153.peopleinside.viewmodel.mypage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.model.common.User
import com.beside153.peopleinside.model.user.UserInfo
import com.beside153.peopleinside.repository.UserRepository
import com.beside153.peopleinside.service.MyContentService
import com.beside153.peopleinside.service.UserService
import com.beside153.peopleinside.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyPageEvent {
    object BookmarkContentsClick : MyPageEvent
    object RatingContentsClick : MyPageEvent
    object EditProfileClick : MyPageEvent
    object SettingClick : MyPageEvent
}

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val myContentService: MyContentService,
    private val userService: UserService,
    private val userRepository: UserRepository
) : BaseViewModel() {
    private val _bookmarkCount = MutableLiveData(0)
    val bookmarkCount: LiveData<Int> get() = _bookmarkCount

    private val _ratingCount = MutableLiveData(0)
    val ratingCount: LiveData<Int> get() = _ratingCount

    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> get() = _userInfo

    private val _myPageEvent = MutableLiveData<Event<MyPageEvent>>()
    val myPageEvent: LiveData<Event<MyPageEvent>> = _myPageEvent

    private lateinit var user: User

    init {
        viewModelScope.launch(Dispatchers.Default) {
            userRepository.userFlow.collectLatest { user = it }
        }
    }

    fun initAllData() {
        viewModelScope.launch(exceptionHandler) {
            val bookmarkCountDeferred = async { myContentService.getBookmarkedCount() }
            val ratingCountDeferred = async { myContentService.getRatedCount() }
            val userInfoDeffered = async { userService.getUserInfo(user.userId) }

            _bookmarkCount.value = bookmarkCountDeferred.await()
            _ratingCount.value = ratingCountDeferred.await()
            _userInfo.value = userInfoDeffered.await()
        }
    }

    fun onBookmarkContentsClick() {
        _myPageEvent.value = Event(MyPageEvent.BookmarkContentsClick)
    }

    fun onRatingContentsClick() {
        _myPageEvent.value = Event(MyPageEvent.RatingContentsClick)
    }

    fun onEditProfileClick() {
        _myPageEvent.value = Event(MyPageEvent.EditProfileClick)
    }

    fun onSettingClick() {
        _myPageEvent.value = Event(MyPageEvent.SettingClick)
    }
}
