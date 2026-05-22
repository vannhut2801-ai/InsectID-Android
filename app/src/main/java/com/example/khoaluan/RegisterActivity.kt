package com.example.khoaluan // ❗ Update with your actual package name

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.khoaluan.databinding.ActivityRegisterBinding // ❗ Ensure this matches your layout name
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Initialize ViewBinding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Handle Register Button Click
        binding.btnDoRegister.setOnClickListener {
            val email = binding.etRegEmail.text.toString().trim()
            val password = binding.etRegPassword.text.toString().trim()
            val confirmPassword = binding.etRegConfirmPassword.text.toString().trim()

            // Input Validation
            if (email.isEmpty()) {
                binding.etRegEmail.error = "Vui lòng nhập Email"
                binding.etRegEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etRegPassword.error = "Vui lòng nhập mật khẩu"
                binding.etRegPassword.requestFocus()
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.etRegPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
                binding.etRegPassword.requestFocus()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                binding.etRegConfirmPassword.error = "Mật khẩu nhập lại không khớp"
                binding.etRegConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            // Create Firebase User
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Đăng ký thành công! Đang đăng nhập...", Toast.LENGTH_SHORT).show()
                        // Registration successful, navigate to Main Activity
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMsg = task.exception?.message ?: "Đăng ký thất bại"
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Handle "Back to Login" Button
        binding.tvBackToLogin.setOnClickListener {
            finish() // Close this activity to return to LoginActivity
        }
    }
}