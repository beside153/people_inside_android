package com.beside153.peopleinside.view.login.nonmember

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.beside153.peopleinside.R
import com.beside153.peopleinside.base.BaseActivity
import com.beside153.peopleinside.common.extension.eventObserve
import com.beside153.peopleinside.databinding.ActivityNonMemberMbtiChoiceBinding
import com.beside153.peopleinside.model.common.MbtiModel
import com.beside153.peopleinside.util.addBackPressedAnimation
import com.beside153.peopleinside.util.setCloseActivityAnimation
import com.beside153.peopleinside.util.setOpenActivityAnimation
import com.beside153.peopleinside.view.MainActivity
import com.beside153.peopleinside.view.common.MbtiChoiceScreenAdapter
import com.beside153.peopleinside.viewmodel.login.nonmember.NonMemberMbtiChoiceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NonMemberMbtiChoiceActivity : BaseActivity() {
    private lateinit var binding: ActivityNonMemberMbtiChoiceBinding
    private val mbtiAdapter = MbtiChoiceScreenAdapter(::onMbtiItemClick)
    private val mbtiChoiceViewmodel: NonMemberMbtiChoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_non_member_mbti_choice)

        binding.apply {
            viewModel = mbtiChoiceViewmodel
            lifecycleOwner = this@NonMemberMbtiChoiceActivity
        }

        addBackPressedAnimation()
        initRecyclerView()

        mbtiChoiceViewmodel.initAllData()

        mbtiChoiceViewmodel.screenList.observe(this) { list ->
            mbtiAdapter.submitList(list)
        }

        mbtiChoiceViewmodel.backButtonClickEvent.eventObserve(this) {
            finish()
            setCloseActivityAnimation()
        }

        mbtiChoiceViewmodel.completeButtonClickEvent.eventObserve(this) { mbti ->
            startActivity(
                MainActivity.newIntent(this, false).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            setOpenActivityAnimation()
        }
    }

    private fun initRecyclerView() {
        val gridLayoutManager = GridLayoutManager(this, COUNT_THREE)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (mbtiAdapter.getItemViewType(position)) {
                    R.layout.item_non_member_mbti_title -> COUNT_THREE
                    else -> COUNT_ONE
                }
            }
        }

        binding.mbtiScreenRecyclerView.apply {
            adapter = mbtiAdapter
            layoutManager = gridLayoutManager
        }
    }

    private fun onMbtiItemClick(item: MbtiModel) {
        mbtiChoiceViewmodel.onMbtiItemClick(item)
    }

    companion object {
        private const val COUNT_ONE = 1
        private const val COUNT_THREE = 3

        fun newIntent(context: Context): Intent {
            return Intent(context, NonMemberMbtiChoiceActivity::class.java)
        }
    }
}
