package com.example.khoaluan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.khoaluan.databinding.ActivityResultBinding
import com.example.khoaluan.databinding.ItemInfoCardBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

// Data Class dùng để hiển thị các giai đoạn sinh trưởng của côn trùng
data class LifeStage(
    val stage: String = "",
    val url: String = ""
)

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLat: Double? = null
    private var currentLng: Double? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) fetchCurrentLocation()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val predictedName = intent.getStringExtra("PREDICTED_NAME")
        val confidence = intent.getFloatExtra("CONFIDENCE", 0f)
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val imageUri = if (!imageUriString.isNullOrEmpty()) Uri.parse(imageUriString) else null
        val imageResId = intent.getIntExtra("IMAGE_RES_ID", -1)
        val inferenceTime = intent.getLongExtra("INFERENCE_TIME", -1L)

        // CHỈNH SỬA: Dịch tên sang tiếng Việt ngay trên tiêu đề để thân thiện hơn
        binding.tvResultName.text = "(${translateToVietnamese(predictedName ?: "")})"
        binding.btnBack.setOnClickListener { finish() }

        if (confidence <= 0f) {
            binding.chipConfidence.visibility = View.GONE
            binding.chipInferenceTime.visibility = View.GONE
            binding.cardNotebook.visibility = View.GONE
        } else {
            binding.chipConfidence.text = "Độ tin cậy: ${"%.2f".format(confidence)}%"
            if (inferenceTime > 0) {
                binding.chipInferenceTime.visibility = View.VISIBLE
                binding.chipInferenceTime.text = "Thời gian: ${inferenceTime}ms"
            } else {
                binding.chipInferenceTime.visibility = View.GONE
            }
            checkPermissionAndGetLocation()
        }

        when {
            imageUri != null -> Glide.with(this).load(imageUri).into(binding.ivResultImage)
            imageResId != -1 -> binding.ivResultImage.setImageResource(imageResId)
        }

        if (predictedName != null) {
            // Vẫn dùng tên Tiếng Anh gốc để lấy dữ liệu từ Collection pest_control_measures
            fetchDetailsFromFirestore(predictedName)
        }

        binding.cardDistribution.root.setOnClickListener {
            if (predictedName != null) {
                showCommunityMapDialog(predictedName)
            }
        }

        binding.tilLocation.setEndIconOnClickListener {
            checkPermissionAndGetLocation()
        }

        binding.btnShare.setOnClickListener {
            shareResult()
        }

        binding.btnCorrect.setOnClickListener {
            if (imageUri != null && predictedName != null) {
                uploadFeedbackToFirebase(
                    imageUri = imageUri,
                    predictedLabel = predictedName,
                    isCorrect = true,
                    confidenceScore = confidence,
                    correctLabel = ""
                )
                binding.btnCorrect.isEnabled = false
                binding.btnWrong.isEnabled = false
                Toast.makeText(this, "Đã ghi nhận đóng góp của bạn!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Lỗi: Không tìm thấy ảnh hoặc tên!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnWrong.setOnClickListener {
            binding.layoutCorrection.visibility = View.VISIBLE
        }

        binding.btnSubmitFeedback.setOnClickListener {
            val correctName = binding.etCorrectName.text.toString().trim()

            if (correctName.isEmpty()) {
                binding.etCorrectName.error = "Vui lòng nhập tên côn trùng đúng"
                binding.etCorrectName.requestFocus()
                return@setOnClickListener
            }

            if (imageUri != null && predictedName != null) {
                uploadFeedbackToFirebase(
                    imageUri = imageUri,
                    predictedLabel = predictedName,
                    isCorrect = false,
                    confidenceScore = confidence,
                    correctLabel = correctName
                )

                binding.layoutCorrection.visibility = View.GONE
                binding.etCorrectName.text.clear()
                binding.btnCorrect.isEnabled = false
                binding.btnWrong.isEnabled = false
                Toast.makeText(this, "Tuyệt vời! Dữ liệu sẽ được dùng để cải thiện AI.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSaveNotebook.setOnClickListener {
            val location = binding.edtLocation.text.toString().trim()
            val note = binding.edtNote.text.toString().trim()
            val isContribute = binding.cbContribute.isChecked

            if (predictedName != null && imageUri != null) {
                saveToHistoryAndContribute(predictedName, imageUri, confidence, location, note, isContribute)
            } else {
                Toast.makeText(this, "Lỗi: Không tìm thấy ảnh hoặc tên", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCommunityMapDialog(insectName: String) {
        // CHỈNH SỬA: Chuyển tên sang tiếng Việt trước khi truy vấn vì Database đã lưu bằng tiếng Việt
        val vnName = translateToVietnamese(insectName)
        Toast.makeText(this, "Đang truy vấn hệ thống cảnh báo...", Toast.LENGTH_SHORT).show()

        db.collection("contributions")
            .whereEqualTo("insectName", vnName) // Tìm bằng tiếng Việt
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    AlertDialog.Builder(this)
                        .setTitle("🌍 Cảnh báo Cộng đồng")
                        .setMessage("Hiện tại chưa có ghi nhận nào từ cộng đồng về loài này. Bạn là người đầu tiên phát hiện!")
                        .setPositiveButton("Đóng", null)
                        .show()
                    return@addOnSuccessListener
                }

                val sortedDocs = documents.sortedByDescending { it.getTimestamp("timestamp") }
                val builder = java.lang.StringBuilder("Dữ liệu cảnh báo từ cộng đồng:\n\n")
                var count = 0

                for (doc in sortedDocs) {
                    val loc = doc.getString("location") ?: ""
                    if (loc.isNotEmpty() && loc != "Không xác định") {
                        val time = doc.getTimestamp("timestamp")?.toDate()
                        val timeStr = time?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it) } ?: ""

                        builder.append("📍 $loc\n   🕒 $timeStr\n\n")
                        count++
                    }
                    if (count >= 5) break
                }

                if (count == 0) {
                    builder.append("Có ${documents.size()} báo cáo nhưng chưa có thông tin vị trí cụ thể.\n\n")
                }

                builder.append("📊 Tổng số ca ghi nhận trên hệ thống: ${documents.size()}")

                AlertDialog.Builder(this)
                    .setTitle("🌍 Bản đồ Dịch hại: $vnName")
                    .setMessage(builder.toString())
                    .setPositiveButton("Đóng", null)
                    .show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi truy vấn cộng đồng: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToHistoryAndContribute(name: String, uri: Uri, confidence: Float, location: String, note: String, isContribute: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val fileName = "history_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child("history/$userId/$fileName")

        binding.btnSaveNotebook.isEnabled = false
        binding.btnSaveNotebook.text = "Đang tải dữ liệu..."

        storageRef.putFile(uri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->

                // CHỈNH SỬA: Chuyển tên sang tiếng Việt trước khi lưu vào Firebase
                val vnName = translateToVietnamese(name)

                val recordData = hashMapOf(
                    "userId" to userId,
                    "insectName" to vnName, // Lưu bằng tiếng Việt
                    "imageUrl" to downloadUrl.toString(),
                    "confidence" to confidence.toDouble(),
                    "location" to if (location.isEmpty()) "Không xác định" else location,
                    "latitude" to (currentLat ?: 0.0),
                    "longitude" to (currentLng ?: 0.0),
                    "note" to note,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                db.collection("users").document(userId).collection("history")
                    .add(recordData)
                    .addOnSuccessListener {
                        if (isContribute) {
                            db.collection("contributions").add(recordData)
                        }
                        Toast.makeText(this, "Đã lưu thành công!", Toast.LENGTH_SHORT).show()
                        binding.btnSaveNotebook.text = "ĐÃ LƯU"
                        binding.btnSaveNotebook.setBackgroundColor(Color.GRAY)
                    }
                    .addOnFailureListener {
                        binding.btnSaveNotebook.isEnabled = true
                        binding.btnSaveNotebook.text = "LƯU VÀO SỔ TAY"
                    }
            }
        }.addOnFailureListener {
            binding.btnSaveNotebook.isEnabled = true
            binding.btnSaveNotebook.text = "LƯU VÀO SỔ TAY"
        }
    }

    private fun uploadFeedbackToFirebase(
        imageUri: Uri,
        predictedLabel: String,
        isCorrect: Boolean,
        confidenceScore: Float,
        correctLabel: String = ""
    ) {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("feedback_images/${System.currentTimeMillis()}.png")
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        storageRef.putFile(imageUri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->

                val feedbackData = hashMapOf(
                    "imageUrl" to downloadUrl.toString(),
                    "predictedLabel" to predictedLabel, // Lưu tiếng Anh để đội AI dễ train lại
                    "isCorrect" to isCorrect,
                    "correctLabel" to if (isCorrect) predictedLabel else correctLabel,
                    "confidence" to confidenceScore,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                db.collection("ai_feedbacks")
                    .add(feedbackData)
                    .addOnSuccessListener {
                        android.widget.Toast.makeText(this, "Cảm ơn bạn đã góp phần cải thiện AI!", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun shareResult() {
        val insectName = binding.tvVietnameseName.text.toString()
        val confidenceText = binding.chipConfidence.text.toString()
        var shareText = "Tôi vừa phát hiện ra: $insectName ($confidenceText) thông qua ứng dụng InsectID! \uD83D\uDC1E\uD83C\uDF3F\nCùng tải app để bảo vệ mùa màng nhé!"

        if (currentLat != null && currentLng != null) {
            val mapLink = "https://www.google.com/maps/search/?api=1&query=$currentLat,$currentLng"
            shareText += "\n\uD83D\uDCCD Vị trí phát hiện: $mapLink"
        }

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Kết quả nhận diện côn trùng")
        intent.putExtra(Intent.EXTRA_TEXT, shareText)

        try {
            val drawable = binding.ivResultImage.drawable
            if (drawable is android.graphics.drawable.BitmapDrawable) {
                val bitmap = drawable.bitmap
                val path = android.provider.MediaStore.Images.Media.insertImage(
                    contentResolver, bitmap, "InsectID_${System.currentTimeMillis()}", null
                )
                if (path != null) {
                    val uri = Uri.parse(path)
                    intent.type = "image/*"
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                }
            }
        } catch (e: Exception) {
            Log.e("ShareError", "Lỗi đính kèm ảnh: ${e.message}")
        }

        startActivity(Intent.createChooser(intent, "Chia sẻ kết quả qua..."))
    }

    private fun checkPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        binding.edtLocation.setText("Đang lấy vị trí...")

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude

                    try {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            binding.edtLocation.setText(addresses[0].getAddressLine(0))
                        } else {
                            binding.edtLocation.setText("Tọa độ: ${location.latitude}, ${location.longitude}")
                        }
                    } catch (e: Exception) {
                        binding.edtLocation.setText("Tọa độ: ${location.latitude}, ${location.longitude}")
                    }
                } else {
                    binding.edtLocation.setText("")
                }
            }
        } catch (e: Exception) {
            binding.edtLocation.setText("")
        }
    }

    private fun fetchDetailsFromFirestore(name: String) {
        setupCard(binding.cardScientifit, "Tên khoa học \uD83E\uDDEC", "Đang tải...")
        setupCard(binding.cardDistribution, "Phân bố (Bấm xem Bản đồ \uD83C\uDF0D)", "Đang tải...")
        setupCard(binding.cardMorphology, "Hình thái \uD83D\uDD2C", "Đang tải...")
        setupCard(binding.cardImpact, "Tác hại ⚠\uFE0F", "Đang tải...")
        setupCard(binding.cardMeasures, "Biện pháp \uD83D\uDEE1\uFE0F", "Đang tải...")

        db.collection("pest_control_measures").whereEqualTo("name", name).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    binding.tvVietnameseName.text = name
                } else {
                    val doc = docs.first()
                    binding.tvVietnameseName.text = doc.getString("vietnamese_name") ?: name
                    updateCard(binding.cardScientifit, doc.getString("scientific_name"))
                    updateCard(binding.cardMorphology, doc.getString("morphology"))
                    updateCard(binding.cardDistribution, doc.getString("distribution"))
                    updateCard(binding.cardImpact,  doc.getString("disadvantage"))
                    updateCard(binding.cardMeasures, doc.getString("measure"))

                    // === XỬ LÝ LẤY MẢNG VÒNG ĐỜI (LIFESTAGES) AN TOÀN ===
                    val lifeStagesList = mutableListOf<LifeStage>()

                    try {
                        val lifeStagesRaw = doc.get("lifeStages") as? List<*>

                        if (lifeStagesRaw != null) {
                            for (item in lifeStagesRaw) {
                                if (item is Map<*, *>) {
                                    val stage = item["stage"]?.toString() ?: ""
                                    val url = item["url"]?.toString() ?: ""

                                    if (stage.isNotEmpty() && url.isNotEmpty()) {
                                        lifeStagesList.add(LifeStage(stage, url))
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseData", "Lỗi phân tích Firebase: ${e.message}")
                    }

                    setupLifeStagesUI(lifeStagesList)
                }
            }
    }

    private fun setupLifeStagesUI(lifeStages: List<LifeStage>) {
        binding.layout3Stages.visibility = View.GONE
        binding.layout4Stages.visibility = View.GONE
        binding.rvLifeStages.visibility = View.GONE

        when (lifeStages.size) {
            3 -> {
                binding.layout3Stages.visibility = View.VISIBLE

                binding.tv31.text = lifeStages[0].stage
                Glide.with(this).load(lifeStages[0].url).circleCrop().into(binding.img31)

                binding.tv32.text = lifeStages[1].stage
                Glide.with(this).load(lifeStages[1].url).circleCrop().into(binding.img32)

                binding.tv33.text = lifeStages[2].stage
                Glide.with(this).load(lifeStages[2].url).circleCrop().into(binding.img33)
            }
            4 -> {
                binding.layout4Stages.visibility = View.VISIBLE

                binding.tv41.text = lifeStages[0].stage
                Glide.with(this).load(lifeStages[0].url).circleCrop().into(binding.img41)

                binding.tv42.text = lifeStages[1].stage
                Glide.with(this).load(lifeStages[1].url).circleCrop().into(binding.img42)

                binding.tv43.text = lifeStages[2].stage
                Glide.with(this).load(lifeStages[2].url).circleCrop().into(binding.img43)

                binding.tv44.text = lifeStages[3].stage
                Glide.with(this).load(lifeStages[3].url).circleCrop().into(binding.img44)
            }
            else -> {
                if(lifeStages.isNotEmpty()) {
                    binding.rvLifeStages.visibility = View.VISIBLE
                    binding.rvLifeStages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    binding.rvLifeStages.adapter = LifeStageAdapter(lifeStages)
                }
            }
        }
    }

    private fun setupCard(card: ItemInfoCardBinding, title: String, content: String) {
        card.tvTitle.text = title
        card.tvContent.text = content
    }

    private fun updateCard(card: ItemInfoCardBinding, content: String?) {
        card.tvContent.text = content ?: "Chưa có thông tin"
    }

    // ====================================================================
    // HÀM CHUYỂN ĐỔI NGÔN NGỮ (Nhựt thêm các loài khác vào đây nhé)
    // ====================================================================
    private fun translateToVietnamese(englishName: String): String {
        return when (englishName.lowercase().trim()) {
            "rice leaf roller" -> "Sâu cuốn lá lúa"
            "rice leaf caterpillar" -> "Sâu ăn lá lúa"
            "rice water weevil" -> "Mọt nước hại lúa"
            "sternochetus frigidus" -> "Mọt xoài (sternochetus frigidus)"
            "cicadellidae" -> "Họ rầy lá"
            "rice leafhopper" -> "Rầy lá lúa"
            "grain spreader thrips" -> "Bọ trĩ hại ngũ cốc"
            "rice shell pest" -> "Sâu hại vỏ trấu"
            "grub" -> "Ấu trùng bọ cánh cứng"
            "mole cricket" -> "Dế dũi"
            "wireworm" -> "Sâu kim"
            "white margined moth" -> "Bướm viền trắng"
            "black cutworm" -> "Sâu xám đen"
            "large cutworm" -> "Sâu xám lớn"
            "paddy stem maggot" -> "Sâu đục thân lúa"
            "yellow cutworm" -> "Sâu xám vàng"
            "red spider" -> "Nhện đỏ"
            "corn borer" -> "Sâu đục thân ngô"
            "army worm" -> "Sâu keo mùa thu"
            "aphids" -> "Rệp"
            "potosiabre vitarsis" -> "Bọ cánh cứng (potosiabre vitarsis)"
            "peach borer" -> "Sâu đục thân đào"
            "english grain aphid" -> "Rệp lúa mì Anh"
            "green bug" -> "Rệp xanh"
            "bird cherry-oataphid" -> "Rệp yến mạch"
            "asiatic rice borer" -> "Sâu đục thân lúa châu Á"
            "wheat blossom midge" -> "Ruồi hại hoa lúa mì"
            "penthaleus major" -> "Nhện đất (penthaleus major)"
            "longlegged spider mite" -> "Nhện dài chân"
            "wheat phloeothrips" -> "Bọ trĩ lúa mì"
            "wheat sawfly" -> "Ong cưa lúa mì"
            "cerodonta denticornis" -> "Ruồi đục lá"
            "beet fly" -> "Ruồi hại củ cải"
            "flea beetle" -> "Bọ nhảy"
            "cabbage army worm" -> "Sâu keo bắp cải"
            "beet army worm" -> "Sâu keo củ cải"
            "yellow rice borer" -> "Sâu đục thân lúa vàng"
            "beet spot flies" -> "Ruồi đốm củ cải"
            "meadow moth" -> "Bướm đồng cỏ"
            "beet weevil" -> "Mọt củ cải"
            "sericaorient alismots chulsky" -> "Bọ cánh cứng (sericaorient...)"
            "alfalfa weevil" -> "Mọt cỏ linh lăng"
            "flax budworm" -> "Sâu nụ lanh"
            "alfalfa plant bug" -> "Bọ xít cỏ linh lăng"
            "tarnished plant bug" -> "Bọ xít hại cây"
            "locustoidea" -> "Châu chấu"
            "lytta polita" -> "Bọ ban miêu"
            "rice gall midge" -> "Muỗi hành lúa"
            "legume blister beetle" -> "Bọ ban miêu họ đậu"
            "blister beetle" -> "Bọ ban miêu"
            "therioaphis maculata buckton" -> "Rệp đốm"
            "odontothrips loti" -> "Bọ trĩ"
            "thrips" -> "Bọ trĩ"
            "alfalfa seed chalcid" -> "Ong ký sinh hạt cỏ linh lăng"
            "pieris canidia" -> "Bướm trắng"
            "apolygus lucorum" -> "Bọ xít xanh"
            "limacodidae" -> "Họ sâu róm"
            "viteus vitifoliae" -> "Rệp nho"
            "rice stemfly" -> "Ruồi thân lúa"
            "colomerus vitis" -> "Nhện nho"
            "brevipoalpus lewisi mcGregor" -> "Nhện đỏ nhỏ"
            "oides decempunctata" -> "Bọ ăn lá"
            "polyphagotars onemus latus" -> "Nhện rộng"
            "pseudococcus comstocki kuwana" -> "Rệp sáp"
            "parathrene regalis" -> "Sâu đục thân"
            "ampelophaga" -> "Sâu ăn lá nho"
            "lycorma delicatula" -> "Rầy đèn"
            "xylotrechus" -> "Sâu đục gỗ"
            "cicadella viridis" -> "Rầy xanh"
            "brown plant hopper" -> "Rầy nâu"
            "miridae" -> "Họ bọ xít"
            "trialeurodes vaporariorum" -> "Bọ phấn trắng"
            "erythroneura apicalis" -> "Rầy lá nho"
            "papilio xuthus" -> "Bướm phượng"
            "panonchus citri McGregor" -> "Nhện đỏ cam quýt"
            "phyllocoptes oleiverus ashmead" -> "Nhện hại cam"
            "icerya purchasi maskell" -> "Rệp sáp bông"
            "unaspis yanonensis" -> "Rệp vảy"
            "ceroplastes rubens" -> "Rệp sáp đỏ"
            "chrysomphalus aonidum" -> "Rệp vảy cam"
            "white backed plant hopper" -> "Rầy lưng trắng"
            "parlatoria zizyphus lucus" -> "Rệp vảy táo tàu"
            "nipaecoccus vastalor" -> "Rệp sáp"
            "aleurocanthus spiniferus" -> "Bọ phấn gai đen"
            "tetradacus c bactrocera minax" -> "Ruồi đục quả"
            "dacus dorsalis(Hendel)" -> "Ruồi đục quả phương Đông"
            "bactrocera tsuneonis" -> "Ruồi đục quả"
            "prodenia litura" -> "Sâu khoang"
            "adristyrannus" -> "Côn trùng hại"
            "phyllocnistis citrella Stainton" -> "Sâu vẽ bùa cam"
            "toxoptera citricidus" -> "Rệp cam"
            "small brown plant hopper" -> "Rầy nâu nhỏ"
            "toxoptera aurantii" -> "Rệp đen"
            "aphis citricola Vander Goot" -> "Rệp cam"
            "scirtothrips dorsalis Hood" -> "Bọ trĩ"
            "dasineura sp" -> "Muỗi gây u sưng"
            "lawana imitata Melichar" -> "Rầy"
            "salurnis marginella Guerr" -> "Rầy"
            "deporaus marginatus Pascoe" -> "Bọ vòi voi"
            "chlumetia transversa" -> "Sâu đục thân"
            "mango flat beak leafhopper" -> "Rầy xoài"
            "rhytidodera bowrinii white" -> "Bọ cánh cứng"
            else -> englishName
        }
    }
}