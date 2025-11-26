package Chat

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.animation.RotateAnimation
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlychitieu_finly.CloudinaryConfig
import com.example.quanlychitieu_finly.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.ByteArrayOutputStream

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerChat: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var btnLocation: ImageView
    private lateinit var btnImage: ImageView
    private lateinit var btnRequestMoney: ImageView
    private lateinit var imgChatAvatar: ImageView
    private lateinit var tvChatName: TextView

    private lateinit var fusedLocation: FusedLocationProviderClient

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var chatId = ""
    private var friendUid = ""
    private var currentUid = ""

    private lateinit var adapter: ChatAdapter
    private var friendAvatar = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatId = intent.getStringExtra("chatId") ?: ""
        friendUid = intent.getStringExtra("friendUid") ?: ""
        currentUid = auth.currentUser?.uid ?: ""

        ensureChatExists()

        recyclerChat = findViewById(R.id.recyclerChat)
        edtMessage = findViewById(R.id.edtMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        btnLocation = findViewById(R.id.btnLocation)
        btnImage = findViewById(R.id.btnImage)
        btnRequestMoney = findViewById(R.id.btnRequestMoney)
        imgChatAvatar = findViewById(R.id.imgChatAvatar)
        tvChatName = findViewById(R.id.tvChatName)

        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        adapter = ChatAdapter(
            currentUid,
            friendAvatar
        ) { msg ->
            onPayRequest(msg)   // ⭐ callback từ adapter
        }

        recyclerChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerChat.adapter = adapter

        loadFriendInfo()
        listenMessages()

        btnSend.setOnClickListener { sendTextMessage() }
        btnImage.setOnClickListener { pickImageDialog() }
        btnLocation.setOnClickListener { sendLocation() }
        btnRequestMoney.setOnClickListener { openRequestMoneyDialog() }
        btnBack.setOnClickListener { animateBack() }
    }
    override fun onResume() {
        super.onResume()
        markAllMessagesAsSeen()
    }
    private fun markAllMessagesAsSeen() {

        val msgRef = db.collection("chats")
            .document(chatId)
            .collection("messages")

        msgRef.whereNotEqualTo("senderId", currentUid)
            .get()
            .addOnSuccessListener { snap ->

                for (d in snap.documents) {
                    val seenBy = (d.get("seenBy") as? MutableList<String>) ?: mutableListOf()

                    if (!seenBy.contains(currentUid)) {
                        seenBy.add(currentUid)
                        d.reference.update("seenBy", seenBy)
                    }
                }
            }
    }

    private fun ensureChatExists() {
        val ref = db.collection("chats").document(chatId)
        ref.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                ref.set(
                    mapOf(
                        "members" to listOf(currentUid, friendUid)
                    )
                )
            }
        }
    }

    private fun loadFriendInfo() {
        db.collection("users").document(friendUid)
            .get()
            .addOnSuccessListener { doc ->
                tvChatName.text = doc.getString("username") ?: "Đang chat"
                friendAvatar = doc.getString("avatarUrl") ?: ""
                Glide.with(this).load(friendAvatar).circleCrop().into(imgChatAvatar)
                adapter.setFriendAvatar(friendAvatar)
            }
    }

    private fun listenMessages() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->

                if (snap == null) return@addSnapshotListener

                val list = snap.documents.map { d ->

                    ChatMessage(
                        senderId = d.getString("senderId") ?: "",
                        text = d.getString("text") ?: "",
                        imageUrl = d.getString("imageUrl") ?: "",
                        mapUrl = d.getString("mapUrl") ?: "",
                        type = d.getString("type") ?: "",
                        latitude = d.getDouble("latitude"),
                        longitude = d.getDouble("longitude"),
                        amount = d.getLong("amount"),
                        note = d.getString("note"),
                        paid = d.getBoolean("paid") ?: false,
                        timestamp = d.getLong("timestamp") ?: 0
                    ).apply {
                        msgId = d.id
                        chatId = this@ChatActivity.chatId
                    }
                }

                adapter.setMessages(list)
                recyclerChat.scrollToPosition(list.size - 1)
            }
    }

    // ================================================================================================
    //                                          SEND MESSAGES
    // ================================================================================================

    private fun sendTextMessage() {
        val text = edtMessage.text.toString().trim()
        if (text.isEmpty()) return

        val msg = mapOf(
            "senderId" to currentUid,
            "text" to text,
            "type" to "text",
            "timestamp" to System.currentTimeMillis(),
            "seenBy" to listOf(currentUid)
        )

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(msg)

        edtMessage.setText("")
    }

    // ================================================================================================
    //                                          IMAGE
    // ================================================================================================

    private fun pickImageDialog() {
        AlertDialog.Builder(this)
            .setTitle("Gửi ảnh bằng...")
            .setItems(arrayOf("Chụp ảnh", "Chọn ảnh từ thư viện")) { _, which ->
                if (which == 0) openCamera()
                else openGallery()
            }.show()
    }

    private fun openGallery() {
        val i = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        galleryLauncher.launch(i)
    }

    private fun openCamera() {
        val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(i)
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == RESULT_OK) {
                res.data?.data?.let { uploadToCloudinary(it) }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == RESULT_OK) {
                val bmp = res.data?.extras?.get("data") as? Bitmap ?: return@registerForActivityResult
                val uri = bitmapToUri(bmp)
                uploadToCloudinary(uri)
            }
        }

    private fun bitmapToUri(b: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, b, "chat_img", null)
        return Uri.parse(path)
    }

    private fun uploadToCloudinary(uri: Uri) {
        Toast.makeText(this, "Đang tải ảnh...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val bytes = contentResolver.openInputStream(uri)!!.readBytes()

                val result = CloudinaryConfig.cloudinaryInstance.uploader().upload(
                    bytes, mapOf("folder" to "chat")
                )

                sendImageMessage(result["secure_url"].toString())

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun sendImageMessage(url: String) {
        val msg = mapOf(
            "senderId" to currentUid,
            "type" to "image",
            "imageUrl" to url,
            "timestamp" to System.currentTimeMillis(),
            "seenBy" to listOf(currentUid)
        )

        db.collection("chats").document(chatId)
            .collection("messages")
            .add(msg)
    }

    // ================================================================================================
    //                                          LOCATION
    // ================================================================================================

    private fun sendLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 111)
            return
        }

        fusedLocation.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {

                val mapUrl = getStaticMap(loc.latitude, loc.longitude)

                val msg = mapOf(
                    "senderId" to currentUid,
                    "type" to "location_map",
                    "mapUrl" to mapUrl,
                    "latitude" to loc.latitude,
                    "longitude" to loc.longitude,
                    "timestamp" to System.currentTimeMillis(),
                    "seenBy" to listOf(currentUid)
                )

                db.collection("chats").document(chatId)
                    .collection("messages")
                    .add(msg)
            }
        }
    }

    private fun getStaticMap(lat: Double, lng: Double): String {
        val apiKey = "e25de4efcd7f4a09816b7cabd121eadd"
        return "https://maps.geoapify.com/v1/staticmap" +
                "?style=osm-carto&width=600&height=300" +
                "&center=lonlat:$lng,$lat&zoom=17" +
                "&marker=lonlat:$lng,$lat;color:%23ff0000;size:medium" +
                "&apiKey=$apiKey"
    }

    // ================================================================================================
    //                                      REQUEST MONEY
    // ================================================================================================

    private fun openRequestMoneyDialog() {

        val v = layoutInflater.inflate(R.layout.dialog_request_money, null)
        val edtAmount = v.findViewById<EditText>(R.id.edtAmount)
        val edtNote = v.findViewById<EditText>(R.id.edtNote)

        AlertDialog.Builder(this)
            .setTitle("Đòi tiền")
            .setView(v)
            .setPositiveButton("Gửi") { _, _ ->

                val amount = edtAmount.text.toString().toLongOrNull() ?: 0
                val note = edtNote.text.toString()

                if (amount > 0) sendMoneyRequest(amount, note)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun sendMoneyRequest(amount: Long, note: String) {

        val msg = mapOf(
            "senderId" to currentUid,
            "type" to "request_money",
            "amount" to amount,
            "note" to note,
            "paid" to false,
            "timestamp" to System.currentTimeMillis(),
            "seenBy" to listOf(currentUid)
        )

        db.collection("chats").document(chatId)
            .collection("messages")
            .add(msg)
    }

    // ================================================================================================
    //                                      PAY MONEY (MAIN LOGIC)
    // ================================================================================================

    fun onPayRequest(msg: ChatMessage) {

        val amount = msg.amount ?: 0
        if (amount <= 0) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        processPayment(msg)
    }

    private fun processPayment(msg: ChatMessage) {

        val payerId = currentUid          // A: người trả tiền
        val receiverId = msg.senderId     // B: người gửi yêu cầu

        val payerRef = db.collection("users").document(payerId)
        val receiverRef = db.collection("users").document(receiverId)

        val amountDouble = msg.amount?.toDouble() ?: 0.0
        if (amountDouble <= 0) {
            Toast.makeText(this, "Số tiền không hợp lệ!", Toast.LENGTH_SHORT).show()
            return
        }

        //  LẤY DANH MỤC "KHÁC - SPENDING" CỦA A (payer)
        payerRef.collection("categories")
            .whereEqualTo("name", "Khác")
            .whereEqualTo("type", "spending")
            .limit(1)
            .get()
            .addOnSuccessListener { snapPayer ->

                if (snapPayer.isEmpty) {
                    Toast.makeText(this, "Bạn không có danh mục Khác (chi tiêu)", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val payerCatDoc = snapPayer.documents[0]
                val payerCatId = payerCatDoc.id

                receiverRef.collection("categories")
                    .whereEqualTo("name", "Khác")
                    .whereEqualTo("type", "income")
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapReceiver ->

                        if (snapReceiver.isEmpty) {
                            Toast.makeText(this, "Người nhận không có danh mục Khác (thu nhập)", Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }

                        val receiverCatDoc = snapReceiver.documents[0]
                        val receiverCatId = receiverCatDoc.id

                        // ⭐ 3️⃣ TẠO 2 TRANSACTION: A (spending) + B (income)
                        val payerTxRef = payerRef.collection("transactions").document()
                        val receiverTxRef = receiverRef.collection("transactions").document()

                        val payerTx = mapOf(
                            "title" to "Thanh toán cho ${tvChatName.text}",
                            "categoryId" to payerCatId,
                            "categoryName" to "Khác",
                            "categoryIconUrl" to payerCatDoc.getString("iconUrl"),
                            "amount" to amountDouble,
                            "type" to "spending",
                            "date" to Timestamp.now()
                        )

                        val receiverTx = mapOf(
                            "title" to "Nhận thanh toán từ bạn",
                            "categoryId" to receiverCatId,
                            "categoryName" to "Khác",
                            "categoryIconUrl" to receiverCatDoc.getString("iconUrl"),
                            "amount" to amountDouble,
                            "type" to "income",
                            "date" to Timestamp.now()
                        )

                        db.runBatch { b ->

                            // transaction của người trả
                            b.set(payerTxRef, payerTx)

                            // transaction của người nhận
                            b.set(receiverTxRef, receiverTx)

                            // tăng spending cho A
                            b.update(
                                payerRef.collection("categories").document(payerCatId),
                                "totalAmount",
                                FieldValue.increment(amountDouble)
                            )

                            // tăng income cho B
                            b.update(
                                receiverRef.collection("categories").document(receiverCatId),
                                "totalAmount",
                                FieldValue.increment(amountDouble)
                            )

                            // đánh dấu đã thanh toán
                            val msgRef = db.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .document(msg.msgId)

                            b.update(msgRef, "paid", true)

                        }.addOnSuccessListener {
                            Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
    }



    // ================================================================================================
    //                                          BACK
    // ================================================================================================

    private fun animateBack() {
        val r = RotateAnimation(
            0f, -180f,
            RotateAnimation.RELATIVE_TO_SELF, .5f,
            RotateAnimation.RELATIVE_TO_SELF, .5f
        )
        r.duration = 250
        btnBack.startAnimation(r)
        finish()
    }
}
