package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

//import com.google.firebase.auth.FirebaseAuth

class LoginActivity: AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var verificationId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navigateToMainActivity()
//        setupView()
//        verifyLoggedUser()
    }

    private fun setupView() {
        binding.btnVerifySms.setOnClickListener {
            onVerifyCode()

        }
        binding.btnSendSms.setOnClickListener {
            onSendVerificationCode()
        }
    }

    private fun onVerifyCode() {
        val verificationCode = binding.veryfyCode.text.toString()
        val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener {
                Toast.makeText(this, R.string.success_login, Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.error_login, Toast.LENGTH_SHORT).show()
            }
    }

    private fun onSendVerificationCode() {
        val phoneNumber = binding.cellphone.text.toString()
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                }

                override fun onVerificationFailed(exception: FirebaseException) {
                    Toast.makeText(this@LoginActivity, R.string.error_login, Toast.LENGTH_SHORT).show()
//                    binding.btnVerifySms.visibility = View.GONE
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(verificationId, token)
                    this@LoginActivity.verificationId = verificationId
                    Toast.makeText(this@LoginActivity, R.string.success_login_sent, Toast.LENGTH_SHORT).show()
                    binding.btnVerifySms.visibility = View.VISIBLE
                    binding.veryfyCode.visibility = View.VISIBLE
                }

            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    private fun verifyLoggedUser() {
//        if(FireBaseAuth.getInstance().currentUser != null) {
//            navigateToMainActivity();
//        }
    }

    private fun navigateToMainActivity() {
        startActivity(MainActivity.newIntent(this))
        finish()
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, LoginActivity::class.java)
    }
}