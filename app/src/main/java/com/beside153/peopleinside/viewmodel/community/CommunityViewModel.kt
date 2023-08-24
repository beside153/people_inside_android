package com.beside153.peopleinside.viewmodel.community

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.model.common.User
import com.beside153.peopleinside.model.community.post.CommunityPostModel
import com.beside153.peopleinside.repository.UserRepository
import com.beside153.peopleinside.service.community.CommunityPostService
import com.beside153.peopleinside.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CommunityEvent {
    object SearchBarClick : CommunityEvent
    data class WritePostClick(val isMember: Boolean) : CommunityEvent
    data class PostItemClick(val postId: Long) : CommunityEvent
}

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityPostService: CommunityPostService,
    private val userRepository: UserRepository
) : BaseViewModel() {
    private val _postList = MutableLiveData<List<CommunityPostModel>>()
    val postList: LiveData<List<CommunityPostModel>> get() = _postList

    private val _communityEvent = MutableLiveData<Event<CommunityEvent>>()
    val communityEvent: LiveData<Event<CommunityEvent>> get() = _communityEvent

    private var page = 1
    private lateinit var user: User

    init {
        viewModelScope.launch(Dispatchers.Default) {
            userRepository.userFlow.collectLatest { user = it }
        }
    }

    fun initPostList() {
        var allPostList = listOf<CommunityPostModel>()
        viewModelScope.launch(exceptionHandler) {
            (1..page).forEach {
                val tempList = communityPostService.getCommunityPostList(it)
                val userMbti = user.mbti.lowercase()

                val updatedList = tempList.map { item ->
                    if (item.mbtiList.contains(userMbti)) {
                        val mutableMbtiList = item.mbtiList.toMutableList()
                        mutableMbtiList.remove(userMbti)
                        mutableMbtiList.add(0, userMbti)
                        item.copy(mbtiList = mutableMbtiList.toList())
                    } else {
                        item
                    }
                }

                allPostList = allPostList.plus(updatedList)
            }
            _postList.value = allPostList
        }
    }

    fun loadMorePostList() {
        viewModelScope.launch(exceptionHandler) {
            val newPostList = communityPostService.getCommunityPostList(++page)
            _postList.value = _postList.value?.plus(newPostList)
        }
    }

    fun onCommunitySearchBarClick() {
        _communityEvent.value = Event(CommunityEvent.SearchBarClick)
    }

    fun onWritePostClick() {
        _communityEvent.value = Event(CommunityEvent.WritePostClick(user.isMember))
    }

    fun onPostItemClick(item: CommunityPostModel) {
        _communityEvent.value = Event(CommunityEvent.PostItemClick(item.postId))
    }
}
