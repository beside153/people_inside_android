package com.beside153.peopleinside.viewmodel.recommend

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beside153.peopleinside.base.BaseViewModel
import com.beside153.peopleinside.model.mediacontent.Pick10Model
import com.beside153.peopleinside.model.mediacontent.RatingBattleModel
import com.beside153.peopleinside.model.mediacontent.SubRankingModel
import com.beside153.peopleinside.service.mediacontent.BookmarkService
import com.beside153.peopleinside.service.mediacontent.MediaContentService
import com.beside153.peopleinside.util.Event
import com.beside153.peopleinside.view.recommend.Pick10ViewPagerAdapter.Pick10ViewPagerModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecommendEvent {
    data class Pick10Click(val item: Pick10Model) : RecommendEvent
    object TopReviewClick : RecommendEvent
}

@HiltViewModel
class RecommendViewModel @Inject constructor(
    private val mediaContentService: MediaContentService,
    private val bookmarkService: BookmarkService
) : BaseViewModel() {

    private val _progressBarVisible = MutableLiveData(true)
    val progressBarVisible: LiveData<Boolean> get() = _progressBarVisible

    private val _viewPagerList = MutableLiveData<List<Pick10ViewPagerModel>>()
    val viewPagerList: LiveData<List<Pick10ViewPagerModel>> get() = _viewPagerList

    private val _recommendEvent = MutableLiveData<Event<RecommendEvent>>()
    val recommendEvent: LiveData<Event<RecommendEvent>> = _recommendEvent

    private val _pick10ItemClickEvent = MutableLiveData<Event<Pick10Model>>()
    val pick10ItemClickEvent: LiveData<Event<Pick10Model>> get() = _pick10ItemClickEvent

    private val _topReviewClickEvent = MutableLiveData<Event<Pick10Model>>()
    val topReviewClickEvent: LiveData<Event<Pick10Model>> get() = _topReviewClickEvent

    private val _pick10ProgressBarVisible = MutableLiveData(false)
    val pick10ProgressBarVisible: LiveData<Boolean> get() = _pick10ProgressBarVisible

    private val _refreshPick10ClickEvent = MutableLiveData<Event<Unit>>()
    val refreshPick10ClickEvent: LiveData<Event<Unit>> get() = _refreshPick10ClickEvent

    private val _movieBattleItem = MutableLiveData<RatingBattleModel>()
    val movieBattleItem: LiveData<RatingBattleModel> get() = _movieBattleItem

    private val _tvBattleItem = MutableLiveData<RatingBattleModel>()
    val tvBattleItem: LiveData<RatingBattleModel> get() = _tvBattleItem

    private val _battleItemClickEvent = MutableLiveData<Event<RatingBattleModel>>()
    val battleItemClickEvent: LiveData<Event<RatingBattleModel>> get() = _battleItemClickEvent

    private val _battleItemCommentClickEvent = MutableLiveData<Event<RatingBattleModel>>()
    val battleItemCommentClickEvent: LiveData<Event<RatingBattleModel>> get() = _battleItemCommentClickEvent

    private val _subRankingList = MutableLiveData<List<SubRankingModel>>()
    val subRankingList: LiveData<List<SubRankingModel>> get() = _subRankingList

    private val _subRankingProgressBarVisible = MutableLiveData(false)
    val subRankingProgressBarVisible: LiveData<Boolean> get() = _subRankingProgressBarVisible

    private val _selectedTab = MutableLiveData("all")
    val selectedTab: LiveData<String> get() = _selectedTab

    private val _subRankingArrowClickEvent = MutableLiveData<Event<String>>()
    val subRankingArrowClickEvent: LiveData<Event<String>> get() = _subRankingArrowClickEvent

    private val _subRankingItemClickEvent = MutableLiveData<Event<SubRankingModel>>()
    val subRankingItemClickEvent: LiveData<Event<SubRankingModel>> get() = _subRankingItemClickEvent

    private val _mbtiImgClickEvent = MutableLiveData<Event<Unit>>()
    val mbtiImgClickEvent: LiveData<Event<Unit>> get() = _mbtiImgClickEvent

    private var pick10PageCount = 1
    private var pick10List = listOf<Pick10Model>()

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
        _mbtiImgClickEvent.value = Event(Unit)
    }

    fun refreshPick10List() {
        viewModelScope.launch(exceptionHandler) {
            _pick10ProgressBarVisible.value = true
            pick10List = mediaContentService.getPick10List(++pick10PageCount)
            _viewPagerList.value = viewPagerList()
            _refreshPick10ClickEvent.value = Event(Unit)
            Handler(Looper.getMainLooper()).postDelayed({
                _pick10ProgressBarVisible.value = false
            }, REFRESH_TIME)
        }
    }

    fun onBookmarkClick(item: Pick10Model) {
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

    fun onPick10ItemClick(item: Pick10Model) {
        _recommendEvent.value = Event(RecommendEvent.Pick10Click(item))
        // _pick10ItemClickEvent.value = Event(item)
    }

    fun onTopReviewClick(item: Pick10Model) {
        _topReviewClickEvent.value = Event(item)
    }

    fun onBattleItemClick(item: RatingBattleModel) {
        _battleItemClickEvent.value = Event(item)
    }

    fun onBattleCommentClick(item: RatingBattleModel) {
        _battleItemCommentClickEvent.value = Event(item)
    }

    fun onSubRankingArrowClick() {
        _subRankingArrowClickEvent.value = Event(_selectedTab.value ?: "all")
    }

    fun onSubRankingItemClick(item: SubRankingModel) {
        _subRankingItemClickEvent.value = Event(item)
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
