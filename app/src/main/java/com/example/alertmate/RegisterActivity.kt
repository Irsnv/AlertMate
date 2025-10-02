package com.example.alertmate

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alertmate.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Spinner options (locations)
        val options = arrayOf("Select your location", "Klang", "Shah Alam")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        binding.locOptions.adapter = adapter

        // Register button
        binding.regBtn.setOnClickListener {
            val fullname = binding.inputFullname.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val phone = binding.inputPhone.text.toString().trim()
            val password = binding.inputPass.text.toString()
            val confirmPassword = binding.inputConfi.text.toString()
            val location = binding.locOptions.selectedItem.toString()

            // Validation
            if (fullname.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() &&
                password.isNotEmpty() && confirmPassword.isNotEmpty() &&
                location != "Select your location"
            ) {
                if (password == confirmPassword) {
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)

                                val uid = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                                val userMap = hashMapOf(
                                    "fullname" to fullname,
                                    "email" to email,
                                    "phone" to phone,
                                    "location" to location
                                )

                                db.collection("users").document(uid).set(userMap)
                                    .addOnSuccessListener {
                                        // Show simple toast success
                                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                                        // Redirect to login
                                        val intent = Intent(this, LoginActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }

                            } else {
                                Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginRedirect.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
