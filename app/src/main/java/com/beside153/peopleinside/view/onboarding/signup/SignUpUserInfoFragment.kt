package com.beside153.peopleinside.view.onboarding.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.beside153.peopleinside.R
import com.beside153.peopleinside.base.BaseFragment
import com.beside153.peopleinside.common.extension.eventObserve
import com.beside153.peopleinside.databinding.FragmentSignUpUserInfoBinding
import com.beside153.peopleinside.view.common.BirthYearBottomSheetFragment
import com.beside153.peopleinside.viewmodel.onboarding.signup.SignUpUserInfoEvent
import com.beside153.peopleinside.viewmodel.onboarding.signup.SignUpUserInfoViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpUserInfoFragment : BaseFragment() {
    private lateinit var binding: FragmentSignUpUserInfoBinding
    private val userInfoViewModel: SignUpUserInfoViewModel by activityViewModels()
    private var year = INITIAL_YEAR
    private var mbti = INITIAL_MBTI
    private var gender = INITIAL_GENDER

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        userInfoViewModel.initNickname()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up_user_info, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            viewModel = userInfoViewModel
            lifecycleOwner = this@SignUpUserInfoFragment
        }

        initSelectedValues()
        setFragmentsResultListener()

        userInfoViewModel.selectedGender.observe(viewLifecycleOwner) {
            gender = it
        }

        userInfoViewModel.backButtonClickEvent.eventObserve(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        userInfoViewModel.error.eventObserve(viewLifecycleOwner) {
            showErrorDialog(it) { userInfoViewModel.onSignUpButtonClick() }
        }

        userInfoViewModel.signUpUserInfoEvent.eventObserve(viewLifecycleOwner) {
            when (it) {
                SignUpUserInfoEvent.BirthYearClick -> {
                    val bottomSheet = BirthYearBottomSheetFragment.newInstance(year)
                    bottomSheet.show(childFragmentManager, bottomSheet.tag)
                }

                SignUpUserInfoEvent.MbtiChoiceClick -> {
                    val action =
                        SignUpUserInfoFragmentDirections.actionSignUpUserInfoFragmentToSignUpMbtiChoiceFragment(mbti)
                    findNavController().navigate(action)
                }

                is SignUpUserInfoEvent.SignUpSuccess -> {
                    firebaseAnalytics.logEvent("회원가입") {
                        param("유저_ID", it.user.userId.toString())
                        param("유저명", it.user.nickname)
                        param("유저_MBTI", it.user.mbti)
                        param("소셜로그인_경로", "KAKAO")
                    }

                    val action =
                        SignUpUserInfoFragmentDirections.actionSignUpUserInfoFragmentToSignUpContentChoiceFragment()
                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun setFragmentsResultListener() {
        childFragmentManager.setFragmentResultListener(
            BirthYearBottomSheetFragment::class.java.simpleName,
            this
        ) { _, bundle ->
            year = bundle.getInt(YEAR_KEY)
            userInfoViewModel.setSelectedYear(year)
        }

        setFragmentResultListener(SignUpMbtiChoiceFragment::class.java.simpleName) { _, bundle ->
            mbti = bundle.getString(MBTI_KEY) ?: INITIAL_MBTI
            userInfoViewModel.setSelectedMbti(mbti)
        }
    }

    private fun initSelectedValues() {
        userInfoViewModel.setSelectedYear(year)
        userInfoViewModel.setSelectedMbti(mbti)
        userInfoViewModel.setSelectedGender(gender)
    }

    companion object {
        private const val YEAR_KEY = "year"
        private const val INITIAL_YEAR = 1990
        private const val MBTI_KEY = "mbti"
        private const val INITIAL_MBTI = "선택"
        private const val INITIAL_GENDER = "women"
    }
}
