package com.example.alertmate.profile

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.alertmate.LoginActivity
import com.example.alertmate.R
import com.example.alertmate.alert.AlertReceiver
import com.example.alertmate.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = firebaseAuth.currentUser ?: return

        // Spinner options
        val locations = arrayOf("Klang", "Shah Alam")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, locations)
        binding.profileLocation.adapter = adapter

        // Fetch user data
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    binding.profileFullname.setText(document.getString("fullname"))
                    binding.profileEmail.setText(document.getString("email"))
                    binding.profilePhone.setText(document.getString("phone"))
                    val location = document.getString("location")
                    val position = locations.indexOf(location)
                    if (position >= 0) binding.profileLocation.setSelection(position)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }

        // Update profile
        binding.btnUpdateProfile.setOnClickListener {
            val fullname = binding.profileFullname.text.toString().trim()
            val phone = binding.profilePhone.text.toString().trim()
            val location = binding.profileLocation.selectedItem.toString()

            if (fullname.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "All fields must be filled", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedData = hashMapOf(
                "fullname" to fullname,
                "phone" to phone,
                "location" to location
            )

            db.collection("users").document(user.uid).update(updatedData as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }
        // testalert
        binding.btnTestAlert.setOnClickListener {
            val context = requireContext().applicationContext
            val intent = Intent(context, AlertReceiver::class.java)
            context.sendBroadcast(intent)  // simpler, triggers immediately
        }

    }

    private fun triggerTestAlert() {
        val context = requireContext().applicationContext
        val intent = Intent(context, AlertReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Trigger after 1 second for demo
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 1000,
            pendingIntent
        )
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
