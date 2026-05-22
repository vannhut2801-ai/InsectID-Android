package com.example.khoaluan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class LifeStageAdapter(private val lifeStages: List<LifeStage>) :
    RecyclerView.Adapter<LifeStageAdapter.LifeStageViewHolder>() {

    class LifeStageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgStage: ImageView = itemView.findViewById(R.id.imgStage)
        val tvStageName: TextView = itemView.findViewById(R.id.tvStage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LifeStageViewHolder {
        // R.layout.item_life_stage chính là file XML bạn đã tạo
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_life_stage, parent, false)
        return LifeStageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LifeStageViewHolder, position: Int) {
        val currentStage = lifeStages[position]

        // Đã sửa lại đúng tên biến tvStageName
        holder.tvStageName.text = currentStage.stage

        // === SỬ DỤNG GLIDE ĐỂ BO TRÒN HÌNH ẢNH (CircleCrop) ===
        Glide.with(holder.itemView.context)
            .load(currentStage.url)
            .circleCrop() // <-- Lệnh xịn xò giúp cắt ảnh thành hình tròn
            .into(holder.imgStage) // Tạm thời bỏ các dòng placeholder đi cho đỡ báo lỗi đỏ
    }

    override fun getItemCount(): Int {
        return lifeStages.size
    }
}