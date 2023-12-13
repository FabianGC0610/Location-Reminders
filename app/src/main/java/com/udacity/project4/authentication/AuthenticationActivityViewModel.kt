package com.udacity.project4.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AuthenticationActivityViewModel : ViewModel() {

    private val _loginEvent = MutableLiveData<Boolean>()
    val loginEvent: LiveData<Boolean> get() = _loginEvent

    private val _loginButtonNotClickYet = MutableLiveData<Boolean>()
    val loginButtonNotClickYet: LiveData<Boolean> get() = _loginButtonNotClickYet

    private val _authenticationState = MutableLiveData<AuthenticationState>()
    val authenticationState: LiveData<AuthenticationState> = _authenticationState

    fun setAuthenticationState(state: AuthenticationState) {
        _authenticationState.value = state
    }

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED
    }

    fun onLoginEvent() {
        _loginEvent.value = true
        _loginButtonNotClickYet.value = false
    }

    fun onLoginEventCompleted() {
        _loginEvent.value = false
    }
}
