package com.beside153.peopleinside.viewmodel.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.model.common.User
import com.beside153.peopleinside.model.mediacontent.OnBoardingChosenContentModel
import com.beside153.peopleinside.model.mediacontent.OnBoardingContentModel
import com.beside153.peopleinside.repository.UserRepository
import com.beside153.peopleinside.service.UserService
import com.beside153.peopleinside.service.mediacontent.MediaContentService
import com.beside153.peopleinside.util.Event
import com.beside153.peopleinside.view.onboarding.ContentScreenAdapter.ContentScreenModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContentChoiceViewModel @Inject constructor(
    private val mediaContentService: MediaContentService,
    private val userService: UserService,
    private val userRepository: UserRepository
) : BaseViewModel() {

    private val _choiceCount = MutableLiveData(0)
    val choiceCount: LiveData<Int> get() = _choiceCount

    private val _isCompleteButtonEnable = MutableLiveData(false)
    val isCompleteButtonEnable: LiveData<Boolean> get() = _isCompleteButtonEnable

    private val _completeButtonClickEvent = MutableLiveData<Event<Unit>>()
    val completeButtonClickEvent: LiveData<Event<Unit>> get() = _completeButtonClickEvent

    private val _screenList = MutableLiveData<List<ContentScreenModel>>()
    val screenList: LiveData<List<ContentScreenModel>> get() = _screenList

    private var page = 1
    private var contentList = listOf<OnBoardingContentModel>()
    private lateinit var user: User

    init {
        viewModelScope.launch(Dispatchers.Default) {
            userRepository.userFlow.collectLatest { user = it }
        }
    }

    fun initAllData() {
        viewModelScope.launch(exceptionHandler) {
            contentList = mediaContentService.getOnBoardingContents(page)
        }
        _screenList.value = screenList()
    }

    fun loadMoreData() {
        viewModelScope.launch(exceptionHandler) {
            val newContentList = mediaContentService.getOnBoardingContents(++page)
            contentList = contentList.plus(newContentList)

            _screenList.value = screenList()
        }
    }

    fun onContentItemClick(item: OnBoardingContentModel) {
        val updatedList = contentList.map {
            if (it == item) {
                if (!it.isChosen) {
                    _choiceCount.value = _choiceCount.value?.plus(1)
                    it.copy(isChosen = true)
                } else {
                    _choiceCount.value = _choiceCount.value?.minus(1)
                    it.copy(isChosen = false)
                }
            } else {
                it
            }
        }

        contentList = updatedList
        checkCompleteButtonEnable()
        _screenList.value = screenList()
    }

    private fun screenList(): List<ContentScreenModel> {
        return listOf(
            ContentScreenModel.TitleViewItem,
            *contentList.map { ContentScreenModel.ContentListItem(it) }.toTypedArray()
        )
    }

    private fun checkCompleteButtonEnable() {
        _isCompleteButtonEnable.value = (_choiceCount.value ?: 0) >= MAX_CHOICE_COUNT
    }

    fun onCompleteButtonClick() {
        viewModelScope.launch(exceptionHandler) {
            val chosenList: List<OnBoardingChosenContentModel> =
                contentList.filter { it.isChosen }
                    .map { OnBoardingChosenContentModel(it.contentId, MAX_RATING) }

            val chosenContentsDeferred = async { mediaContentService.postChosenContents(chosenList) }
            val onBoardingCompletedDeferred = async { userService.postOnBoardingCompleted(user.userId) }

            val isSuccess = chosenContentsDeferred.await()
            onBoardingCompletedDeferred.await()

            if (isSuccess) {
                _completeButtonClickEvent.value = Event(Unit)
            }
        }
    }

    companion object {
        private const val MAX_CHOICE_COUNT = 5
        private const val MAX_RATING = 5f
    }
}
