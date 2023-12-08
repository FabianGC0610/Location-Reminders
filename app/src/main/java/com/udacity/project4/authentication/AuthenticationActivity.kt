package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */

private const val TAG = "AuthenticationActivity"
private const val SIGN_IN_RESULT_CODE = 1001

class AuthenticationActivity : AppCompatActivity() {

    private var loginButtonNotClickYet = true

    private val viewModel by viewModels<AuthenticationActivityViewModel>()

    private val signInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onActivityResult(SIGN_IN_RESULT_CODE, result.resultCode, result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityAuthenticationBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_authentication)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        observeAuthenticationState()

        viewModel.loginEvent.observe(this) { loginEvent ->
            if (loginEvent) {
                launchSignInFlow()
                viewModel.onLoginEventCompleted()
            }
        }

        viewModel.loginButtonNotClickYet.observe(this) { buttonNotClickedYet ->
            if (!buttonNotClickedYet) {
                loginButtonNotClickYet = buttonNotClickedYet
            }
        }

        // TODO: a bonus is to customize the sign in flow to look nice using :
        // https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_RESULT_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                Log.i(
                    TAG,
                    "Successfully signed in user " +
                        "${FirebaseAuth.getInstance().currentUser?.displayName}!",
                )
            } else {
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    private fun observeAuthenticationState() {
        viewModel.authenticationState.observe(this) { authenticationState ->
            when (authenticationState) {
                AuthenticationActivityViewModel.AuthenticationState.AUTHENTICATED -> {
                    launchRemindersActivity()
                }

                else -> {
                    if (!loginButtonNotClickYet) {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "You do not complete the login process. Try again.",
                            Snackbar.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
        )

        val signInIntent =
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                providers,
            ).build()

        signInLauncher.launch(signInIntent)
    }

    private fun launchRemindersActivity() {
        val intent = Intent(this, RemindersActivity::class.java)
        startActivity(intent)
    }
}
