package com.example.khoaluan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.khoaluan.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

// Lớp dữ liệu cho Dashboard và Bản đồ
data class Article(val title: String, val summary: String, val content: String, val imageUrl: String)
data class Category(val id: Int, val name: String, val iconRes: Int)
data class FeaturedInsect(val id: Int, val name: String, val description: String, val imageRes: Int)
data class PestCluster(val lat: Double, val lng: Double, val name: String, val imageUrl: String, var count: Int)

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    private val MODEL_INPUT_SIZE = 224
    private lateinit var imageProcessor: ImageProcessor

    // GPS & Bản đồ
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null

    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            fetchUserLocationAndInsects()
        } else {
            binding.tvRegionalTitle.text = "Côn trùng quanh bạn (Chưa bật GPS)"
            loadDefaultRegionalInsects()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = loadBitmapFromUri(it)
            if (bitmap != null) {
                val cacheUri = saveBitmapToCache(bitmap)
                runInference(bitmap, cacheUri)
            } else {
                Toast.makeText(this, "Không thể đọc ảnh này", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            val uri = saveBitmapToCache(it)
            runInference(it, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabChatAI.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        val builder = android.os.StrictMode.VmPolicy.Builder()
        android.os.StrictMode.setVmPolicy(builder.build())

        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.btnLogout.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        initTensorFlowLite()

        setupNavigation()
        setupDashboardLists()
        checkCameraPermission()
        checkAdminRole()
        fetchArticles()
        checkLocationPermission()

        // KHỞI TẠO BẢN ĐỒ GOOGLE MAPS
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    // ==========================================
    // XỬ LÝ THANH ĐIỀU HƯỚNG BÊN DƯỚI (BOTTOM NAV)
    // ==========================================
    private fun setupNavigation() {
        binding.fabCamera.setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val options = arrayOf("Chụp ảnh", "Thư viện", "Hủy")
                AlertDialog.Builder(this).setItems(options) { _, which ->
                    when (which) {
                        0 -> cameraLauncher.launch(null)
                        1 -> galleryLauncher.launch("image/*")
                    }
                }.show()
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền Camera", Toast.LENGTH_SHORT).show()
                checkCameraPermission()
            }
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.scrollViewHome.visibility = View.VISIBLE
                    binding.mapContainer.visibility = View.GONE
                    true
                }
                R.id.nav_map -> {
                    binding.scrollViewHome.visibility = View.GONE
                    binding.mapContainer.visibility = View.VISIBLE
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    false
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> false
            }
        }
        binding.bottomNavigationView.menu.findItem(R.id.placeholder)?.isEnabled = false
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val vietnam = LatLng(10.37, 105.43)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnam, 8f))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
        }

        // --- CẤU HÌNH INFO WINDOW (BẢNG THÔNG TIN CÓ ẢNH TỪ FIREBASE) ---
        googleMap?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {

            override fun getInfoWindow(marker: com.google.android.gms.maps.model.Marker): View? {
                return null
            }

            override fun getInfoContents(marker: com.google.android.gms.maps.model.Marker): View {
                val view = layoutInflater.inflate(R.layout.custom_info_window, null)
                val img = view.findViewById<ImageView>(R.id.imgInfoWindow)
                val tv = view.findViewById<TextView>(R.id.tvInfoWindowName)

                tv.text = marker.title
                val imageUrl = marker.tag as? String

                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(view.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.bo)
                        .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                            override fun onResourceReady(
                                resource: android.graphics.drawable.Drawable,
                                transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?
                            ) {
                                img.setImageDrawable(resource)
                                if (marker.isInfoWindowShown) {
                                    marker.showInfoWindow()
                                }
                            }
                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                                img.setImageDrawable(placeholder)
                            }
                        })
                } else {
                    img.setImageResource(R.drawable.bo)
                }
                return view
            }
        })

        fetchPestMapData()
    }

    // ==========================================
    // KHU VỰC XỬ LÝ BẢN ĐỒ DỊCH HẠI (CLUSTERING)
    // ==========================================
    private fun fetchPestMapData() {
        val db = FirebaseFirestore.getInstance()
        db.collection("contributions")
            .get()
            .addOnSuccessListener { result ->
                val clusterMap = HashMap<String, PestCluster>()

                // BƯỚC A: Gom nhóm dữ liệu theo Khu vực (bán kính ~1km) và Tên loài
                for (document in result) {
                    val lat = document.getDouble("latitude") ?: continue
                    val lng = document.getDouble("longitude") ?: continue
                    val insectName = document.getString("insectName") ?: "Côn trùng"
                    val imageUrl = document.getString("imageUrl") ?: ""

                    if (lat != 0.0 && lng != 0.0) {
                        // Làm tròn tọa độ 2 chữ số thập phân để gom các vị trí gần nhau
                        val roundedLat = String.format(Locale.US, "%.2f", lat)
                        val roundedLng = String.format(Locale.US, "%.2f", lng)
                        val key = "${roundedLat}_${roundedLng}_${insectName}"

                        if (clusterMap.containsKey(key)) {
                            clusterMap[key]!!.count += 1 // Tăng tần suất xuất hiện
                        } else {
                            clusterMap[key] = PestCluster(lat, lng, insectName, imageUrl, 1)
                        }
                    }
                }

                // BƯỚC B: Duyệt qua các nhóm đã gộp và vẽ lên bản đồ
                for (cluster in clusterMap.values) {
                    val position = LatLng(cluster.lat, cluster.lng)

                    // Quy định màu sắc theo tên côn trùng
                    val markerColor = when (cluster.name.lowercase(Locale.getDefault())) {
                        "brown plant hopper", "rầy nâu" -> android.graphics.Color.parseColor("#D32F2F") // Đỏ báo động
                        "rice leaf roller", "sâu cuốn lá lúa" -> android.graphics.Color.parseColor("#388E3C") // Xanh lá
                        "dacus dorsalis(hendel)", "ruồi đục quả" -> android.graphics.Color.parseColor("#FBC02D") // Cam vàng
                        else -> android.graphics.Color.parseColor("#1976D2") // Xanh dương mặc định
                    }

                    Glide.with(this@MainActivity)
                        .asBitmap()
                        .load(cluster.imageUrl)
                        .circleCrop()
                        .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                                if (isDestroyed) return

                                // Gọi hàm vẽ Marker truyền thêm Màu sắc và Tần suất
                                val customMarker = createCustomMarkerBitmap(this@MainActivity, resource, cluster.name, cluster.count, markerColor)

                                val marker = googleMap?.addMarker(
                                    MarkerOptions()
                                        .position(position)
                                        .title("${cluster.name} (Ghi nhận: ${cluster.count} ca)")
                                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(customMarker))
                                )
                                marker?.tag = cluster.imageUrl
                            }
                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                        })
                }
            }
            .addOnFailureListener {
                Log.e("MapData", "Lỗi khi tải dữ liệu bản đồ", it)
            }
    }

    private fun createCustomMarkerBitmap(context: android.content.Context, insectBitmap: Bitmap, name: String, count: Int, bgColor: Int): Bitmap {
        val view = android.view.LayoutInflater.from(context).inflate(R.layout.custom_marker_layout, null)

        val ivMarkerImage = view.findViewById<ImageView>(R.id.ivMarkerImage)
        val tvMarkerName = view.findViewById<TextView>(R.id.tvMarkerName)
        val cardMarker = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardMarker)
        val triangle = view.findViewById<View>(R.id.triangle)
        val tvCountBadge = view.findViewById<TextView>(R.id.tvCountBadge)

        // Set ảnh và tên
        ivMarkerImage.setImageBitmap(insectBitmap)
        tvMarkerName.text = name

        // Đổi màu viền và mũi nhọn
        cardMarker.setCardBackgroundColor(bgColor)
        triangle.setBackgroundColor(bgColor)

        // Tính toán kích thước và hiển thị số lượng nếu xuất hiện nhiều hơn 1
        var sizeDp = 50 // Kích thước gốc
        if (count > 1) {
            tvCountBadge.visibility = View.VISIBLE
            tvCountBadge.text = count.toString()

            // Tạo hình tròn màu Đỏ báo hiệu cho Badge
            val shape = android.graphics.drawable.GradientDrawable()
            shape.shape = android.graphics.drawable.GradientDrawable.OVAL
            shape.setColor(android.graphics.Color.RED)
            shape.setStroke(2, android.graphics.Color.WHITE)
            tvCountBadge.background = shape

            // Tăng kích thước ảnh thêm 10dp cho mỗi ca, tối đa to đến 80dp
            sizeDp = (50 + (count * 5)).coerceAtMost(80)

            val layoutParams = cardMarker.layoutParams
            // Chuyển Dp sang Pixel
            val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()
            layoutParams.width = sizePx
            layoutParams.height = sizePx
            cardMarker.radius = sizePx / 2f
            cardMarker.layoutParams = layoutParams
        }

        // Vẽ ra màn hình
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }
    // ==========================================


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocationAndInsects()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchUserLocationAndInsects() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val province = addresses[0].adminArea ?: addresses[0].subAdminArea ?: "khu vực của bạn"
                        binding.tvRegionalTitle.text = "Phổ biến tại $province"
                        loadInsectsForRegion(province)
                    } else {
                        binding.tvRegionalTitle.text = "Côn trùng phổ biến"
                        loadDefaultRegionalInsects()
                    }
                } catch (e: Exception) {
                    binding.tvRegionalTitle.text = "Côn trùng phổ biến"
                    loadDefaultRegionalInsects()
                }
            } else {
                binding.tvRegionalTitle.text = "Côn trùng quanh bạn (Đang định vị...)"
                loadDefaultRegionalInsects()
            }
        }.addOnFailureListener {
            loadDefaultRegionalInsects()
        }
    }

    private fun loadInsectsForRegion(region: String) {
        val regionalInsects = mutableListOf<FeaturedInsect>()
        val regionLower = region.lowercase(Locale.getDefault())

        if (regionLower.contains("an giang") || regionLower.contains("đồng tháp") ||
            regionLower.contains("cần thơ") || regionLower.contains("kiên giang") ||
            regionLower.contains("long an") || regionLower.contains("thái bình") ||
            regionLower.contains("nam định")) {

            regionalInsects.add(FeaturedInsect(1, "brown plant hopper", "Rầy nâu", R.drawable.ray_nau))
            regionalInsects.add(FeaturedInsect(2, "rice leaf roller", "Sâu cuốn lá lúa", R.drawable.sau_cuon_la))
            regionalInsects.add(FeaturedInsect(3, "rice gall midge", "Muỗi hành", R.drawable.muoi_hanh))
            regionalInsects.add(FeaturedInsect(4, "yellow rice borer", "Sâu đục thân lúa", R.drawable.sau_duc_than_lua_chau_a))
            regionalInsects.add(FeaturedInsect(5, "white backed plant hopper", "Rầy lưng trắng", R.drawable.ray_lung_trang))
        }
        else if (regionLower.contains("bến tre") || regionLower.contains("tiền giang") ||
            regionLower.contains("vĩnh long") || regionLower.contains("hậu giang") ||
            regionLower.contains("trà vinh")) {

            regionalInsects.add(FeaturedInsect(6, "dacus dorsalis", "Ruồi đục quả", R.drawable.ruoi_duc_qua))
            regionalInsects.add(FeaturedInsect(7, "mango flat beak leafhopper", "Làm rụng bông, đen nụ xoài", R.drawable.ray_dau_bet_hai_xoai))
            regionalInsects.add(FeaturedInsect(8, "panonchus citri McGregor", "Nhện đỏ", R.drawable.nhen_do))
            regionalInsects.add(FeaturedInsect(9, "phyllocnistis citrella stainton", "Sâu vẽ bùa", R.drawable.sau_ve_bua))
        }
        else if (regionLower.contains("lâm đồng") || regionLower.contains("đắk lắk") ||
            regionLower.contains("gia lai") || regionLower.contains("bình phước") ||
            regionLower.contains("đắk nông") || regionLower.contains("đồng nai")) {

            regionalInsects.add(FeaturedInsect(10, "scirtothrips dorsalis Hood", "Bọ trĩ", R.drawable.botri))
            regionalInsects.add(FeaturedInsect(11, "prodenia litura", "Sâu khoang", R.drawable.sau_khoan))
            regionalInsects.add(FeaturedInsect(12, "toxoptera citricidus", "Rệp sáp hại cam, quyết", R.drawable.rep_sap))
            regionalInsects.add(FeaturedInsect(13, "aphids", "Rệp", R.drawable.rep))
        }
        else if (regionLower.contains("sơn la") || regionLower.contains("điện biên") ||
            regionLower.contains("hòa bình") || regionLower.contains("hà giang") ||
            regionLower.contains("lào cai")) {

            regionalInsects.add(FeaturedInsect(14, "corn borer", "Sâu đục thân ngô", R.drawable.sau_duc_than_ngo))
            regionalInsects.add(FeaturedInsect(15, "cabbage army worm", "Sâu xanh bắp cải", R.drawable.sau_keo_hai_bap_cai))
            regionalInsects.add(FeaturedInsect(16, "flea beetle", "Bọ nhảy", R.drawable.bo_nhay))
        }
        else {
            loadDefaultRegionalInsects()
            return
        }

        binding.rvRegionalInsects.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvRegionalInsects.adapter = FeaturedInsectAdapter(regionalInsects, isHorizontal = true)
    }

    private fun loadDefaultRegionalInsects() {
        val defaultList = listOf(
            FeaturedInsect(1, "Rầy nâu", "Dịch hại lúa nguy hiểm", R.drawable.ray_lung_trang),
            FeaturedInsect(2, "Sâu cuốn lá lúa", "Gây hại đẻ nhánh", R.drawable.sau_cuon_la),
            FeaturedInsect(3, "Ruồi đục quả", "Gây rụng quả", R.drawable.ruoi),
            FeaturedInsect(4, "Sâu đục thân ngô", "Hại bắp ngô", R.drawable.sau_cuon_la)
        )
        binding.rvRegionalInsects.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvRegionalInsects.adapter = FeaturedInsectAdapter(defaultList, isHorizontal = true)
    }

    private fun checkAdminRole() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.email == "admin@gmail.com") {
            binding.btnAddArticle.visibility = View.VISIBLE
            binding.btnAddArticle.setOnClickListener {
                startActivity(Intent(this, AddArticleActivity::class.java))
            }
        } else {
            binding.btnAddArticle.visibility = View.GONE
        }
    }

    private fun fetchArticles() {
        val db = FirebaseFirestore.getInstance()
        db.collection("articles")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val articlesList = mutableListOf<Article>()
                for (document in result) {
                    val article = Article(
                        title = document.getString("title") ?: "",
                        summary = document.getString("summary") ?: "",
                        content = document.getString("content") ?: "",
                        imageUrl = document.getString("imageUrl") ?: ""
                    )
                    articlesList.add(article)
                }
                binding.rvArticles.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                binding.rvArticles.adapter = ArticleAdapter(articlesList)
            }
    }

    private fun checkCameraPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(this.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            }
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) { null }
    }

    private fun setupDashboardLists() {
        val categories = listOf(
            Category(1, "Phổ biến nhất", R.drawable.buom),
            Category(2, "Bọ cánh cứng", R.drawable.bo),
            Category(3, "Bọ xít", R.drawable.boxit),
            Category(4, "Ruồi", R.drawable.ruoi),
            Category(5, "Châu chấu", R.drawable.chauchau),
            Category(6, "Nhện", R.drawable.nhen),
            Category(7, "Bọ trĩ", R.drawable.botri),
            Category(8, "Ong", R.drawable.kien),
            Category(9 ,"Rệp", R.drawable.rep)
        )
        binding.rvCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.adapter = CategoryAdapter(categories)

        val featuredInsects = listOf(
            FeaturedInsect(1, "brown plant hopper", "Rầy nâu", R.drawable.ray_nau),
            FeaturedInsect(2, "prodenia litura", "Sâu khoang", R.drawable.sau_khoan),
            FeaturedInsect(3, "dacus dorsalis(Hendel)", "Ruồi đục quả", R.drawable.ruoi_duc_qua),
            FeaturedInsect(4, "apolygus lucorum", "Bọ xít xanh", R.drawable.bo_xit_xanh)
        )
        binding.rvFeatured.layoutManager = GridLayoutManager(this, 2)
        binding.rvFeatured.adapter = FeaturedInsectAdapter(featuredInsects)
        binding.rvFeatured.isNestedScrollingEnabled = false
    }

    private fun initTensorFlowLiteFromFirebase() {
        val conditions = com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions.Builder()
            .requireWifi()
            .build()

        com.google.firebase.ml.modeldownloader.FirebaseModelDownloader.getInstance()
            .getModel("insect_classifier", com.google.firebase.ml.modeldownloader.DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
            .addOnSuccessListener { model ->
                try {
                    val modelFile = model.file
                    if (modelFile != null) {
                        android.util.Log.d("ML", "Tuyệt vời! Đã nạp AI mới nhất từ hệ thống.")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ML", "Lỗi khi đọc file mới tải về", e)
                }
            }
            .addOnFailureListener {
                android.util.Log.e("ML", "Không có mạng hoặc lỗi tải. Hãy tiếp tục dùng file AI mặc định trong máy.", it)
            }
    }

    private fun initTensorFlowLite() {
        try {
            // 1. Nạp danh sách tên côn trùng (Kiểm tra xem file của em tên là labels.txt hay classes.txt nhé)
            labels = FileUtil.loadLabels(this, "labels.txt")
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .build()

            // 2. Nạp file AI Offline trực tiếp
            val localModel = FileUtil.loadMappedFile(this, "insect_classifier.tflite")

            // Cấu hình chạy 4 luồng cho nhanh
            val options = Interpreter.Options().apply { setNumThreads(4) }
            interpreter = Interpreter(localModel, options)

            Log.d("ML", "Đã nạp AI Offline thành công!")
        } catch (e: Exception) {
            Log.e("TFLite", "Lỗi nạp AI", e)
            Toast.makeText(this, "Lỗi nạp mô hình: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadLocalFallbackModel() {
        try {
            val localModel = FileUtil.loadMappedFile(this, "insect_classifier.tflite")
            interpreter = Interpreter(localModel)
            Log.d("ML", "Đã nạp AI Offline.")
        } catch (e: Exception) {
            Log.e("TFLite", "Lỗi nạp mô hình offline", e)
        }
    }

    private fun runInference(bitmap: Bitmap, imageUri: Uri?) {
        if (interpreter == null) return
        try {
            val finalBitmap = if (imageUri != null) {
                rotateBitmap(bitmap, imageUri)
            } else {
                bitmap
            }

            val correctedUri = saveBitmapToCache(finalBitmap)
            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(finalBitmap)
            tensorImage = imageProcessor.process(tensorImage)

            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, labels.size), DataType.FLOAT32)

            val startTime = android.os.SystemClock.uptimeMillis()
            interpreter?.run(tensorImage.buffer, outputBuffer.buffer.rewind())
            val endTime = android.os.SystemClock.uptimeMillis()
            val inferenceTime = endTime - startTime

            val probs = outputBuffer.floatArray
            val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0

            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("PREDICTED_NAME", labels[maxIdx])
                putExtra("CONFIDENCE", probs[maxIdx] * 100)
                putExtra("IMAGE_URI", correctedUri.toString())
                putExtra("INFERENCE_TIME", inferenceTime)
            }
            startActivity(intent)
        } catch (e: Exception) { Log.e("Inference", "Lỗi AI", e) }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val fileName = "temp_img_${System.currentTimeMillis()}.png"
        val file = File(cacheDir, fileName)
        try {
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Uri.fromFile(file)
    }

    private fun rotateBitmap(bitmap: Bitmap, imageUri: Uri): Bitmap {
        var inputStream = contentResolver.openInputStream(imageUri)
        val exif = androidx.exifinterface.media.ExifInterface(inputStream!!)
        val orientation = exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )
        inputStream.close()

        val matrix = android.graphics.Matrix()
        when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        interpreter?.close()
    }

    // --- ADAPTERS ---
    inner class CategoryAdapter(private val list: List<Category>) : RecyclerView.Adapter<CategoryAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.imgCategoryIcon)
            val text: TextView = v.findViewById(R.id.tvCategoryName)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_category, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, p: Int) {
            h.text.text = list[p].name
            h.icon.setImageResource(list[p].iconRes)
            h.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, CategoryDetailActivity::class.java)
                intent.putExtra("CATEGORY_NAME", list[p].name)
                startActivity(intent)
            }
        }
    }

    inner class FeaturedInsectAdapter(private val list: List<FeaturedInsect>, private val isHorizontal: Boolean = false) : RecyclerView.Adapter<FeaturedInsectAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgInsect)
            val title: TextView = v.findViewById(R.id.tvInsectName)
            val desc: TextView = v.findViewById(R.id.tvInsectDesc)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
            val view = LayoutInflater.from(p.context).inflate(R.layout.item_featured_insect, p, false)
            if (isHorizontal) {
                view.layoutParams = ViewGroup.LayoutParams(500, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            return VH(view)
        }
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, p: Int) {
            h.title.text = list[p].name
            h.desc.text = list[p].description
            h.img.setImageResource(list[p].imageRes)
            h.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, ResultActivity::class.java).apply {
                    putExtra("PREDICTED_NAME", list[p].name)
                    putExtra("CONFIDENCE", 100f)
                    putExtra("IMAGE_RES_ID", list[p].imageRes)
                }
                startActivity(intent)
            }
        }
    }

    inner class ArticleAdapter(private val list: List<Article>) : RecyclerView.Adapter<ArticleAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgArticle)
            val title: TextView = v.findViewById(R.id.tvArticleTitle)
            val summary: TextView = v.findViewById(R.id.tvArticleSummary)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_article, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, p: Int) {
            val article = list[p]
            h.title.text = article.title
            h.summary.text = article.summary

            Glide.with(h.itemView.context)
                .load(article.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(h.img)

            h.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, ArticleDetailActivity::class.java).apply {
                    putExtra("TITLE", article.title)
                    putExtra("CONTENT", article.content)
                    putExtra("IMAGE_URL", article.imageUrl)
                }
                startActivity(intent)
            }
        }
    }
}