package com.beside153.peopleinside.viewmodel.contentdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.model.contentdetail.ContentDetailModel
import com.beside153.peopleinside.model.contentdetail.ContentReviewModel
import com.beside153.peopleinside.service.ContentDetailService
import com.beside153.peopleinside.util.Event
import com.beside153.peopleinside.view.contentdetail.ContentDetailScreenAdapter.ContentDetailScreenModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ContentDetailViewModel(private val contentDetailService: ContentDetailService) : ViewModel() {
    private val _backButtonClickEvent = MutableLiveData<Event<Unit>>()
    val backButtonClickEvent: LiveData<Event<Unit>> get() = _backButtonClickEvent

    private val _contentDetailItem = MutableLiveData<ContentDetailModel>()
    val contentDetailItem: LiveData<ContentDetailModel> get() = _contentDetailItem

    private val reviewList = MutableLiveData<List<ContentReviewModel>>()

    private val _screenList = MutableLiveData<List<ContentDetailScreenModel>>()
    val screenList: LiveData<List<ContentDetailScreenModel>> get() = _screenList

    private val _scrollEvent = MutableLiveData<Event<Unit>>()
    val scrollEvent: LiveData<Event<Unit>> get() = _scrollEvent

    fun onBackButtonClick() {
        _backButtonClickEvent.value = Event(Unit)
    }

    fun initAllData(contentId: Int, didClickComment: Boolean) {
        // 로딩 및 ExceptionHandler 구현 필요

        viewModelScope.launch {
            val contentDetailItemDeferred = async { contentDetailService.getContentDetail(contentId) }
            val reviewListDeferred = async { contentDetailService.getContentReviewList(contentId, 1) }

            _contentDetailItem.value = contentDetailItemDeferred.await()
            reviewList.value = reviewListDeferred.await()

            @Suppress("SpreadOperator")
            _screenList.value = listOf(
                ContentDetailScreenModel.PosterView(_contentDetailItem.value!!),
                ContentDetailScreenModel.ReviewView,
                ContentDetailScreenModel.InfoView(_contentDetailItem.value!!),
                ContentDetailScreenModel.CommentsView,
                *reviewList.value?.map { ContentDetailScreenModel.ContentReviewItem(it) }?.toTypedArray()
                    ?: emptyArray()
            )
            if (didClickComment) _scrollEvent.value = Event(Unit)
        }
    }
}
