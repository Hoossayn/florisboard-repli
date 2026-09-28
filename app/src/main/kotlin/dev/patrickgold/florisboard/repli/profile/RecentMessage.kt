package dev.patrickgold.florisboard.repli.profile

data class RecentMessage(
    val sourcePackage: String,
    val sender: String,
    val text: String,
    val receivedAt: Long,
    val notificationKey: String? = null,
    val conversationId: String? = null,
    val isGroupConversation: Boolean = false,
    val openedAt: Long? = null,
)
