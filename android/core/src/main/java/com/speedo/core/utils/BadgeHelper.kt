package com.speedo.core.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BadgeHelper {
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun updateUnreadCount(count: Int) {
        _unreadCount.value = count
    }

    fun decrementCount() {
        if (_unreadCount.value > 0) {
            _unreadCount.value -= 1
        }
    }
}
