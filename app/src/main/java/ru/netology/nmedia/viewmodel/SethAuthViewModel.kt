package ru.netology.nmedia.viewmodel

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.error.ApiError
import java.io.IOException
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.nmedia.api.ApiService
import javax.inject.Inject

@HiltViewModel
class SetAuthViewModel @Inject constructor
    (application: Application,
     private val auth: AppAuth,
     private val apiService: ApiService,) : AndroidViewModel(application) {

    val app = application

    fun updateUser(login: String, pass: String) {
        val text = "Указан неверный логин или пароль"
        val textNetwork = "Проверьте подключение к сети"
        val textUnknown = "Неизвестная ошибка"
        val duration = Toast.LENGTH_SHORT
        viewModelScope.launch {
            try {
                val response = apiService.updateUser(login, pass)
                if (!response.isSuccessful) {
                    val toast = Toast.makeText(app, text, duration)
                    toast.show()
                } else {
                    val body =
                        response.body() ?: throw ApiError(response.code(), response.message())
                    auth.setAuth(body.id, body.token)
                }
            } catch (e: IOException) {
                val toast = Toast.makeText(app, textNetwork, duration)
                toast.show()
            } catch (e: Exception) {
                val toast = Toast.makeText(app, textUnknown, duration)
                toast.show()
            }
        }
    }
}