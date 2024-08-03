package com.beside153.peopleinside.viewmodel.community

import android.text.Editable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.model.community.post.CommunityPostModel
import com.beside153.peopleinside.service.community.CommunityPostService
import com.beside153.peopleinside.util.Event
import com.beside153.peopleinside.util.PreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunitySearchViewModel @Inject constructor(
    private val communityPostService: CommunityPostService,
    private val prefs: PreferenceUtil
) : BaseViewModel() {

    private val _keyword = MutableLiveData("")
    val keyword: LiveData<String> get() = _keyword

    private val _postItemClickEvent = MutableLiveData<Event<Long>>()
    val postItemClickEvent: LiveData<Event<Long>> get() = _postItemClickEvent

    private val _searchedPostList = MutableLiveData<List<CommunityPostModel>>(listOf())
    val searchedPostList: LiveData<List<CommunityPostModel>> get() = _searchedPostList

    private val _isSearched = MutableLiveData(false)
    val isSearched: LiveData<Boolean> get() = _isSearched

    private val _noResult = MutableLiveData(false)
    val noResult: LiveData<Boolean> get() = _noResult

    private val _searchWordList = MutableLiveData<List<String>>(listOf())
    val searchWordList: LiveData<List<String>> get() = _searchWordList

    private val _hasSearchWord = MutableLiveData(false)
    val hasSearchWord: LiveData<Boolean> get() = _hasSearchWord

    private var page = 1

    fun initSearchWordList() {
        _searchWordList.value = prefs.getRecentSearchList().toMutableList()
        checkHasSearchWord()
    }

    private fun checkHasSearchWord() {
        _hasSearchWord.value = _searchWordList.value?.isNotEmpty() ?: false
    }

    fun searchPostAction() {
        page = 1
        _noResult.value = false
        val searchedWord = _keyword.value ?: ""
        if (searchedWord.isEmpty()) return

        viewModelScope.launch(exceptionHandler) {
            _searchedPostList.value = communityPostService.getCommunityPostList(page, searchedWord)
            _isSearched.value = true

            if (_searchedPostList.value.isNullOrEmpty()) {
                _noResult.value = true
            }
            addSearchWord(searchedWord)
        }
    }

    fun loadMorePostList() {
        viewModelScope.launch(exceptionHandler) {
            val newPostList = communityPostService.getCommunityPostList(++page)
            _searchedPostList.value = _searchedPostList.value?.plus(newPostList)
        }
    }

    private fun addSearchWord(word: String) {
        val tempList = _searchWordList.value?.toMutableList() ?: mutableListOf()
        if (tempList.size >= word_max_size) {
            tempList.removeAt(tempList.size - 1)
        }
        tempList.add(0, word)
        prefs.setRecentSearchList(tempList.toList())
        _searchWordList.value = tempList
        checkHasSearchWord()
    }

    fun deleteAllSearchWord() {
        prefs.setRecentSearchList(listOf())
        _searchWordList.value = listOf()
        checkHasSearchWord()
    }

    fun onPostItemClick(item: CommunityPostModel) {
        _postItemClickEvent.value = Event(item.postId)
    }

    fun afterKeywordTextChanged(editable: Editable?) {
        _keyword.value = editable.toString()
        if (editable.isNullOrEmpty()) {
            _isSearched.value = false
            _noResult.value = false
        }
    }

    fun onSearchCancelClick() {
        _keyword.value = ""
        _isSearched.value = false
    }

    companion object {
        private const val word_max_size = 20
    }
}
