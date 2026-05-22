package com.example.khoaluan

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.khoaluan.databinding.ActivityAddArticleBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddArticleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddArticleBinding
    private var selectedImageUri: Uri? = null

    // Mở thư viện chọn ảnh
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivCoverImage.setImageURI(uri)
            binding.ivCoverImage.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bấm vào ảnh để chọn ảnh
        binding.ivCoverImage.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnPostArticle.setOnClickListener {
            val title = binding.edtTitle.text.toString().trim()
            val summary = binding.edtSummary.text.toString().trim()
            val content = binding.edtContent.text.toString().trim()

            if (title.isEmpty() || content.isEmpty() || selectedImageUri == null) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin và chọn ảnh!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadArticle(title, summary, content, selectedImageUri!!)
        }
    }

    private fun uploadArticle(title: String, summary: String, content: String, imageUri: Uri) {
        binding.btnPostArticle.isEnabled = false
        binding.btnPostArticle.text = "Đang tải lên..."

        val fileName = "articles/${UUID.randomUUID()}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child(fileName)

        // 1. Tải ảnh lên Storage
        storageRef.putFile(imageUri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->

                // 2. Lưu thông tin bài viết vào Firestore
                val article = hashMapOf(
                    "title" to title,
                    "summary" to summary,
                    "content" to content,
                    "imageUrl" to downloadUrl.toString(),
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                FirebaseFirestore.getInstance().collection("articles")
                    .add(article)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show()
                        finish() // Đóng màn hình quay về Dashboard
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                        resetButton()
                    }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Lỗi tải ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
            resetButton()
        }
    }

    private fun resetButton() {
        binding.btnPostArticle.isEnabled = true
        binding.btnPostArticle.text = "ĐĂNG BÀI"
    }
}