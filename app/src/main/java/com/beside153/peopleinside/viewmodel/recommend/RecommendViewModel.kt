package com.beside153.peopleinside.viewmodel.recommend

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.model.common.User
import com.beside153.peopleinside.model.mediacontent.Pick10Model
import com.beside153.peopleinside.model.mediacontent.RatingBattleModel
import com.beside153.peopleinside.model.mediacontent.SubRankingModel
import com.beside153.peopleinside.repository.UserRepository
import com.beside153.peopleinside.service.mediacontent.BookmarkService
import com.beside153.peopleinside.service.mediacontent.MediaContentService
import com.beside153.peopleinside.util.Event
import com.beside153.peopleinside.view.recommend.Pick10ViewPagerAdapter.Pick10ViewPagerModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecommendEvent {
    data class Pick10ItemClick(val item: Pick10Model) : RecommendEvent
    data class TopReviewClick(val item: Pick10Model) : RecommendEvent
    data class BattleItemClick(val item: RatingBattleModel) : RecommendEvent
    data class BattleItemCommentClick(val item: RatingBattleModel) : RecommendEvent
    data class SubRankingArrowClick(val mediaType: String) : RecommendEvent
    data class SubRankingItemClick(val item: SubRankingModel) : RecommendEvent
    object MbtiImgClick : RecommendEvent
    object RefreshPick10Click : RecommendEvent
    object GuestLogin : RecommendEvent
    data class GoToWriteReviewClick(val contentId: Int) : RecommendEvent
}

