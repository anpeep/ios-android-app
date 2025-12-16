// ui/auth/AuthViewModel.kt
package com.example.gpssportmap.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpssportmap.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: SessionRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                authRepository.login(email, password)
                _success.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Login failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun register(first: String, last: String, email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                authRepository.register(first, last, email, password)
                _success.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed"
            } finally {
                _loading.value = false
            }
        }
    }
}
