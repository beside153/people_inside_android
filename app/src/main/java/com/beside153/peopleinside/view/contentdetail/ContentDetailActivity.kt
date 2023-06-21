package com.beside153.peopleinside.view.contentdetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.beside153.peopleinside.R
import com.beside153.peopleinside.databinding.ActivityContentDetailBinding
import com.beside153.peopleinside.service.RetrofitClient
import com.beside153.peopleinside.util.EventObserver
import com.beside153.peopleinside.util.addBackPressedCallback
import com.beside153.peopleinside.util.setCloseActivityAnimation
import com.beside153.peopleinside.view.contentdetail.ContentDetailScreenAdapter.ContentDetailScreenModel
import com.beside153.peopleinside.viewmodel.contentdetail.ContentDetailViewModel

class ContentDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContentDetailBinding
    private val contentDetailScreenAdapter = ContentDetailScreenAdapter(::onCreateReviewClick)
    private val contentDetailViewModel: ContentDetailViewModel by viewModels(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ContentDetailViewModel(RetrofitClient.contentDetailService) as T
                }
            }
        }
    )

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_content_detail)

        binding.apply {
            viewModel = contentDetailViewModel
            lifecycleOwner = this@ContentDetailActivity
        }

        addBackPressedCallback()

        val contentId = intent.getIntExtra(CONTENT_ID, 0)
        contentDetailViewModel.loadContentDetail(contentId)

        contentDetailViewModel.backButtonClickEvent.observe(
            this,
            EventObserver {
                finish()
                setCloseActivityAnimation()
            }
        )

        binding.contentDetailRecyclerView.apply {
            adapter = contentDetailScreenAdapter
            layoutManager = LinearLayoutManager(this@ContentDetailActivity)
        }

        @Suppress("MagicNumber")
        val commentList = listOf(
            ContentDetailScreenModel.CommentItem(
                1,
                "ENTP / 미소님",
                "이 드라마는 도전적이고 흥미진진한 플롯이었어.이 드라마는 도전적이고 흥미진진한 플롯이었어.리뷰 최대 세 줄까지 노출됩니다다다다다"
            ),
            ContentDetailScreenModel.CommentItem(
                2,
                "ENTP / 미소님",
                "이 드라마는 도전적이고 흥미진진한 플롯이었어."
            ),
            ContentDetailScreenModel.CommentItem(
                3,
                "ENTP / 미소님",
                "이 드라마는 도전적이고 흥미진진한 플롯이었어.이 드라마는 도전적이고 흥미진진한 플롯이었어.리뷰"
            ),
            ContentDetailScreenModel.CommentItem(
                4,
                "ENTP / 미소님",
                "이 드라마는 도전적이고 흥미진진한 플롯이었어.이 드라마는 도전적이고 흥미진진한 플롯이었어.리뷰 최대 세 줄까지 노출됩니다다다다다"
            ),
            ContentDetailScreenModel.CommentItem(
                5,
                "ENTP / 미소님",
                "이 드라마는 도전적이고 흥미진진한 플롯이었어.이 드라마는 도전적이고 흥미진진한 플롯이었어.리뷰 최대 세 줄까지 노출됩니다다다다다"
            )
        )

        val list = mutableListOf(
            ContentDetailScreenModel.PosterView,
            ContentDetailScreenModel.ReviewView,
            ContentDetailScreenModel.InfoView,
            ContentDetailScreenModel.CommentsView
        )
        list += commentList
        contentDetailScreenAdapter.submitList(list)

        val didClickComment = intent.getBooleanExtra(DID_CLICK_COMMENT, false)
        if (didClickComment) {
            val smoothScroller = object : LinearSmoothScroller(this) {
                override fun getVerticalSnapPreference(): Int = SNAP_TO_START
            }
            smoothScroller.targetPosition = POSITION_COMMENT_LIST
            binding.contentDetailRecyclerView.layoutManager?.startSmoothScroll(smoothScroller)
        }
    }

    private fun onCreateReviewClick() {
        startActivity(CreateReviewActivity.newIntent(this))
    }

    companion object {
        private const val DID_CLICK_COMMENT = "DID_CLICK_COMMENT"
        private const val CONTENT_ID = "CONTENT_ID"
        private const val POSITION_COMMENT_LIST = 3

        fun newIntent(context: Context, didClickComment: Boolean, contentId: Int): Intent {
            val intent = Intent(context, ContentDetailActivity::class.java)
            intent.putExtra(DID_CLICK_COMMENT, didClickComment)
            intent.putExtra(CONTENT_ID, contentId)
            return intent
        }
    }
}
