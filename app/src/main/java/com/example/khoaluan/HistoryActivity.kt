package com.example.khoaluan

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.khoaluan.databinding.ActivityHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// ✅ BƯỚC 1: Đảm bảo lớp này có biến confidence: Float
data class HistoryItem(
    val insectName: String,
    val imageUrl: String,
    val timestamp: com.google.firebase.Timestamp,
    val confidence: Float // <--- Biến này phải có ở đây
)

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var historyAdapter: HistoryAdapter
    private val historyList = mutableListOf<HistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        fetchHistory()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(historyList)
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun fetchHistory() {
        binding.historyProgressBar.visibility = View.VISIBLE
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.historyProgressBar.visibility = View.GONE
                historyList.clear()
                for (doc in documents) {
                    try {
                        // Lấy dữ liệu an toàn từ Firestore
                        val name = doc.getString("insectName") ?: "Không rõ"
                        val url = doc.getString("imageUrl") ?: ""
                        val time = doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now()

                        // ✅ Lấy confidence kiểu Double rồi chuyển sang Float
                        val conf = doc.getDouble("confidence")?.toFloat() ?: 0f

                        historyList.add(HistoryItem(name, url, time, conf))
                    } catch (e: Exception) {
                        Log.e("History", "Error parsing: ${e.message}")
                    }
                }
                historyAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                binding.historyProgressBar.visibility = View.GONE
            }
    }
}