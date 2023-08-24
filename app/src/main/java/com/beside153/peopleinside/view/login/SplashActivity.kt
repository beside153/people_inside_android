package com.beside153.peopleinside.view.login

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import com.beside153.peopleinside.R
import com.beside153.peopleinside.base.BaseActivity
import com.beside153.peopleinside.common.extension.eventObserve
import com.beside153.peopleinside.view.MainActivity
import com.beside153.peopleinside.view.dialog.OneButtonDialog
import com.beside153.peopleinside.view.onboarding.signup.SignUpActivity
import com.beside153.peopleinside.viewmodel.login.SplashEvent
import com.beside153.peopleinside.viewmodel.login.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : BaseActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initObservers()
    }

    override fun onResume() {
        super.onResume()
        splashViewModel.getAllData()
    }

    private fun initObservers() {
        splashViewModel.error.eventObserve(this) {
            showErrorDialog(it) { splashViewModel.getAllData() }
        }

        splashViewModel.splashEvent.eventObserve(this) {
            when (it) {
                SplashEvent.UpdateApp -> showNeedUpdateDialog()

                SplashEvent.GoToPlayStore -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.beside153.peopleinside")
                    startActivity(intent)
                }

                SplashEvent.NoUserInfo -> showNoUserInfoDialog()

                is SplashEvent.OnBoardingCompleted -> {
                    if (it.isOnBoardingCompleted) {
                        if (it.isMember) {
                            startActivity(LoginActivity.newIntent(this))
                            finish()
                            return@eventObserve
                        }
                        startActivity(MainActivity.newIntent(this, false))
                        finish()
                        return@eventObserve
                    }
                    startActivity(SignUpActivity.newIntent(this, ON_BOARDING))
                    finish()
                }
            }
        }
    }

    private fun showNoUserInfoDialog() {
        val noUserInfoDialog = OneButtonDialog.OneButtonDialogBuilder()
            .setDescriptionRes(R.string.no_user_info_dialog_description)
            .setButtonTextRes(R.string.sign_up)
            .setButtonClickListener(object : OneButtonDialog.OneButtonDialogListener {
                override fun onDialogButtonClick() {
                    startActivity(LoginActivity.newIntent(this@SplashActivity))
                    finish()
                }
            }).create()
        noUserInfoDialog.isCancelable = false
        noUserInfoDialog.show(supportFragmentManager, noUserInfoDialog.tag)
    }

    private fun showNeedUpdateDialog() {
        val needUpdateDialog = OneButtonDialog.OneButtonDialogBuilder()
            .setTitleRes(R.string.need_update_dialog_title)
            .setDescriptionRes(R.string.need_update_dialog_description)
            .setButtonTextRes(R.string.need_update_dialog_button)
            .setButtonClickListener(object : OneButtonDialog.OneButtonDialogListener {
                override fun onDialogButtonClick() {
                    splashViewModel.onGoToPlayStoreButtonClick()
                }
            }).create()
        needUpdateDialog.isCancelable = false
        needUpdateDialog.show(supportFragmentManager, needUpdateDialog.tag)
    }

    companion object {
        private const val ON_BOARDING = "on boarding not completed"
    }
}
