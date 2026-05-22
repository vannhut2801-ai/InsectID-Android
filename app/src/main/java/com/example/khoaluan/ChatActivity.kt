package com.example.khoaluan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

class ChatActivity : AppCompatActivity() {
    private val apiKey = "AIzaSyBjnSGHnoSJuM5Z0te1H7vimLW3ROsPtTw"

    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnBack: ImageView

    private val messagesList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var generativeChat: Chat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        // Bắt sự kiện click cho các nút gợi ý
        val suggest1 = findViewById<TextView>(R.id.btnSuggest1)
        val suggest2 = findViewById<TextView>(R.id.btnSuggest2)
        val suggest3 = findViewById<TextView>(R.id.btnSuggest3)

        val suggestClickListener = View.OnClickListener { view ->
            // Lấy chữ từ cái nút vừa được bấm
            val text = (view as TextView).text.toString()
            // Gửi thẳng câu hỏi đó cho AI
            sendMessageToAI(text)
        }

        // Gắn sự kiện cho cả 3 nút
        suggest1.setOnClickListener(suggestClickListener)
        suggest2.setOnClickListener(suggestClickListener)
        suggest3.setOnClickListener(suggestClickListener)

        rvChat = findViewById(R.id.rvChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)

        // Thiết lập RecyclerView
        chatAdapter = ChatAdapter(messagesList)
        rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChat.adapter = chatAdapter

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            systemInstruction = content {
                text("Bạn là một chuyên gia nông nghiệp tại Việt Nam. Hãy trả lời các câu hỏi về sâu bệnh hại lúa và cây ăn quả một cách ngắn gọn, dễ hiểu cho bà con nông dân. ")
            }
        )
        generativeChat = generativeModel.startChat()
        messagesList.add(ChatMessage("Bạn đang gặp vấn đề gì về côn trùng?", false))
        chatAdapter.notifyItemInserted(0)

        btnBack.setOnClickListener {
            finish()
        }

        btnSend.setOnClickListener {
            val userText = etMessage.text.toString().trim()
            if (userText.isNotEmpty()) {
                sendMessageToAI(userText)
            }
        }
    }

    private fun sendMessageToAI(userText: String) {
        messagesList.add(ChatMessage(userText, true))
        chatAdapter.notifyItemInserted(messagesList.size - 1)
        rvChat.smoothScrollToPosition(messagesList.size - 1)
        etMessage.text.clear()

        val typingIndex = messagesList.size
        messagesList.add(ChatMessage("Đang suy nghĩ...", false))
        chatAdapter.notifyItemInserted(typingIndex)
        rvChat.smoothScrollToPosition(typingIndex)

        lifecycleScope.launch {
            try {
                val response = generativeChat.sendMessage(userText)

                val rawText = response.text ?: "Xin lỗi, tôi không thể trả lời lúc này."

                val cleanText = rawText.replace("**", "")
                    .replace("* ", "• ")

                messagesList[typingIndex] = ChatMessage(cleanText, false)

                chatAdapter.notifyItemChanged(typingIndex)
                rvChat.smoothScrollToPosition(messagesList.size - 1)

            } catch (e: Exception) {
                // Xử lý nếu rớt mạng hoặc lỗi
                messagesList[typingIndex] = ChatMessage("Lỗi kết nối mạng: ${e.localizedMessage}", false)
                chatAdapter.notifyItemChanged(typingIndex)
            }
        }
    }

    inner class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

        inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val layoutUser: LinearLayout = view.findViewById(R.id.layoutUserMessage)
            val tvUser: TextView = view.findViewById(R.id.tvUserMessage)
            val layoutBot: LinearLayout = view.findViewById(R.id.layoutBotMessage)
            val tvBot: TextView = view.findViewById(R.id.tvBotMessage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
            return ChatViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val message = messages[position]
            if (message.isUser) {
                holder.layoutUser.visibility = View.VISIBLE
                holder.layoutBot.visibility = View.GONE
                holder.tvUser.text = message.text
            } else {
                holder.layoutBot.visibility = View.VISIBLE
                holder.layoutUser.visibility = View.GONE
                holder.tvBot.text = message.text
            }
        }

        override fun getItemCount() = messages.size
    }
}