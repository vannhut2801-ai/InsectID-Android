package com.example.khoaluan

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.khoaluan.databinding.ActivityArticleDetailBinding

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val title = intent.getStringExtra("TITLE") ?: "Bài viết"
        val content = intent.getStringExtra("CONTENT") ?: "Chưa có nội dung"
        val imageUrl = intent.getStringExtra("IMAGE_URL")

        binding.tvTitle.text = title
        binding.tvContent.text = content

        Log.d("ArticleDetail", "Link ảnh nhận được: $imageUrl")

        if (!imageUrl.isNullOrEmpty() && imageUrl != "null") {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>, // Thường không có dấu ? ở Target trong bản mới
                        isFirstResource: Boolean
                    ): Boolean {
                        Toast.makeText(this@ArticleDetailActivity, "Lỗi tải ảnh!", Toast.LENGTH_SHORT).show()
                        Log.e("ArticleDetail", "Glide Error: ${e?.message}")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .error(android.R.drawable.stat_notify_error)
                .into(binding.ivHeader)
        } else {
            binding.ivHeader.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }
}