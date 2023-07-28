package com.beside153.peopleinside.viewmodel.search

import android.text.Editable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.model.mediacontent.SearchHotModel
import com.beside153.peopleinside.model.mediacontent.SearchedContentModel
import com.beside153.peopleinside.model.mediacontent.SearchingTitleModel
import com.beside153.peopleinside.model.mediacontent.ViewLogContentModel
import com.beside153.peopleinside.service.RetrofitClient
import com.beside153.peopleinside.service.mediacontent.MediaContentService
import com.beside153.peopleinside.util.Event
import com.beside153.peopleinside.view.search.SearchScreenAdapter.SearchScreenModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

interface SearchViewModelHandler {
    val viewLogList: LiveData<List<ViewLogContentModel>>
}

class SearchViewModel(private val mediaContentService: MediaContentService) : BaseViewModel(), SearchViewModelHandler {

    private val _keyword = MutableLiveData("")
    val keyword: LiveData<String> get() = _keyword

    private val _viewLogList = MutableLiveData<List<ViewLogContentModel>>()
    override val viewLogList: LiveData<List<ViewLogContentModel>> get() = _viewLogList

    private val searchingTitleList = MutableLiveData<List<SearchingTitleModel>>()
    private val searchedContentList = MutableLiveData<List<SearchedContentModel>>()
    private val searchHotList = MutableLiveData<List<SearchHotModel>>()

    private val _screenList = MutableLiveData<List<SearchScreenModel>>()
    val screenList: LiveData<List<SearchScreenModel>> get() = _screenList

    private val _searchCompleteEvent = MutableLiveData<Event<Unit>>()
    val searchCompleteEvent: LiveData<Event<Unit>> get() = _searchCompleteEvent

    private var isSearching = false
    private var page = 1

    fun afterKeywordTextChanged(editable: Editable?) {
        _keyword.value = editable.toString()
        loadSearchingTitle()
    }

    fun onSearchCancelClick() {
        _keyword.value = ""
        initSearchScreen()
    }

    @Suppress("SpreadOperator")
    fun initSearchScreen() {
        viewModelScope.launch(exceptionHandler) {
            val viwLogListDeferred = async { mediaContentService.getViewLogList() }
            val searchHotListDeferred = async { mediaContentService.getHotContentList() }

            _viewLogList.value = viwLogListDeferred.await()
            searchHotList.value = searchHotListDeferred.await()

            val updatedList = searchHotList.value?.mapIndexed { index, item ->
                item.copy(rank = index + 1)
            }
            searchHotList.value = updatedList ?: emptyList()

            if ((_viewLogList.value ?: emptyList()).isEmpty()) {
                _screenList.value = listOf(
                    SearchScreenModel.NoViewLogView,
                    SearchScreenModel.HotView,
                    *searchHotList.value?.map { SearchScreenModel.SearchHotItem(it) }?.toTypedArray() ?: emptyArray()
                )
                return@launch
            }

            _screenList.value = listOf(
                SearchScreenModel.SeenView,
                SearchScreenModel.HotView,
                *searchHotList.value?.map { SearchScreenModel.SearchHotItem(it) }?.toTypedArray() ?: emptyArray()
            )
        }
    }

    private fun loadSearchingTitle() {
        if (isSearching) {
            isSearching = false
            return
        }

        viewModelScope.launch(exceptionHandler) {
            if (_keyword.value?.isNotEmpty() == true) {
                searchingTitleList.value = mediaContentService.getSearchingTitleList(_keyword.value ?: "")
                changeScreenWhenSearching()
                return@launch
            }
            initSearchScreen()
        }
    }

    fun searchContentAction() {
        page = 1

        val exceptionHandler = CoroutineExceptionHandler { _, _ ->
            changeScreenWhenNoResult()
        }

        viewModelScope.launch(exceptionHandler) {
            if (_keyword.value?.isNotEmpty() == true) {
                searchedContentList.value = mediaContentService.getSearchedContentList(_keyword.value ?: "", page)
                if ((searchedContentList.value ?: emptyList()).isEmpty()) {
                    changeScreenWhenNoResult()
                    return@launch
                }
                changeScreenWhenSearchedContent()
            }
        }
    }

    fun loadMoreContentList() {
        viewModelScope.launch(exceptionHandler) {
            if (_keyword.value?.isNotEmpty() == true) {
                val newContentList = mediaContentService.getSearchedContentList(_keyword.value ?: "", ++page)
                searchedContentList.value = searchedContentList.value?.plus(newContentList)

                changeScreenWhenSearchedContent()
            }
        }
    }

    @Suppress("SpreadOperator")
    private fun changeScreenWhenSearching() {
        _screenList.value = listOf(
            *searchingTitleList.value?.map { SearchScreenModel.SearchingTitleItem(it) }?.toTypedArray()
                ?: emptyArray()
        )
    }

    @Suppress("SpreadOperator")
    private fun changeScreenWhenSearchedContent() {
        _screenList.value = listOf(
            *searchedContentList.value?.map { SearchScreenModel.SearchedContentItem(it) }?.toTypedArray()
                ?: emptyArray()
        )
        _searchCompleteEvent.value = Event(Unit)
    }

    private fun changeScreenWhenNoResult() {
        _screenList.value = listOf(SearchScreenModel.NoResultView)
    }

    fun onSearchingTitleItemClick(item: SearchingTitleModel) {
        isSearching = true
        _keyword.value = item.title
        searchContentAction()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val mediaContentService = RetrofitClient.mediaContentService
                return SearchViewModel(mediaContentService) as T
            }
        }
    }
}
