package com.beside153.peopleinside.view.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.beside153.peopleinside.R
import com.beside153.peopleinside.base.BaseFragment
import com.beside153.peopleinside.databinding.FragmentMyPageBinding
import com.beside153.peopleinside.util.EventObserver
import com.beside153.peopleinside.util.setOpenActivityAnimation
import com.beside153.peopleinside.viewmodel.mypage.MyPageViewModel

class MyPageFragment : BaseFragment() {
    private lateinit var binding: FragmentMyPageBinding
    private val myPageViewModel: MyPageViewModel by viewModels { MyPageViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_my_page, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            viewModel = myPageViewModel
            lifecycleOwner = this@MyPageFragment
        }

        myPageViewModel.initAllData()

        myPageViewModel.bookmarkContentsClickEvent.observe(
            viewLifecycleOwner,
            EventObserver {
                bookmarkContentsActivityLauncher.launch(BookmarkContentsActivity.newIntent(requireActivity()))
                requireActivity().setOpenActivityAnimation()
            }
        )

        myPageViewModel.ratingContentsClickEvent.observe(
            viewLifecycleOwner,
            EventObserver {
                ratingContentsActivityLauncher.launch(RatingContentsActivity.newIntent(requireActivity()))
                requireActivity().setOpenActivityAnimation()
            }
        )

        myPageViewModel.editProfileClickEvent.observe(
            viewLifecycleOwner,
            EventObserver {
                editProfileActivityLauncher.launch(EditProfileActivity.newIntent(requireActivity()))
                requireActivity().setOpenActivityAnimation()
            }
        )

        myPageViewModel.settingClickEvent.observe(
            viewLifecycleOwner,
            EventObserver {
                startActivity(SettingActivity.newIntent(requireActivity()))
                requireActivity().setOpenActivityAnimation()
            }
        )

        myPageViewModel.error.observe(
            viewLifecycleOwner,
            EventObserver {
                showErrorDialog { myPageViewModel.initAllData() }
            }
        )
    }

    private val bookmarkContentsActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                myPageViewModel.initAllData()
            }
        }

    private val ratingContentsActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                myPageViewModel.initAllData()
            }
        }

    private val editProfileActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                myPageViewModel.initAllData()
            }
        }
}
