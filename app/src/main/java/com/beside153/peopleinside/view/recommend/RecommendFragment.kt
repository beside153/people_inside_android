package com.beside153.peopleinside.view.recommend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.MarginPageTransformer
import com.beside153.peopleinside.R
import com.beside153.peopleinside.base.BaseFragment
import com.beside153.peopleinside.common.extension.eventObserve
import com.beside153.peopleinside.databinding.FragmentRecommendBinding
import com.beside153.peopleinside.model.mediacontent.Pick10Model
import com.beside153.peopleinside.model.mediacontent.SubRankingModel
import com.beside153.peopleinside.util.LinearLinelItemDecoration
import com.beside153.peopleinside.util.dpToPx
import com.beside153.peopleinside.util.setOpenActivityAnimation
import com.beside153.peopleinside.view.contentdetail.ContentDetailActivity
import com.beside153.peopleinside.view.contentdetail.CreateReviewActivity
import com.beside153.peopleinside.view.login.nonmember.NonMemberLoginActivity
import com.beside153.peopleinside.viewmodel.recommend.RecommendEvent
import com.beside153.peopleinside.viewmodel.recommend.RecommendViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecommendFragment : BaseFragment() {
    private lateinit var binding: FragmentRecommendBinding
    private val recommendViewModel: RecommendViewModel by viewModels()

    private val pagerAdapter =
        Pick10ViewPagerAdapter(
            ::onPick10ItemClick,
            ::onTopReviewClick,
            ::onBookmarkClick,
            ::onGoToWriteReviewClick,
            ::onRefreshClick
        )
    private val rankingAdpater = SubRankingListAdapter(::onSubRankingItemClick)
    private var scrollPosition: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_recommend, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            viewModel = recommendViewModel
            lifecycleOwner = this@RecommendFragment
        }

        binding.pick10ViewPager.apply {
            val pagerOffsetPx = 16.dpToPx(resources.displayMetrics)
            val pagerMarginPx = 8.dpToPx(resources.displayMetrics)
            adapter = pagerAdapter
            offscreenPageLimit = 2
            setPadding(pagerOffsetPx, 0, pagerOffsetPx, 0)
            setPageTransformer(MarginPageTransformer(pagerMarginPx))
        }

        binding.subRankingRecyclerView.apply {
            adapter = rankingAdpater
            layoutManager = object : LinearLayoutManager(requireActivity()) {
                override fun canScrollVertically(): Boolean = false
            }
            addItemDecoration(
                LinearLinelItemDecoration(
                    1f.dpToPx(resources.displayMetrics),
                    0f,
                    ContextCompat.getColor(requireActivity(), R.color.gray_300)
                )
            )
        }

        recommendViewModel.initAllData()

        recommendViewModel.viewPagerList.observe(viewLifecycleOwner) { list ->
            pagerAdapter.submitList(list)
        }

        recommendViewModel.error.eventObserve(viewLifecycleOwner) {
            showErrorDialog(it) { recommendViewModel.initAllData() }
        }

        recommendViewModel.subRankingList.observe(viewLifecycleOwner) { list ->
            rankingAdpater.submitList(list)
        }

        recommendViewModel.recommendEvent.eventObserve(viewLifecycleOwner) {
            when (it) {
                is RecommendEvent.Pick10ItemClick -> {
                    activityResultLauncher.launch(
                        ContentDetailActivity.newIntent(
                            requireActivity(),
                            false,
                            it.item.contentId
                        )
                    )
                    requireActivity().setOpenActivityAnimation()
                }

                is RecommendEvent.TopReviewClick -> {
                    activityResultLauncher.launch(
                        ContentDetailActivity.newIntent(
                            requireActivity(),
                            true,
                            it.item.contentId
                        )
                    )
                    requireActivity().setOpenActivityAnimation()
                }

                is RecommendEvent.BattleItemClick -> {
                    activityResultLauncher.launch(
                        ContentDetailActivity.newIntent(
                            requireActivity(),
                            false,
                            it.item.contentId
                        )
                    )
                    requireActivity().setOpenActivityAnimation()
                }

                is RecommendEvent.BattleItemCommentClick -> {
                    activityResultLauncher.launch(
                        ContentDetailActivity.newIntent(
                            requireActivity(),
                            true,
                            it.item.contentId
                        )
                    )
                    requireActivity().setOpenActivityAnimation()
                }

                is RecommendEvent.SubRankingArrowClick -> {
                    startActivity(RecommendSubRankingActivity.newIntent(requireActivity(), it.mediaType))
                    requireActivity().setOpenActivityAnimation()
                }

                is RecommendEvent.SubRankingItemClick -> {
                    startActivity(ContentDetailActivity.newIntent(requireActivity(), false, it.item.contentId))
                    requireActivity().setOpenActivityAnimation()
                }

                RecommendEvent.RefreshPick10Click -> {
                    binding.pick10ViewPager.currentItem = 0
                }

                RecommendEvent.MbtiImgClick -> {
                    findNavController().navigate(R.id.myPageFragment)
                }

                RecommendEvent.GuestLogin -> {
                    startActivity(NonMemberLoginActivity.newIntent(requireActivity()))
                    requireActivity().setOpenActivityAnimation()
                }

                is RecommendEvent.GoToWriteReviewClick -> {
                    activityResultLauncher.launch(CreateReviewActivity.newIntent(requireActivity(), it.contentId, ""))
                    requireActivity().setOpenActivityAnimation()
                }
            }
        }
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                recommendViewModel.initAllData()
            }
        }

    override fun onResume() {
        super.onResume()
        binding.recommendScrollView.post { binding.recommendScrollView.scrollTo(0, scrollPosition) }
    }

    override fun onStop() {
        super.onStop()
        scrollPosition = binding.recommendScrollView.scrollY
    }

    private fun onPick10ItemClick(item: Pick10Model) {
        recommendViewModel.onPick10ItemClick(item)
    }

    private fun onTopReviewClick(item: Pick10Model) {
        recommendViewModel.onTopReviewClick(item)
    }

    private fun onBookmarkClick(item: Pick10Model) {
        recommendViewModel.onBookmarkClick(item)
    }

    private fun onGoToWriteReviewClick(item: Pick10Model) {
        recommendViewModel.onGoToWriteReviewClick(item)
    }

    private fun onRefreshClick() {
        recommendViewModel.refreshPick10List()
    }

    private fun onSubRankingItemClick(item: SubRankingModel) {
        recommendViewModel.onSubRankingItemClick(item)
    }
}
