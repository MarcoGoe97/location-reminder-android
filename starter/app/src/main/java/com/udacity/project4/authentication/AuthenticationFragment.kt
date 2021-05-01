package com.udacity.project4.authentication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.databinding.FragmentAuthenticationBinding
import com.udacity.project4.utils.Constants

class AuthenticationFragment : Fragment() {

    private lateinit var binding: FragmentAuthenticationBinding

    private val viewModel by viewModels<AuthenticationViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAuthenticationBinding.inflate(inflater)
        binding.lifecycleOwner = this

        binding.btnLogin.setOnClickListener {
            launchSignInFlow()
        }

        // Observe the authentication state so we can know if the user has logged in successfully.
        viewModel.authenticationState.observe(viewLifecycleOwner) { authenticationState ->
            if(authenticationState == Constants.AuthenticationState.AUTHENTICATED) {
                findNavController().navigate(AuthenticationFragmentDirections.toReminderList())
            } else {
                binding.btnLogin.visibility = View.VISIBLE
            }
        }

        return binding.root
    }

    private fun launchSignInFlow() {
        //Sign in options
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent.
        startActivity(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
        )
    }
}