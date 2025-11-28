package com.lea.aichattab
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import com.lea.feishulibrary.protocol.ITabProvider
import com.lea.feishulibrary.protocol.TabLifecycle
import com.lea.feishulibrary.protocol.TabContext
import com.lea.aichat.ui.aiChat.AiChatScreen
class AiChatProvider: ITabProvider, TabLifecycle {
    override val tabId: String = "com.lea.feishutab.aichat"
    override val tabName: String = "AI聊天"
    override val tabIcon = Icons.Filled.AccountCircle
    override val priority: Int = 2 // 优先级，显示在消息Tab之后
    @Composable
    override fun TabContent(context: TabContext) {
        AiChatScreen()
    }

    override fun onCreate() {
        // Tab创建时的初始化逻辑
        // 例如：初始化AI服务、注册推送监听等
    }

    override fun onResume() {
        // Tab可见时的逻辑
        // 例如：刷新聊天列表、恢复消息推送等
    }

    override fun onPause() {
        // Tab不可见时的逻辑
        // 例如：暂停消息推送、保存状态等
    }

    override fun onDestroy() {
        // Tab销毁时的清理逻辑
        // 例如：取消网络请求、释放资源等
    }
}