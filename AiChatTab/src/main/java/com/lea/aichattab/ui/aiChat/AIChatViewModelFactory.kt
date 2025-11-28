package com.lea.aichattab.ui.aiChat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lea.aichat.DataBsae.ChatDatabase
import com.lea.aichat.data.AIChat.repository.ChatRepository
class AIChatViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AIChatViewModel::class.java)) {
            val database = ChatDatabase.getDataBase(context)
            val repository = ChatRepository(database = database)
            @Suppress("UNCHECKED_CAST")
            return AIChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}