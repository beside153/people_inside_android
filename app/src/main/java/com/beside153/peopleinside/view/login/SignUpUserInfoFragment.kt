package com.beside153.peopleinside.view.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.beside153.peopleinside.R
import com.beside153.peopleinside.databinding.FragmentSignUpUserInfoBinding

class SignUpUserInfoFragment : Fragment() {
    private lateinit var binding: FragmentSignUpUserInfoBinding

    private var year = 1964

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

        childFragmentManager.setFragmentResultListener(
            SignUpBottomSheetFragment::class.java.simpleName,
            this
        ) { _, bundle ->
            year = bundle.getInt("year")
            binding.birthYearChoiceTextView.text = "${year}년"
        }

        binding.birthYearChoiceTextView.setOnClickListener {
            val bottomSheet = SignUpBottomSheetFragment.newInstance(year)
            bottomSheet.show(childFragmentManager, bottomSheet.tag)
        }

        binding.mbtiChoiceTextView.setOnClickListener {
            val action = SignUpUserInfoFragmentDirections.actionSignUpUserInfoFragmentToSignUpMbtiChoiceFragment()
            findNavController().navigate(action)
        }

        binding.signUpButton.setOnClickListener {
            val action = SignUpUserInfoFragmentDirections.actionSignUpUserInfoFragmentToSignUpContentChoiceFragment()
            findNavController().navigate(action)
        }
    }
}
