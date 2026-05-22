package com.example.khoaluan

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.khoaluan.databinding.ActivityGuideBinding

class GuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Xử lý nút quay lại
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}