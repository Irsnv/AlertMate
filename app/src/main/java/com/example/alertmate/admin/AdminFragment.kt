package com.example.alertmate

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp


class AdminFragment : Fragment() {

    private lateinit var db: FirebaseFirestore

    // Tips buttons
    private lateinit var btnAdd: Button
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button

    // Emergency Contact buttons
    private lateinit var btnAddEC: Button
    private lateinit var btnEditEC: Button
    private lateinit var btnDelEC: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()

        // Initialize Tips buttons
        btnAdd = view.findViewById(R.id.btnAddTips)
        btnEdit = view.findViewById(R.id.btnEditTips)
        btnDelete = view.findViewById(R.id.btnDelTips)

        btnAdd.setOnClickListener { showAddDialog() }
        btnEdit.setOnClickListener { showEditDialog() }
        btnDelete.setOnClickListener { showDeleteDialog() }

        // Initialize Emergency Contact buttons
        btnAddEC = view.findViewById(R.id.btnAddEC)
        btnEditEC = view.findViewById(R.id.btnEditEC)
        btnDelEC = view.findViewById(R.id.btnDelEC)

        btnAddEC.setOnClickListener { showAddECDialog() }
        btnEditEC.setOnClickListener { showEditECDialog() }
        btnDelEC.setOnClickListener { showDeleteECDialog() }

