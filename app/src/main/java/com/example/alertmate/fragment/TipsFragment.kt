package com.example.alertmate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.alertmate.R
import com.google.firebase.firestore.FirebaseFirestore

class TipsFragment : Fragment() {

    private lateinit var btnTips: Button
    private lateinit var btnEmeCont: Button
    private lateinit var layoutTips: ScrollView
    private lateinit var layoutEmergency: ScrollView
    private lateinit var db: FirebaseFirestore
    private lateinit var textViewTips: TextView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tips, container, false)
    }
//
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnTips = view.findViewById(R.id.btnTips)
        btnEmeCont = view.findViewById(R.id.btnEmeCont)
        layoutTips = view.findViewById(R.id.layoutTips)
        layoutEmergency = view.findViewById(R.id.layoutEmergency)
        textViewTips = view.findViewById(R.id.textViewTips)

        db = FirebaseFirestore.getInstance()

        // Default visible: Tips section
        layoutTips.visibility = View.VISIBLE
        layoutEmergency.visibility = View.GONE

        // Button toggles
        btnTips.setOnClickListener {
            layoutTips.visibility = View.VISIBLE
            layoutEmergency.visibility = View.GONE
        }

        btnEmeCont.setOnClickListener {
            layoutTips.visibility = View.GONE
            layoutEmergency.visibility = View.VISIBLE
        }

        loadAllTips()
        loadAllEmergencyContacts()

    }

    private fun loadAllTips() {
        db.collection("guidance")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    textViewTips.text = "No tips available."
                    return@addOnSuccessListener
                }

                val allTipsText = StringBuilder()

                for (document in result) {
                    val docName =
                        document.id.replaceFirstChar { it.uppercase() } // capitalize title
                    val tipsList = document.get("tips") as? List<String>

                    allTipsText.append("⚠️ $docName\n")
                    if (tipsList != null && tipsList.isNotEmpty()) {
                        allTipsText.append(
                            tipsList.joinToString(
                                separator = "\n• ",
                                prefix = "• ",
                                postfix = "\n\n"
                            )
                        )
                    } else {
                        allTipsText.append("• No tips found.\n\n")
                    }
                }

                textViewTips.text = allTipsText.toString().trim()
            }
            .addOnFailureListener { e ->
                textViewTips.text = "Error loading tips: ${e.message}"
            }
        }

    private fun loadAllEmergencyContacts() {
        val contactsLayout = layoutEmergency.findViewById<LinearLayout>(R.id.layoutEmergencyContacts)
        contactsLayout.removeAllViews()

        db.collection("emergency_contacts")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    val emptyText = TextView(requireContext()).apply {
                        text = "No contacts available."
                    }
                    contactsLayout.addView(emptyText)
                    return@addOnSuccessListener
                }

                for (document in result) {
                    val typeName = document.id.replaceFirstChar { it.uppercase() }
                    val contacts = document.get("contacts") as? List<Map<String, String>> ?: emptyList()

                    // Type title
                    val typeText = TextView(requireContext()).apply {
                        text = " $typeName"
                        textSize = 16f
                        setPadding(0, 8, 0, 4)
                    }
                    contactsLayout.addView(typeText)

                    if (contacts.isEmpty()) {
                        val emptyText = TextView(requireContext()).apply {
                            text = "No contacts found."
                            setPadding(0, 0, 0, 8)
                        }
                        contactsLayout.addView(emptyText)
                    } else {
                        contacts.forEach { contact ->
                            val name = contact["name"] ?: "Unknown"
                            val number = contact["number"] ?: "N/A"
                            val description = contact["description"] ?: ""

                            val contactText = TextView(requireContext()).apply {
                                text = "• $name ($number)\n  $description"
                                setPadding(0, 0, 0, 8)
                            }
                            contactsLayout.addView(contactText)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                val errorText = TextView(requireContext()).apply {
                    text = "Error loading contacts: ${e.message}"
                }
                contactsLayout.addView(errorText)
            }
    }
}