package Chat

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val mapUrl: String = "",
    val type: String = "text",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = 0
)
