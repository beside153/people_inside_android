package com.beside153.peopleinside.viewmodel.community

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.model.common.CreateContentRequest
import com.beside153.peopleinside.model.common.User
import com.beside153.peopleinside.model.community.comment.CommunityCommentModel
import com.beside153.peopleinside.model.community.post.CommunityPostModel
import com.beside153.peopleinside.repository.UserRepository
import com.beside153.peopleinside.service.community.CommunityCommentService
import com.beside153.peopleinside.service.community.CommunityPostService
import com.beside153.peopleinside.util.Event
import com.beside153.peopleinside.view.community.PostDetailScreenAdapter.PostDetailScreenModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

interface PostDetailViewModelHandler {
    val postMbtiList: List<String>
    fun onCommentDotsClick(item: CommunityCommentModel)
}

data class CommentFixModel(
    val postId: Long,
    val commentId: Long,
    val commentContent: String
)

sealed interface PostDetailEvent {
    object CompleteUploadComment : PostDetailEvent
    object CompleteDeletePost : PostDetailEvent
    object CompleteDeleteComment : PostDetailEvent
    object CompleteReport : PostDetailEvent
    object GoToNonMemberLogin : PostDetailEvent
    data class PostDotsClick(val isMyPost: Boolean) : PostDetailEvent
    data class CommentDotsClick(val isMyComment: Boolean) : PostDetailEvent
    data class CommentFixClick(val commentFixModel: CommentFixModel) : PostDetailEvent
}

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val communityPostService: CommunityPostService,
    private val communityCommentService: CommunityCommentService,
    private val userRepository: UserRepository
) : BaseViewModel(), PostDetailViewModelHandler {

    val commentText = MutableLiveData("")

    private val _screenList = MutableLiveData<List<PostDetailScreenModel>>()
    val screenList: MutableLiveData<List<PostDetailScreenModel>> get() = _screenList

    private val _uploadButtonVisible = MutableLiveData(false)
    val uploadButtonVisible: LiveData<Boolean> get() = _uploadButtonVisible

    private val _isUploadCommentEnabled = MutableLiveData(false)
    val isUploadCommentEnabled: LiveData<Boolean> get() = _isUploadCommentEnabled

    private val _postDetailEvent = MutableLiveData<Event<PostDetailEvent>>()
    val postDetailEvent: LiveData<Event<PostDetailEvent>> = _postDetailEvent

    private var postId = 1L
    private var postDetailItem: CommunityPostModel? = null
    private var commentList = listOf<CommunityCommentModel>()
    private var page = 1
    private var selectedCommentId = 0L
    private var selectedCommentContent = ""

    override var postMbtiList = listOf<String>()

    private lateinit var user: User

    init {
        viewModelScope.launch(Dispatchers.Default) {
            userRepository.userFlow.collectLatest { user = it }
        }
    }

    fun setPostId(id: Long) {
        postId = id
    }

    fun initAllData() {
        viewModelScope.launch(exceptionHandler) {
            postDetailItem = communityPostService.getCommunityPostDetail(postId)
            commentList = listOf()
            (1..page).forEach {
                val newCommentList = communityCommentService.getCommunityCommentList(postId, it)
                commentList = commentList.plus(newCommentList)
            }
            postMbtiList = postDetailItem?.mbtiList ?: listOf()
            _screenList.value = screenList()
        }
    }

    fun loadMoreCommentList() {
        viewModelScope.launch(exceptionHandler) {
            val newCommentList = communityCommentService.getCommunityCommentList(postId, ++page)
            commentList = commentList.plus(newCommentList)
            _screenList.value = screenList()
        }
    }

    private fun screenList(): List<PostDetailScreenModel> {
        val commentAreaList = if ((postDetailItem?.totalComment ?: 0) <= 0) {
            listOf(PostDetailScreenModel.NoCommentView)
        } else {
            listOf(*commentList.map { PostDetailScreenModel.CommentItem(it) }.toTypedArray())
        }

        return listOf(PostDetailScreenModel.PostItem(postDetailItem!!)) + commentAreaList
    }

    fun onCommentTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        commentText.value = (s ?: "").toString()
        checkUploadCommentEnable()
    }

    private fun checkUploadCommentEnable() {
        _isUploadCommentEnabled.value = commentText.value?.isNotEmpty() ?: false
    }

    fun setUploadButtonVisible(isVisible: Boolean) {
        _uploadButtonVisible.value = isVisible
    }

    fun onUploadCommentButtonClick() {
        viewModelScope.launch(exceptionHandler) {
            communityCommentService.postCommunityComment(postId, CreateContentRequest(commentText.value ?: ""))
            commentText.value = ""
            _postDetailEvent.value = Event(PostDetailEvent.CompleteUploadComment)
        }
    }

    fun onPostVerticalDotsClick() {
        if (!user.isMember) {
            _postDetailEvent.value = Event(PostDetailEvent.GoToNonMemberLogin)
            return
        }
        if (user.userId.toLong() == (postDetailItem?.author?.userId ?: 1L)) {
            _postDetailEvent.value = Event(PostDetailEvent.PostDotsClick(true))
            return
        }
        _postDetailEvent.value = Event(PostDetailEvent.PostDotsClick(false))
    }

    override fun onCommentDotsClick(item: CommunityCommentModel) {
        if (!user.isMember) {
            _postDetailEvent.value = Event(PostDetailEvent.GoToNonMemberLogin)
            return
        }

        selectedCommentId = item.commentId
        selectedCommentContent = item.content
        if (user.userId.toLong() == item.author.userId) {
            _postDetailEvent.value = Event(PostDetailEvent.CommentDotsClick(true))
            return
        }
        _postDetailEvent.value = Event(PostDetailEvent.CommentDotsClick(false))
    }

    fun onCommentFixClick() {
        _postDetailEvent.value =
            Event(PostDetailEvent.CommentFixClick(CommentFixModel(postId, selectedCommentId, selectedCommentContent)))
    }

    fun deletePost() {
        viewModelScope.launch(exceptionHandler) {
            communityPostService.deleteCommunityPost(postId)
            _postDetailEvent.value = Event(PostDetailEvent.CompleteDeletePost)
        }
    }

    fun deleteComment() {
        viewModelScope.launch(exceptionHandler) {
            communityCommentService.deleteCommunityComment(postId, selectedCommentId)
            _postDetailEvent.value = Event(PostDetailEvent.CompleteDeleteComment)
            initAllData()
        }
    }

    fun reportPost(reportId: Int) {
        viewModelScope.launch(exceptionHandler) {
            communityPostService.postCommunityPostReport(postId, reportId)
            _postDetailEvent.value = Event(PostDetailEvent.CompleteReport)
        }
    }

    fun reportComment(reportId: Int) {
        viewModelScope.launch(exceptionHandler) {
            communityCommentService.postCommunityCommentReport(postId, selectedCommentId, reportId)
            _postDetailEvent.value = Event(PostDetailEvent.CompleteReport)
        }
    }

    fun onCommentEditTextFocused() {
        if (!user.isMember) {
            _postDetailEvent.value = Event(PostDetailEvent.GoToNonMemberLogin)
        }
    }
}
