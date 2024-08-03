package com.beside153.peopleinside.viewmodel.contentdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.common.exception.ApiException
import com.beside153.peopleinside.model.common.User
import com.beside153.peopleinside.model.mediacontent.ContentDetailModel
import com.beside153.peopleinside.model.mediacontent.rating.ContentRatingModel
import com.beside153.peopleinside.model.mediacontent.rating.ContentRatingRequest
import com.beside153.peopleinside.model.mediacontent.review.ContentCommentModel
import com.beside153.peopleinside.model.mediacontent.review.ContentReviewModel
import com.beside153.peopleinside.repository.UserRepository
import com.beside153.peopleinside.service.mediacontent.BookmarkService
import com.beside153.peopleinside.service.mediacontent.MediaContentService
import com.beside153.peopleinside.service.mediacontent.RatingService
import com.beside153.peopleinside.service.mediacontent.ReviewService
import com.beside153.peopleinside.util.Event
import com.beside153.peopleinside.util.roundToHalf
import com.beside153.peopleinside.view.contentdetail.ContentDetailScreenAdapter.ContentDetailScreenModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ContentDetailEvent {
    object Scroll : ContentDetailEvent
    object VerticalDotsClick : ContentDetailEvent
    data class CreateReview(val contentId: Int, val content: String) : ContentDetailEvent
    data class ReportSuccess(val isSuccess: Boolean) : ContentDetailEvent
    data class CreateRating(val item: ContentRatingModel, val user: User) : ContentDetailEvent
    object GuestLogin : ContentDetailEvent
}

