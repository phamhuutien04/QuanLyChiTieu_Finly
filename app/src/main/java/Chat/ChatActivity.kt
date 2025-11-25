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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
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

        adapter = ChatAdapter(currentUid, "")
        recyclerChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerChat.adapter = adapter

        loadFriendInfo()
        listenMessages()

        btnSend.setOnClickListener { sendTextMessage() }
        btnLocation.setOnClickListener { sendLocation() }
        btnBack.setOnClickListener { animateBack() }
        btnImage.setOnClickListener { pickImageDialog() }
        btnRequestMoney.setOnClickListener { openRequestMoneyDialog() }
    }

    private fun ensureChatExists() {
        val ref = db.collection("chats").document(chatId)
        ref.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                ref.set(mapOf("members" to listOf(currentUid, friendUid)))
            }
        }
    }

    private fun loadFriendInfo() {
        db.collection("users").document(friendUid)
            .get().addOnSuccessListener { doc ->

                val name = doc.getString("username") ?: "Đang chat"
                tvChatName.text = name

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

                val msgs = snap.documents.map { d ->
                    ChatMessage(
                        senderId = d.getString("senderId") ?: "",
                        text = d.getString("text") ?: "",
                        imageUrl = d.getString("imageUrl") ?: "",
                        mapUrl = d.getString("mapUrl") ?: "",
                        type = d.getString("type") ?: "text",
                        latitude = d.getDouble("latitude"),
                        longitude = d.getDouble("longitude"),
                        amount = d.getLong("amount"),
                        note = d.getString("note"),
                        paid = d.getBoolean("paid") ?: false,
                        timestamp = d.getLong("timestamp") ?: 0
                    )
                }

                adapter.setMessages(msgs)
                recyclerChat.scrollToPosition(msgs.size - 1)

                markSeen(snap)
            }
    }

    private fun markSeen(snap: QuerySnapshot) {
        snap.documents.forEach {
            val sender = it.getString("senderId") ?: ""
            if (sender == currentUid) return@forEach

            val seenBy = it.get("seenBy") as? MutableList<String> ?: mutableListOf()
            if (!seenBy.contains(currentUid)) {
                seenBy.add(currentUid)
                it.reference.update("seenBy", seenBy)
            }
        }
    }

    // ---------------- TEXT ----------------

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

        db.collection("chats").document(chatId)
            .collection("messages").add(msg)

        edtMessage.setText("")
    }

    // ---------------- LOCATION ----------------

    private fun getStaticMap(lat: Double, lng: Double): String {
        val apiKey = "e25de4efcd7f4a09816b7cabd121eadd"

        return "https://maps.geoapify.com/v1/staticmap" +
                "?style=osm-carto" +
                "&width=600&height=300" +
                "&center=lonlat:$lng,$lat" +
                "&zoom=17" +
                "&marker=lonlat:$lng,$lat;color:%23ff0000;size:medium" +
                "&apiKey=$apiKey"
    }

    private fun sendLocation() {

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                111
            )
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

    // ---------------- PICK IMAGE ----------------

    private fun pickImageDialog() {
        val options = arrayOf("Chụp ảnh", "Chọn ảnh từ thư viện")

        AlertDialog.Builder(this)
            .setTitle("Gửi ảnh bằng...")
            .setItems(options) { _, which ->
                if (which == 0) openCamera()
                else openGallery()
            }
            .show()
    }

    private fun openGallery() {
        val i = Intent(Intent.ACTION_PICK)
        i.type = "image/*"
        galleryLauncher.launch(i)
    }

    private fun openCamera() {
        val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(i)
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == RESULT_OK) {
                val uri = res.data?.data
                if (uri != null) uploadToCloudinary(uri)
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

    private fun bitmapToUri(bmp: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bmp, "chat_img", null)
        return Uri.parse(path)
    }

    private fun uploadToCloudinary(uri: Uri) {
        Toast.makeText(this, "Đang tải ảnh...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val input = contentResolver.openInputStream(uri)
                val bytes = input!!.readBytes()

                val result = CloudinaryConfig.cloudinaryInstance.uploader().upload(
                    bytes,
                    mapOf("folder" to "chat")
                )

                val url = result["secure_url"].toString()
                sendImageMessage(url)

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

    // ---------------- REQUEST MONEY ----------------

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

    // ---------------- BACK ----------------

    private fun animateBack() {
        val rotate = RotateAnimation(
            0f, -180f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f
        )
        rotate.duration = 250
        btnBack.startAnimation(rotate)
        finish()
    }
}
