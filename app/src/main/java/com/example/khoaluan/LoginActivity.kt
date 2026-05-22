package com.example.khoaluan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.khoaluan.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // 1. Trình lắng nghe kết quả trả về từ cửa sổ đăng nhập Google
    private val googleAuthLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Đăng nhập Google bị hủy hoặc thất bại!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // 2. Kiểm tra nếu đã đăng nhập thì vào thẳng màn hình chính
        if (auth.currentUser != null) {
            goToMainActivity()
        }

        // 3. Cấu hình Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // ID này tự động sinh ra khi kết nối Firebase
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ==========================================
        // CÁC SỰ KIỆN CLICK NÚT BẤM
        // ==========================================

        // 4. Xử lý nút Đăng nhập bằng Email/Password
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Vui lòng nhập Email"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Vui lòng nhập mật khẩu"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                        goToMainActivity()
                    } else {
                        val errorMessage = task.exception?.message ?: "Lỗi không xác định"
                        Toast.makeText(this, "Đăng nhập thất bại: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // 5. Xử lý nút Quên mật khẩu
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email của bạn vào ô Email trước để đặt lại mật khẩu", Toast.LENGTH_LONG).show()
                binding.etEmail.requestFocus()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Đã gửi link đặt lại mật khẩu qua Email!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // 6. Xử lý nút Đăng nhập bằng Google
        binding.btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleAuthLauncher.launch(signInIntent)
        }

        // 7. Xử lý nút Đăng ký (chuyển trang)
        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // ==========================================
    // CÁC HÀM HỖ TRỢ
    // ==========================================

    // Hàm liên kết tài khoản Google với hệ thống Firebase
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                } else {
                    Toast.makeText(this, "Lỗi xác thực Firebase: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Hàm chuyển sang màn hình chính và xóa lịch sử màn hình đăng nhập
    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}