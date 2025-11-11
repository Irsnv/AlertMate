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

        //initialize firestore and firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        //spinner dropdown options (locations)
        val options = arrayOf("Select your location", "Klang", "Shah Alam")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        binding.locOptions.adapter = adapter

        //=== register button ===
        //get all user detail
        binding.regBtn.setOnClickListener {
            val fullname = binding.inputFullname.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val phone = binding.inputPhone.text.toString().trim()
            val password = binding.inputPass.text.toString()
            val confirmPassword = binding.inputConfi.text.toString()
            val location = binding.locOptions.selectedItem.toString()

            //check if field input not empty
            if (fullname.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() &&
                password.isNotEmpty() && confirmPassword.isNotEmpty() &&
                location != "Select your location"
            ) {
                //recheck if pass and reconfirm pass same
                if (password == confirmPassword) {
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                                //prepare user data to store inside Firestore
                                val uid = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                                val userMap = hashMapOf(
                                    "fullname" to fullname,
                                    "email" to email,
                                    "phone" to phone,
                                    "location" to location,
                                    "role" to "user" // "user" automatically as default
                                )

                                //save user data into "users" class in firestore
                                db.collection("users").document(uid).set(userMap)
                                    .addOnSuccessListener {
                                        //show simple toast success
                                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                                        //redirect to login
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
