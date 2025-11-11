package com.example.alertmate

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.alertmate.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity: AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    //use firebase authentication to login/reg
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // to get firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            //get user email and pass
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()

            //check if both fields are filled
            if (email.isNotEmpty() && password.isNotEmpty()) {

                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        //get current user id
                        val uid = firebaseAuth.currentUser?.uid
                        if (uid != null) {
                            //access to firestore
                            // === login section to compare the email and role ===
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            //fetch data user from db class "users"
                            db.collection("users").document(uid).get()
                                .addOnSuccessListener { document ->
                                    //check if there or not
                                    if (document != null && document.exists()) {
                                        val role = document.getString("role")

                                        //direct to MainActivity and pass `role` info
                                        val intent = Intent(this, MainActivity::class.java)
                                        intent.putExtra("role", role)
                                        startActivity(intent)
                                        finish()

                                    } else {
                                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error fetching role: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        //if login fails error show
                        Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
                    }

                }
            } else {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        // === forgot password section ===
        binding.forgotPassword.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.dialog_forget, null)
            val userEmail = view.findViewById<EditText>(R.id.editBox)

            builder.setView(view)
            val dialog = builder.create()

            view.findViewById<Button>(R.id.btnReset).setOnClickListener {
                compareEmail(userEmail)
                dialog.dismiss()
            }

            view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
            }

            if (dialog.window != null) {
                dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            dialog.show()
        }

        binding.tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
    //check whether email valid or not
    private fun compareEmail(email: EditText) {
        if (email.text.toString().isEmpty()) {
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            return
        }

        firebaseAuth.sendPasswordResetEmail(email.text.toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Check your email", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

