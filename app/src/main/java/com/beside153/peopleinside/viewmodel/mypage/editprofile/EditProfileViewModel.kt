package com.beside153.peopleinside.viewmodel.mypage.editprofile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.App
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.common.exception.ApiException
import com.beside153.peopleinside.model.common.User
import com.beside153.peopleinside.model.editprofile.EdittedUserInfo
import com.beside153.peopleinside.repository.UserRepository
import com.beside153.peopleinside.service.UserService
import com.beside153.peopleinside.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EditProfileEvent {
    object BirthYearClick : EditProfileEvent
    object MbtiChoiceClick : EditProfileEvent
    object CompleteButtonClick : EditProfileEvent
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userService: UserService,
    private val userRepository: UserRepository
) : BaseViewModel() {

    val nickname = MutableLiveData("")

    private val _nicknameCount = MutableLiveData(0)
    val nicknameCount: LiveData<Int> get() = _nicknameCount

    private val _selectedGender = MutableLiveData("")
    val selectedGender: LiveData<String> get() = _selectedGender

    private val _selectedYear = MutableLiveData(0)
    val selectedYear: LiveData<Int> get() = _selectedYear

    private val _selectedMbti = MutableLiveData("")
    val selectedMbti: LiveData<String> get() = _selectedMbti

    private val _nicknameIsEmpty = MutableLiveData(false)
    val nicknameIsEmpty: LiveData<Boolean> get() = _nicknameIsEmpty

    private val _isDuplicate = MutableLiveData(false)
    val isDuplicate: LiveData<Boolean> get() = _isDuplicate

    private val _hasBadWord = MutableLiveData(false)
    val hasBadWord: LiveData<Boolean> get() = _hasBadWord

    private val _editProfileEvent = MutableLiveData<Event<EditProfileEvent>>()
    val editProfileEvent: LiveData<Event<EditProfileEvent>> = _editProfileEvent

    private lateinit var user: User

    init {
        viewModelScope.launch(Dispatchers.Default) {
            userRepository.userFlow.collectLatest { user = it }
        }
    }

    fun onNicknameTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        nickname.value = (s ?: "").toString()
        _nicknameCount.value = s?.length ?: 0
        _isDuplicate.value = false
        _hasBadWord.value = false
    }

    fun onBirthYearClick() {
        _editProfileEvent.value = Event(EditProfileEvent.BirthYearClick)
    }

    fun onMbtiChoiceClick() {
        _editProfileEvent.value = Event(EditProfileEvent.MbtiChoiceClick)
    }

    fun setSelectedGender(gender: String) {
        _selectedGender.value = gender
    }

    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
    }

    fun setSelectedMbti(mbti: String) {
        _selectedMbti.value = mbti
    }

    fun setNickname(nickname: String) {
        this.nickname.value = nickname
    }

    fun setNicknameCount(count: Int) {
        _nicknameCount.value = count
    }

    fun onCompleteButtonClick() {
        val ceh = CoroutineExceptionHandler { context, t ->
            when (t) {
                is ApiException -> {
                    when (t.error.statusCode) {
                        400 -> {
                            _isDuplicate.value = true
                        }

                        403 -> {
                            _hasBadWord.value = true
                        }

                        else -> {
                            exceptionHandler.handleException(context, t)
                        }
                    }
                }

                else -> exceptionHandler.handleException(context, t)
            }
        }
        viewModelScope.launch(ceh) {
            if ((nickname.value?.length ?: 0) <= 0) {
                setNicknameIsEmpty(true)
                return@launch
            }

            userService.patchUserInfo(
                user.userId,
                EdittedUserInfo(
                    nickname.value ?: "",
                    _selectedMbti.value ?: "",
                    _selectedYear.value.toString(),
                    _selectedGender.value ?: ""
                )
            )

            userRepository.updateUser(
                user.copy(
                    nickname = nickname.value ?: "",
                    mbti = _selectedMbti.value ?: "",
                    birth = _selectedYear.value.toString(),
                    gender = _selectedGender.value ?: ""
                )
            )

            // TODO: 앱 전체 userRepository로 수정 후 아래 코드를 삭제
            App.prefs.setNickname(nickname.value ?: "")
            App.prefs.setMbti(_selectedMbti.value ?: "")
            App.prefs.setBirth(_selectedYear.value.toString())
            App.prefs.setGender(_selectedGender.value ?: "")

            _editProfileEvent.value = Event(EditProfileEvent.CompleteButtonClick)
        }
    }

    fun setNicknameIsEmpty(isEmpty: Boolean) {
        _nicknameIsEmpty.value = isEmpty
    }
}
