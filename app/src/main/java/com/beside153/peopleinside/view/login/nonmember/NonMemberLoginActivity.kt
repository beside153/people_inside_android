package com.beside153.peopleinside.view.login.nonmember

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.beside153.peopleinside.R
import com.beside153.peopleinside.base.BaseActivity
import com.beside153.peopleinside.common.extension.eventObserve
import com.beside153.peopleinside.databinding.ActivityNonMemberLoginBinding
import com.beside153.peopleinside.util.addBackPressedAnimation
import com.beside153.peopleinside.util.setCloseActivityAnimation
import com.beside153.peopleinside.util.showToast
import com.beside153.peopleinside.view.MainActivity
import com.beside153.peopleinside.view.onboarding.signup.SignUpActivity
import com.beside153.peopleinside.viewmodel.login.nonmember.NonMemberLoginEvent
import com.beside153.peopleinside.viewmodel.login.nonmember.NonMemberLoginViewModel
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.AuthError
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class NonMemberLoginActivity : BaseActivity() {
    private lateinit var binding: ActivityNonMemberLoginBinding
    private lateinit var kakaoApi: UserApiClient
    private val nonMemberLoginViewModel: NonMemberLoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_non_member_login)

        binding.apply {
            viewModel = nonMemberLoginViewModel
            lifecycleOwner = this@NonMemberLoginActivity
        }

        KakaoSdk.init(this, getString(R.string.kakao_native_app_key))
        kakaoApi = UserApiClient.instance

        addBackPressedAnimation { setResult(BACK_FROM_LOGINACTIVITY) }

        nonMemberLoginViewModel.backButtonClickEvent.eventObserve(this) {
            setResult(BACK_FROM_LOGINACTIVITY)
            finish()
            setCloseActivityAnimation()
        }

        nonMemberLoginViewModel.error.eventObserve(this) {
            showErrorDialog(it)
        }

        nonMemberLoginViewModel.nonMemberLoginEvent.eventObserve(this) {
            when (it) {
                NonMemberLoginEvent.KakaoLoginClick -> kakaoLogin()
                is NonMemberLoginEvent.GoToSignUp -> {
                    startActivity(
                        SignUpActivity.newIntent(this, it.authToken).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }

                is NonMemberLoginEvent.OnBoardingCompleted -> {
                    if (it.isCompleted) {
                        startActivity(
                            MainActivity.newIntent(this, false).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    } else {
                        startActivity(
                            SignUpActivity.newIntent(this, ON_BOARDING).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
                }
            }
        }
    }

    private val didUserCancel: (error: Throwable) -> Boolean = { error ->
        error is ClientError && error.reason == ClientErrorCause.Cancelled ||
            error is AuthError && error.msg == "UserInfo denied access"
    }

    private fun kakaoLogin() {
        if (kakaoApi.isKakaoTalkLoginAvailable(this)) {
            kakaoApi.loginWithKakaoTalk(this) { token, error ->
                if (error != null) {
                    Timber.e(error)
                    if (!didUserCancel(error)) {
                        showToast(R.string.kakaotalk_login_failed)
                        // 카카오 이메일 로그인
                        kakaoApi.loginWithKakaoAccount(this, callback = kakaoEmailLoginCallbak)
                    }
                } else if (token != null) {
                    getKakaoAccountInfo(token.accessToken)
                }
            }
        } else {
            // 카카오 이메일 로그인
            kakaoApi.loginWithKakaoAccount(this, callback = kakaoEmailLoginCallbak)
        }
    }

    private val kakaoEmailLoginCallbak: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            Timber.e(error)
            if (!didUserCancel(error)) {
                showToast(R.string.kakao_email_login_failed)
            }
        } else if (token != null) {
            getKakaoAccountInfo(token.accessToken)
        }
    }

    private fun getKakaoAccountInfo(authToken: String) {
        nonMemberLoginViewModel.setAuthToken(authToken)

        kakaoApi.me { user, error ->
            if (error != null) {
                Timber.e(error)
                showToast(R.string.kakao_user_info_load_failed)
            } else if (user != null) {
                val email = user.kakaoAccount?.email ?: ""
                nonMemberLoginViewModel.login(email)
            }
        }
    }

    companion object {
        private const val BACK_FROM_LOGINACTIVITY = 111
        private const val ON_BOARDING = "on boarding not completed"

        fun newIntent(context: Context): Intent {
            return Intent(context, NonMemberLoginActivity::class.java)
        }
    }
}