        SendWarningSection(view)
    }


    //=== Add Tips===
    private fun showAddDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.add_tips_dialog, null)
        val categoryInput = dialogView.findViewById<EditText>(R.id.inputCategory)
        val tipInput = dialogView.findViewById<EditText>(R.id.inputTip)

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Tip")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val category = categoryInput.text.toString().trim()
                val tip = tipInput.text.toString().trim()

                if (category.isNotEmpty() && tip.isNotEmpty()) {
                    db.collection("guidance").document(category)
                        .update("tips", FieldValue.arrayUnion(tip))
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Tip added successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            redirectToDashboard()
                        }
                        .addOnFailureListener { e ->
                            // if document doesn’t exist, create it
                            val newData = hashMapOf("tips" to listOf(tip))
                            db.collection("guidance").document(category)
                                .set(newData)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "New category added successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    redirectToDashboard()
                                }
                                .addOnFailureListener { err ->
                                    Toast.makeText(
                                        requireContext(),
                                        "Error: ${err.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                } else {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                        .show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // === Edit Tips ===
    private fun showEditDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.edit_tips_dialog, null)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val editTextTips = dialogView.findViewById<EditText>(R.id.editTipsContent)

        db.collection("guidance").get().addOnSuccessListener { snapshot ->
            val categories = snapshot.documents.map { it.id }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                categories
            )
            spinnerCategory.adapter = adapter

            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedCat = categories[position]
                    db.collection("guidance").document(selectedCat).get()
                        .addOnSuccessListener { doc ->
                            val tipsList = doc.get("tips") as? List<String>
                            editTextTips.setText(tipsList?.joinToString("\n") ?: "")
                        }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Edit Tips")
                .setView(dialogView)
                .setPositiveButton("Save") { dialog, _ ->
                    val selectedCat = spinnerCategory.selectedItem.toString()
                    val newTips =
                        editTextTips.text.toString().trim().split("\n").filter { it.isNotBlank() }

                    db.collection("guidance").document(selectedCat)
                        .update("tips", newTips)
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Tips updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            redirectToDashboard()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                requireContext(),
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    //=== Delete Tips ===
    private fun showDeleteDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.delete_tips_dialog, null)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerDeleteCategory)
        val spinnerTip = dialogView.findViewById<Spinner>(R.id.spinnerDeleteTip)

        db.collection("guidance").get().addOnSuccessListener { snapshot ->
            val categories = snapshot.documents.map { it.id }
            val categoryAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                categories
            )
            spinnerCategory.adapter = categoryAdapter

            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedCat = categories[position]
                    db.collection("guidance").document(selectedCat).get()
                        .addOnSuccessListener { doc ->
                            val tipsList = doc.get("tips") as? List<String> ?: emptyList()
                            val tipsAdapter = ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                tipsList
                            )
                            spinnerTip.adapter = tipsAdapter
                        }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Delete Tip")
                .setView(dialogView)
                .setPositiveButton("Delete") { dialog, _ ->
                    val selectedCat = spinnerCategory.selectedItem.toString()
                    val selectedTip = spinnerTip.selectedItem.toString()

                    db.collection("guidance").document(selectedCat)
                        .update("tips", FieldValue.arrayRemove(selectedTip))
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Tip deleted successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            redirectToDashboard()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                requireContext(),
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    //=== Redirect Function ===
    private fun redirectToDashboard() {
        // Replace with your actual navigation method to go back to dashboard
        requireActivity().supportFragmentManager.popBackStack()
    }


    //=== Add Emergency Contact ===
    private fun showAddECDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.add_ec_dialog, null)

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val inputName = dialogView.findViewById<EditText>(R.id.inputName)
        val inputNumber = dialogView.findViewById<EditText>(R.id.inputNumber)
        val inputDesc = dialogView.findViewById<EditText>(R.id.inputDesc)

        // Load types from string-array instead of Firestore
        val types = resources.getStringArray(R.array.emergency_types)
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        spinnerType.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setTitle("Add Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val selectedType = spinnerType.selectedItem.toString()
                val name = inputName.text.toString().trim()
                val number = inputNumber.text.toString().trim()
                val desc = inputDesc.text.toString().trim()

                if (name.isEmpty() || number.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Name and Number are required",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val contact = hashMapOf(
                    "name" to name,
                    "number" to number,
                    "description" to desc
                )

                // Save to Firestore dynamically
                val docRef = db.collection("emergency_contacts").document(selectedType)
                docRef.get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        // Add new contact to existing type
                        docRef.update("contacts", FieldValue.arrayUnion(contact))
                    } else {
                        // Create type document if it doesn't exist
                        docRef.set(hashMapOf("contacts" to listOf(contact)))
                    }
                    Toast.makeText(
                        requireContext(),
                        "Contact added successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    redirectToDashboard()
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    //=== Edit Emergency ===
    private fun showEditECDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.edit_ec_dialog, null)

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val spinnerContact = dialogView.findViewById<Spinner>(R.id.spinnerContact)
        val inputName = dialogView.findViewById<EditText>(R.id.inputName)
        val inputNumber = dialogView.findViewById<EditText>(R.id.inputNumber)
        val inputDesc = dialogView.findViewById<EditText>(R.id.inputDesc)

        var originalContact: Map<String, String>? = null

        db.collection("emergency_contacts").get().addOnSuccessListener { snapshot ->
            val firestoreTypes = snapshot.documents.map { it.id }
            val types = if (firestoreTypes.isEmpty()) {
                resources.getStringArray(R.array.emergency_types).toList()
            } else {
                firestoreTypes
            }

            spinnerType.adapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)

            spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedType = types[position]
                    val docRef = db.collection("emergency_contacts").document(selectedType)
                    docRef.get().addOnSuccessListener { doc ->
                        val contacts =
                            doc.get("contacts") as? List<Map<String, String>> ?: emptyList()
                        val contactNames = contacts.map { it["name"] ?: "Unknown" }
                        spinnerContact.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            contactNames
                        )

                        spinnerContact.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    p: AdapterView<*>,
                                    v: View?,
                                    pos: Int,
                                    i: Long
                                ) {
                                    originalContact = contacts[pos]
                                    inputName.setText(originalContact?.get("name"))
                                    inputNumber.setText(originalContact?.get("number"))
                                    inputDesc.setText(originalContact?.get("description"))
                                }

                                override fun onNothingSelected(p: AdapterView<*>) {}
                            }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                if (originalContact == null) {
                    Toast.makeText(
                        requireContext(),
                        "No contact selected to edit.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val selectedType = spinnerType.selectedItem.toString()
                val docRef = db.collection("emergency_contacts").document(selectedType)

                val updatedContact = hashMapOf(
                    "name" to inputName.text.toString().trim(),
                    "number" to inputNumber.text.toString().trim(),
                    "description" to inputDesc.text.toString().trim()
                )

                // Atomically remove the old contact and add the updated one
                docRef.update("contacts", FieldValue.arrayRemove(originalContact))
                    .addOnSuccessListener {
                        docRef.update("contacts", FieldValue.arrayUnion(updatedContact))
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Contact updated!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                redirectToDashboard()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //=== Delete Emergency ===
    private fun showDeleteECDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.delete_ec_dialog, null)

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val spinnerContact = dialogView.findViewById<Spinner>(R.id.spinnerContact)

        var contactToDelete: Map<String, String>? = null

        db.collection("emergency_contacts").get().addOnSuccessListener { snapshot ->
            val firestoreTypes = snapshot.documents.map { it.id }
            val types = if (firestoreTypes.isEmpty()) {
                resources.getStringArray(R.array.emergency_types).toList()
            } else {
                firestoreTypes
            }

            spinnerType.adapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)

            spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedType = types[position]
                    val docRef = db.collection("emergency_contacts").document(selectedType)
                    docRef.get().addOnSuccessListener { doc ->
                        val contacts =
                            doc.get("contacts") as? List<Map<String, String>> ?: emptyList()
                        val contactNames = contacts.map { it["name"] ?: "Unknown" }
                        spinnerContact.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            contactNames
                        )

                        spinnerContact.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    p: AdapterView<*>,
                                    v: View?,
                                    pos: Int,
                                    i: Long
                                ) {
                                    contactToDelete = contacts[pos]
                                }

                                override fun onNothingSelected(p: AdapterView<*>) {}
                            }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Delete") { dialog, _ ->
                if (contactToDelete == null) {
                    Toast.makeText(
                        requireContext(),
                        "No contact selected to delete.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val selectedType = spinnerType.selectedItem.toString()
                db.collection("emergency_contacts").document(selectedType)
                    .update("contacts", FieldValue.arrayRemove(contactToDelete))
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Contact deleted!", Toast.LENGTH_SHORT)
                            .show()
                        redirectToDashboard()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun SendWarningSection(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.profileLocation)
        val btnSendWarn = view.findViewById<Button>(R.id.btnSendWarn)
        val tvKlang = view.findViewById<TextView>(R.id.tvLocKlang)
        val tvShahAlam = view.findViewById<TextView>(R.id.tvLocShahAlam)

        //populate dropdown options
        val locations = listOf("Select location", "Klang", "Shah Alam")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, locations)
        spinner.adapter = adapter

        // Handle dropdown selection
        // Show/hide the warning message for the selected location
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLocation = parent.getItemAtPosition(position).toString()
                // Hide all first
                tvKlang.visibility = View.GONE
                tvShahAlam.visibility = View.GONE

                when (selectedLocation) {
                    "Klang" -> tvKlang.visibility = View.VISIBLE
                    "Shah Alam" -> tvShahAlam.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        //setup Realtime Database + Firestore
        val realtimeDb = com.google.firebase.database.FirebaseDatabase
            .getInstance("https://alertmate-6eaf4-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("alerts")

        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // send button logic
        btnSendWarn.setOnClickListener {
            val selectedLocation = spinner.selectedItem.toString()

            if (selectedLocation == "Select location") {
                Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val message = when (selectedLocation) {
                "Klang" -> tvKlang.text.toString()
                "Shah Alam" -> tvShahAlam.text.toString()
                else -> ""
            }

            val alertData = mapOf(
                "message" to message,
                "location" to selectedLocation,
                "timestamp" to System.currentTimeMillis()
            )


            //Send to Firestore (for history)
            firestore.collection("alerts")
                .add(alertData)
                .addOnSuccessListener {
                    //Also send to Realtime DB (for live alert)
                    val alertId = realtimeDb.push().key ?: "unknown_id"
                    realtimeDb.child(alertId).setValue(alertData)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Warning sent successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to send alert: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to save alert: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
