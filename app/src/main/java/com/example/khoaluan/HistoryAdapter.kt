package com.example.khoaluan

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.khoaluan.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(private val items: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryItem) {
            binding.tvHistoryName.text = item.insectName
            binding.tvHistoryDate.text = dateFormatter.format(item.timestamp.toDate())

            Glide.with(binding.root.context)
                .load(item.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivHistoryImage)

            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, ResultActivity::class.java).apply {
                    putExtra("PREDICTED_NAME", item.insectName)
                    putExtra("IMAGE_URI", item.imageUrl)
                    putExtra("CONFIDENCE", item.confidence)
                }
                context.startActivity(intent)
            }
        }
    }
}