package com.example.khoaluan // Thay đổi theo đúng tên package của bạn

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.khoaluan.R
import com.example.khoaluan.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mMap: GoogleMap
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance()

        // Nạp Google Maps vào Fragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Tọa độ mặc định: Long Xuyên, An Giang (Khu vực trọng điểm của đồ án)
        val anGiang = LatLng(10.3759, 105.4358)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(anGiang, 10f))

        // Kích hoạt nút Zoom và La bàn mặc định của Google Maps
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        Toast.makeText(requireContext(), "Đang tải dữ liệu dịch hại từ cộng đồng...", Toast.LENGTH_SHORT).show()

        // Gọi hàm tải dữ liệu và vẽ Marker
        loadCommunityContributions()
    }

    private fun loadCommunityContributions() {
        db.collection("contributions")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "Hiện chưa có dữ liệu dịch hại nào được ghi nhận.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val lat = document.getDouble("latitude")
                    val lng = document.getDouble("longitude")
                    val insectName = document.getString("insectName") ?: "Không rõ"
                    val imageUrl = document.getString("imageUrl") ?: ""

                    // Bỏ qua nếu không có tọa độ hợp lệ
                    if (lat == null || lng == null || lat == 0.0 || lng == 0.0) continue

                    val position = LatLng(lat, lng)

                    // Dùng Glide tải ảnh côn trùng từ Firebase Storage về
                    Glide.with(requireContext())
                        .asBitmap()
                        .load(imageUrl)
                        .circleCrop() // Bo tròn ảnh tự động
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                // Nếu Activity/Fragment đã bị đóng thì không vẽ nữa để tránh lỗi crash
                                if (!isAdded || context == null) return

                                // Tạo Marker tùy chỉnh
                                val customMarkerBitmap = createCustomMarkerBitmap(requireContext(), resource, insectName)

                                // Gắn Marker lên bản đồ
                                mMap.addMarker(
                                    MarkerOptions()
                                        .position(position)
                                        .title("Phát hiện: $insectName")
                                        .snippet("Bấm để xem chi tiết")
                                        .icon(BitmapDescriptorFactory.fromBitmap(customMarkerBitmap))
                                )
                            }

                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                                // Không xử lý
                            }
                        })
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapFragment", "Lỗi khi tải dữ liệu bản đồ: ", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Hàm chuyển đổi Layout XML (custom_marker_layout) thành Bitmap để dán lên Bản đồ
    private fun createCustomMarkerBitmap(context: Context, insectBitmap: Bitmap, name: String): Bitmap {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_marker_layout, null)

        val ivMarkerImage = view.findViewById<ImageView>(R.id.ivMarkerImage)
        val tvMarkerName = view.findViewById<TextView>(R.id.tvMarkerName)

        ivMarkerImage.setImageBitmap(insectBitmap)
        tvMarkerName.text = name

        // Tính toán kích thước View
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // Vẽ View ra Bitmap
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}