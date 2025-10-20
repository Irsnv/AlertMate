package com.example.alertmate.fragment

import android.R
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.alertmate.LoginActivity
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
        val adapter =
            ArrayAdapter(requireContext(), R.layout.simple_spinner_dropdown_item, locations)
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

        binding.btnTestAlert.setOnClickListener {
            triggerTestAlert()  // now the method is used
        }


    }

    private fun triggerTestAlert() {
        val context = requireContext().applicationContext
        val intent = Intent(context, AlertReceiver::class.java)

        // Trigger immediately for testing
        context.sendBroadcast(intent)

        // Optional: schedule exact alarm for demo (API 28+)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 1000, // 1 second later
                        pendingIntent
                    )
                } else {
                    Toast.makeText(context, "Enable exact alarms in settings for scheduled alerts.", Toast.LENGTH_LONG).show()
                }
            } else {
                // Older devices: setExact works but exactness may vary
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Exact alarm failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}