@HiltViewModel
class ContentDetailViewModel @Inject constructor(
    private val mediaContentService: MediaContentService,
    private val ratingService: RatingService,
    private val reviewService: ReviewService,
    private val bookmarkService: BookmarkService,
    private val userRepository: UserRepository
) : BaseViewModel() {

    private val _contentDetailItem = MutableLiveData<ContentDetailModel>()
    val contentDetailItem: LiveData<ContentDetailModel> get() = _contentDetailItem

    private val _screenList = MutableLiveData<List<ContentDetailScreenModel>>()
    val screenList: LiveData<List<ContentDetailScreenModel>> get() = _screenList

    private val _contentDetailEvent = MutableLiveData<Event<ContentDetailEvent>>()
    val contentDetailEvent: LiveData<Event<ContentDetailEvent>> = _contentDetailEvent

    private var contentId = 0
    private var currentRating = 0f
    private var currentRatingId = 0
    private var page = 1
    private var writerHasReview = false
    private var bookmarked = false
    private var commentIdForReport = 0
    private var contentRatingItem = ContentRatingModel(1, 0, 0f)
    private var writerReviewItem = ContentReviewModel(0, 0, "", 0, null)
    private var commentList = listOf<ContentCommentModel>()

    private lateinit var user: User

    init {
        viewModelScope.launch(Dispatchers.Default) {
            userRepository.userFlow.collectLatest { user = it }
        }
    }

    fun setContentId(id: Int) {
        contentId = id
    }

    fun loadMoreCommentList() {
        viewModelScope.launch(exceptionHandler) {
            val newCommentList = reviewService.getContentReviewList(contentId, ++page)
            commentList = commentList.plus(newCommentList)

            _screenList.value = _screenList.value?.plus(
                listOf(
                    *newCommentList.map { ContentDetailScreenModel.ContentCommentItem(it) }.toTypedArray()
                )
            )
        }
    }

    fun onVerticalDotsClick(item: ContentCommentModel) {
        if (!user.isMember) {
            _contentDetailEvent.value = Event(ContentDetailEvent.GuestLogin)
            return
        }
        _contentDetailEvent.value = Event(ContentDetailEvent.VerticalDotsClick)
        commentIdForReport = item.reviewId
    }

    fun onCommentLikeClick(item: ContentCommentModel) {
        if (!user.isMember) {
            _contentDetailEvent.value = Event(ContentDetailEvent.GuestLogin)
            return
        }

        viewModelScope.launch(exceptionHandler) {
            val updatedList: List<ContentCommentModel>?
            if (item.like) {
                updatedList = commentList.map {
                    if (item == it) {
                        it.copy(like = false, likeCount = it.likeCount - 1)
                    } else {
                        it
                    }
                }
            } else {
                updatedList = commentList.map {
                    if (item == it) {
                        it.copy(like = true, likeCount = it.likeCount + 1)
                    } else {
                        it
                    }
                }
            }
            commentList = updatedList
            _screenList.value = screenList()

            reviewService.postLikeToggle(contentId, item.reviewId)
        }
    }

    fun reportComment(reportId: Int) {
        val ceh = CoroutineExceptionHandler { context, t ->
            when (t) {
                is ApiException -> {
                    if (t.error.statusCode == 400) {
                        _contentDetailEvent.value = Event(ContentDetailEvent.ReportSuccess(false))
                    } else {
                        exceptionHandler.handleException(context, t)
                    }
                }

                else -> exceptionHandler.handleException(context, t)
            }
        }

        viewModelScope.launch(ceh) {
            reviewService.postReport(contentId, commentIdForReport, reportId)
            _contentDetailEvent.value = Event(ContentDetailEvent.ReportSuccess(true))
        }
    }

    fun initAllData(didClickComment: Boolean) {
        page = 1
        viewModelScope.launch(exceptionHandler) {
            initRating()
            initWriterReview()
            val contentDetailItemDeferred = async { mediaContentService.getContentDetail(contentId) }
            val bookmarkStatusDeferred = async { bookmarkService.getBookmarkStatus(contentId) }
            val commentListDeferred = async { reviewService.getContentReviewList(contentId, page) }
            val postViewLogDeferred = async { mediaContentService.postViewLog(contentId, ENTER) }

            _contentDetailItem.value = contentDetailItemDeferred.await()
            bookmarked = bookmarkStatusDeferred.await()
            commentList = commentListDeferred.await()
            postViewLogDeferred.await()

            _screenList.value = screenList()
            if (didClickComment) _contentDetailEvent.value = Event(ContentDetailEvent.Scroll)
        }
    }

    fun postViewLogStay() {
        viewModelScope.launch(exceptionHandler) {
            mediaContentService.postViewLog(contentId, STAY)
        }
    }

    private suspend fun initWriterReview() {
        val ceh = CoroutineExceptionHandler { context, t ->
            when (t) {
                is ApiException -> {
                    if (t.error.statusCode == 404) {
                        writerHasReview = false
                    } else {
                        exceptionHandler.handleException(context, t)
                    }
                }

                else -> exceptionHandler.handleException(context, t)
            }
        }
        viewModelScope.launch(ceh) {
            writerReviewItem = reviewService.getWriterReview(contentId, user.userId)
            writerHasReview = true
        }
    }

    private suspend fun initRating() {
        val ceh = CoroutineExceptionHandler { context, t ->
            when (t) {
                is ApiException -> {
                    if (t.error.statusCode == 404) {
                        contentRatingItem = ContentRatingModel(contentId, 0, 0f)
                        currentRatingId = 0
                        currentRating = 0f
                    } else {
                        exceptionHandler.handleException(context, t)
                    }
                }

                else -> exceptionHandler.handleException(context, t)
            }
        }
        viewModelScope.launch(ceh) {
            contentRatingItem = ratingService.getContentRating(contentId, user.userId)
            currentRating = contentRatingItem.rating
            currentRatingId = contentRatingItem.ratingId
        }
    }

    fun onRatingChanged(rating: Float) {
        viewModelScope.launch(exceptionHandler) {
            if (currentRating.roundToHalf() == rating) return@launch

            if (currentRating <= 0) {
                contentRatingItem =
                    ratingService.postContentRating(contentId, ContentRatingRequest(rating))
                currentRating = rating
                currentRatingId = contentRatingItem.ratingId
                _contentDetailEvent.value = Event(ContentDetailEvent.CreateRating(contentRatingItem, user))
                return@launch
            }
            val currentRatingHasValue = 0 < currentRating && currentRating <= MAX_RATING
            if (currentRatingHasValue && (0 < rating && rating <= MAX_RATING)) {
                ratingService.putContentRating(contentId, ContentRatingRequest(rating))
                currentRating = rating
                return@launch
            }
            if (currentRatingHasValue && rating == 0f) {
                ratingService.deleteContentRating(contentId, currentRatingId)
                currentRating = rating
            }
        }
    }

    fun onBookmarkClick() {
        if (!user.isMember) {
            _contentDetailEvent.value = Event(ContentDetailEvent.GuestLogin)
            return
        }
        bookmarked = bookmarked == false
        viewModelScope.launch(exceptionHandler) {
            initRating()
            bookmarkService.postBookmarkStatus(contentId)
            _screenList.value = screenList()
        }
    }

    private fun screenList(): List<ContentDetailScreenModel> {
        val commentAreaList = if (commentList.isEmpty()) {
            listOf(ContentDetailScreenModel.NoCommentView)
        } else {
            listOf(
                *commentList.map { ContentDetailScreenModel.ContentCommentItem(it) }.toTypedArray()
            )
        }

        return listOf(
            ContentDetailScreenModel.PosterView(_contentDetailItem.value!!),
            ContentDetailScreenModel.ReviewView(contentRatingItem, bookmarked, writerHasReview),
            ContentDetailScreenModel.InfoView(_contentDetailItem.value!!),
            ContentDetailScreenModel.CommentsView
        ) + commentAreaList
    }

    fun onCreateReviewClick() {
        if (!user.isMember) {
            _contentDetailEvent.value = Event(ContentDetailEvent.GuestLogin)
            return
        }
        if (!writerHasReview) {
            _contentDetailEvent.value = Event(ContentDetailEvent.CreateReview(contentId, ""))
        } else {
            _contentDetailEvent.value = Event(ContentDetailEvent.CreateReview(contentId, writerReviewItem.content))
        }
    }

    companion object {
        private const val MAX_RATING = 5
        private const val ENTER = "enter"
        private const val STAY = "stay"
    }
}
