package com.example.khoaluan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.khoaluan.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Hiển thị email người dùng
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.tvUserEmail.text = currentUser.email
        } else {
            binding.tvUserEmail.text = "Chưa đăng nhập"
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnGuide.setOnClickListener {    // Chuyển sang Activity hướng dẫn (Ví dụ bạn đặt tên là GuideActivity)
            val intent = Intent(this, GuideActivity::class.java)
            startActivity(intent)
        }
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            // Chuyển về màn hình Login và xóa toàn bộ lịch sử màn hình trước đó
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}