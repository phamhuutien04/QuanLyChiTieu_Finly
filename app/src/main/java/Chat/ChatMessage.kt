package Chat

data class ChatMessage(
    var msgId: String = "",
    var chatId: String = "",

    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val mapUrl: String = "",
    val type: String = "text",

    // location
    val latitude: Double? = null,
    val longitude: Double? = null,

    // request money
    val amount: Long? = null,
    val note: String? = null,
    val paid: Boolean = false,

    val timestamp: Long = 0
)
