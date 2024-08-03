package com.beside153.peopleinside.view.contentdetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.beside153.peopleinside.R
import com.beside153.peopleinside.base.BaseActivity
import com.beside153.peopleinside.common.extension.eventObserve
import com.beside153.peopleinside.databinding.ActivityContentDetailBinding
import com.beside153.peopleinside.model.mediacontent.review.ContentCommentModel
import com.beside153.peopleinside.util.addBackPressedAnimation
import com.beside153.peopleinside.util.setCloseActivityAnimation
import com.beside153.peopleinside.util.setOpenActivityAnimation
import com.beside153.peopleinside.util.showToast
import com.beside153.peopleinside.view.common.BottomSheetFragment
import com.beside153.peopleinside.view.common.BottomSheetType
import com.beside153.peopleinside.view.login.nonmember.NonMemberLoginActivity
import com.beside153.peopleinside.viewmodel.contentdetail.ContentDetailEvent
import com.beside153.peopleinside.viewmodel.contentdetail.ContentDetailViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContentDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityContentDetailBinding
    private val contentDetailViewModel: ContentDetailViewModel by viewModels()
    private val contentDetailScreenAdapter =
        ContentDetailScreenAdapter(
            ::onBookmarkClick,
            ::onCreateReviewClick,
            ::goToNonMemberLoginAcitivity,
            ::onRatingChanged,
            ::onVerticalDotsClick,
            ::onCommentLikeClick
        )
    private val bottomSheet = BottomSheetFragment(BottomSheetType.ReviewReport)
    private var reportId = 0
    private var didClickComment = false
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_content_detail)

        firebaseAnalytics = Firebase.analytics

        binding.apply {
            viewModel = contentDetailViewModel
            lifecycleOwner = this@ContentDetailActivity
        }

        addBackPressedAnimation { setResult(RESULT_OK) }

        val contentId = intent.getIntExtra(CONTENT_ID, 1)
        contentDetailViewModel.setContentId(contentId)
        didClickComment = intent.getBooleanExtra(DID_CLICK_COMMENT, false)
        contentDetailViewModel.initAllData(didClickComment)

        binding.contentDetailRecyclerView.apply {
            adapter = contentDetailScreenAdapter
            layoutManager = LinearLayoutManager(this@ContentDetailActivity)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val lastVisibleItemPosition =
                        (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                    val itemTotalCount = recyclerView.adapter!!.itemCount - 1

                    if (!recyclerView.canScrollVertically(1) && lastVisibleItemPosition == itemTotalCount) {
                        contentDetailViewModel.loadMoreCommentList()
                    }
                }
            })
        }

        supportFragmentManager.setFragmentResultListener(
            BottomSheetType.ReviewReport.name,
            this
        ) { _, bundle ->
            reportId = bundle.getInt(BottomSheetType.ReviewReport.name)
            contentDetailViewModel.reportComment(reportId)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            contentDetailViewModel.postViewLogStay()
        }, STAY_TIME)

        initObserver()
    }

    private fun initObserver() {
        contentDetailViewModel.backButtonClickEvent.eventObserve(this) {
            setResult(RESULT_OK)
            finish()
            setCloseActivityAnimation()
        }

        contentDetailViewModel.error.eventObserve(this) {
            showErrorDialog(it) { contentDetailViewModel.initAllData(didClickComment) }
        }

        contentDetailViewModel.screenList.observe(this) { screenList ->
            contentDetailScreenAdapter.submitList(screenList)
        }

        contentDetailViewModel.contentDetailEvent.eventObserve(this) {
            when (it) {
                ContentDetailEvent.Scroll -> {
                    val smoothScroller = object : LinearSmoothScroller(this) {
                        override fun getVerticalSnapPreference(): Int = SNAP_TO_START
                    }
                    smoothScroller.targetPosition = POSITION_COMMENT_LIST
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.contentDetailRecyclerView.layoutManager?.startSmoothScroll(smoothScroller)
                    }, SCROLL_DURATION)
                }

                ContentDetailEvent.VerticalDotsClick -> {
                    bottomSheet.show(supportFragmentManager, bottomSheet.tag)
                }

                is ContentDetailEvent.CreateReview -> {
                    createReviewActivityLauncher.launch(CreateReviewActivity.newIntent(this, it.contentId, it.content))
                    setOpenActivityAnimation()
                }

                is ContentDetailEvent.ReportSuccess -> {
                    if (it.isSuccess) {
                        showToast(R.string.report_success)
                        return@eventObserve
                    }
                    showToast(R.string.report_failed)
                }

                is ContentDetailEvent.CreateRating -> {
                    firebaseAnalytics.logEvent("평가작성") {
                        param("유저_ID", it.user.userId.toString())
                        param("유저_MBTI", it.user.mbti)
                        param("콘텐츠_ID", it.item.contentId.toString())
                        param("별점", it.item.rating.toString())
                    }
                }

                ContentDetailEvent.GuestLogin -> {
                    goToNonMemberLoginAcitivity()
                }
            }
        }
    }

    private fun goToNonMemberLoginAcitivity() {
        startActivity(NonMemberLoginActivity.newIntent(this))
        setOpenActivityAnimation()
    }

    private fun onRatingChanged(rating: Float) {
        contentDetailViewModel.onRatingChanged(rating)
    }

    private fun onBookmarkClick() {
        contentDetailViewModel.onBookmarkClick()
    }

    private fun onCreateReviewClick() {
        contentDetailViewModel.onCreateReviewClick()
    }

    private fun onVerticalDotsClick(item: ContentCommentModel) {
        contentDetailViewModel.onVerticalDotsClick(item)
    }

    private fun onCommentLikeClick(item: ContentCommentModel) {
        contentDetailViewModel.onCommentLikeClick(item)
    }

    private val createReviewActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                contentDetailViewModel.initAllData(false)
            }
        }

    companion object {
        private const val DID_CLICK_COMMENT = "DID_CLICK_COMMENT"
        private const val CONTENT_ID = "CONTENT_ID"
        private const val POSITION_COMMENT_LIST = 3
        private const val STAY_TIME = 3000L
        private const val SCROLL_DURATION = 300L

        fun newIntent(context: Context, didClickComment: Boolean, contentId: Int): Intent {
            val intent = Intent(context, ContentDetailActivity::class.java)
            intent.putExtra(DID_CLICK_COMMENT, didClickComment)
            intent.putExtra(CONTENT_ID, contentId)
            return intent
        }
    }
}
