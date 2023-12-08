package com.udacity.project4.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.udacity.project4.FirebaseUserLiveData

class AuthenticationActivityViewModel : ViewModel() {

    private val _loginEvent = MutableLiveData<Boolean>()
    val loginEvent: LiveData<Boolean> get() = _loginEvent

    private val _loginButtonNotClickYet = MutableLiveData<Boolean>()
    val loginButtonNotClickYet: LiveData<Boolean> get() = _loginButtonNotClickYet

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

    fun onLoginEvent() {
        _loginEvent.value = true
        _loginButtonNotClickYet.value = false
    }

    fun onLoginEventCompleted() {
        _loginEvent.value = false
    }
}