@HiltViewModel
class RecommendViewModel @Inject constructor(
    private val mediaContentService: MediaContentService,
    private val bookmarkService: BookmarkService,
    private val userRepository: UserRepository
) : BaseViewModel() {

    private val _progressBarVisible = MutableLiveData(true)
    val progressBarVisible: LiveData<Boolean> get() = _progressBarVisible

    private val _viewPagerList = MutableLiveData<List<Pick10ViewPagerModel>>()
    val viewPagerList: LiveData<List<Pick10ViewPagerModel>> get() = _viewPagerList

    private val _pick10ProgressBarVisible = MutableLiveData(false)
    val pick10ProgressBarVisible: LiveData<Boolean> get() = _pick10ProgressBarVisible

    private val _movieBattleItem = MutableLiveData<RatingBattleModel>()
    val movieBattleItem: LiveData<RatingBattleModel> get() = _movieBattleItem

    private val _tvBattleItem = MutableLiveData<RatingBattleModel>()
    val tvBattleItem: LiveData<RatingBattleModel> get() = _tvBattleItem

    private val _subRankingList = MutableLiveData<List<SubRankingModel>>()
    val subRankingList: LiveData<List<SubRankingModel>> get() = _subRankingList

    private val _subRankingProgressBarVisible = MutableLiveData(false)
    val subRankingProgressBarVisible: LiveData<Boolean> get() = _subRankingProgressBarVisible

    private val _selectedTab = MutableLiveData("all")
    val selectedTab: LiveData<String> get() = _selectedTab

    private val _recommendEvent = MutableLiveData<Event<RecommendEvent>>()
    val recommendEvent: LiveData<Event<RecommendEvent>> = _recommendEvent

    private var pick10PageCount = 1
    private var pick10List = listOf<Pick10Model>()
    private lateinit var user: User

    init {
        viewModelScope.launch(Dispatchers.Default) {
            userRepository.userFlow.collectLatest { user = it }
        }
    }

    fun initAllData() {
        viewModelScope.launch(exceptionHandler) {
            val pick10ListDeferred = async { mediaContentService.getPick10List(pick10PageCount) }
            val movieBattleItemDeferred = async { mediaContentService.getRatingBattleItem("movie") }
            val tvBattleItemDeferred = async { mediaContentService.getRatingBattleItem("tv") }
            val subrankingListDeferred = async { mediaContentService.getSubRankingItem("all", MAX_TAKE) }

            pick10List = pick10ListDeferred.await()
            _movieBattleItem.value = movieBattleItemDeferred.await()
            _tvBattleItem.value = tvBattleItemDeferred.await()
            _subRankingList.value = subrankingListDeferred.await()

            val updatedSubRankingList = _subRankingList.value?.mapIndexed { index, item ->
                item.copy(rank = index + 1)
            }
            _subRankingList.value = updatedSubRankingList ?: emptyList()

            _viewPagerList.value = viewPagerList()
            _progressBarVisible.value = false
        }
    }

    fun onMbtiImgClick() {
        if (!user.isMember) {
            _recommendEvent.value = Event(RecommendEvent.GuestLogin)
            return
        }
        _recommendEvent.value = Event(RecommendEvent.MbtiImgClick)
    }

    fun refreshPick10List() {
        viewModelScope.launch(exceptionHandler) {
            _pick10ProgressBarVisible.value = true
            pick10List = mediaContentService.getPick10List(++pick10PageCount)
            _viewPagerList.value = viewPagerList()
            _recommendEvent.value = Event(RecommendEvent.RefreshPick10Click)
            Handler(Looper.getMainLooper()).postDelayed({
                _pick10ProgressBarVisible.value = false
            }, REFRESH_TIME)
        }
    }

    fun onBookmarkClick(item: Pick10Model) {
        if (!user.isMember) {
            _recommendEvent.value = Event(RecommendEvent.GuestLogin)
            return
        }

        viewModelScope.launch(exceptionHandler) {
            bookmarkService.postBookmarkStatus(item.contentId)

            val updatedList = pick10List.map {
                if (item == it) {
                    if (it.bookmarked) {
                        it.copy(bookmarked = false)
                    } else {
                        it.copy(bookmarked = true)
                    }
                } else {
                    it
                }
            }

            pick10List = updatedList
            _viewPagerList.value = viewPagerList()
        }
    }

    private fun viewPagerList(): List<Pick10ViewPagerModel> {
        return listOf(
            *pick10List.map { Pick10ViewPagerModel.Pick10Item(it) }.toTypedArray(),
            Pick10ViewPagerModel.RefreshView
        )
    }

    fun onGoToWriteReviewClick(item: Pick10Model) {
        if (!user.isMember) {
            _recommendEvent.value = Event(RecommendEvent.GuestLogin)
            return
        }

        _recommendEvent.value = Event(RecommendEvent.GoToWriteReviewClick(item.contentId))
    }

    fun onPick10ItemClick(item: Pick10Model) {
        _recommendEvent.value = Event(RecommendEvent.Pick10ItemClick(item))
    }

    fun onTopReviewClick(item: Pick10Model) {
        _recommendEvent.value = Event(RecommendEvent.TopReviewClick(item))
    }

    fun onBattleItemClick(item: RatingBattleModel) {
        _recommendEvent.value = Event(RecommendEvent.BattleItemClick(item))
    }

    fun onBattleCommentClick(item: RatingBattleModel) {
        _recommendEvent.value = Event(RecommendEvent.BattleItemCommentClick(item))
    }

    fun onSubRankingArrowClick() {
        _recommendEvent.value = Event(RecommendEvent.SubRankingArrowClick(_selectedTab.value ?: "all"))
    }

    fun onSubRankingItemClick(item: SubRankingModel) {
        _recommendEvent.value = Event(RecommendEvent.SubRankingItemClick(item))
    }

    fun setSelectedTab(tab: String) {
        _selectedTab.value = tab
        onSubRankingTabClick()
    }

    private fun onSubRankingTabClick() {
        _subRankingProgressBarVisible.value = true

        viewModelScope.launch(exceptionHandler) {
            val subrankingListDeferred =
                async { mediaContentService.getSubRankingItem(_selectedTab.value ?: "all", MAX_TAKE) }
            _subRankingList.value = subrankingListDeferred.await()

            val updatedList = _subRankingList.value?.mapIndexed { index, item ->
                item.copy(rank = index + 1)
            }
            _subRankingList.value = updatedList ?: emptyList()

            Handler(Looper.getMainLooper()).postDelayed({
                _subRankingProgressBarVisible.value = false
            }, SUB_RANKING_REFRESH_TIME)
        }
    }

    companion object {
        private const val MAX_TAKE = 3
        private const val REFRESH_TIME = 2000L
        private const val SUB_RANKING_REFRESH_TIME = 1000L
    }
}
