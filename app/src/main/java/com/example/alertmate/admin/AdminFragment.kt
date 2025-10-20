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

class AdminFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var btnAdd: Button
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()

        btnAdd = view.findViewById(R.id.btnAddTips)
        btnEdit = view.findViewById(R.id.btnEditTips)
        btnDelete = view.findViewById(R.id.btnDelTips)

        btnAdd.setOnClickListener { showAddDialog() }
        btnEdit.setOnClickListener { showEditDialog() }
        btnDelete.setOnClickListener { showDeleteDialog() }
    }

    // ----------------------- ADD TIPS -----------------------
    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.add_tips_dialog, null)
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
                            Toast.makeText(requireContext(), "Tip added successfully!", Toast.LENGTH_SHORT).show()
                            redirectToDashboard()
                        }
                        .addOnFailureListener { e ->
                            // if document doesn’t exist, create it
                            val newData = hashMapOf("tips" to listOf(tip))
                            db.collection("guidance").document(category)
                                .set(newData)
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "New category added successfully!", Toast.LENGTH_SHORT).show()
                                    redirectToDashboard()
                                }
                                .addOnFailureListener { err ->
                                    Toast.makeText(requireContext(), "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                } else {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ----------------------- EDIT TIPS -----------------------
    private fun showEditDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.edit_tips_dialog, null)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val editTextTips = dialogView.findViewById<EditText>(R.id.editTipsContent)

        db.collection("guidance").get().addOnSuccessListener { snapshot ->
            val categories = snapshot.documents.map { it.id }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
            spinnerCategory.adapter = adapter

            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
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
                    val newTips = editTextTips.text.toString().trim().split("\n").filter { it.isNotBlank() }

                    db.collection("guidance").document(selectedCat)
                        .update("tips", newTips)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Tips updated successfully!", Toast.LENGTH_SHORT).show()
                            redirectToDashboard()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // ----------------------- DELETE TIPS -----------------------
    private fun showDeleteDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.delete_tips_dialog, null)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerDeleteCategory)
        val spinnerTip = dialogView.findViewById<Spinner>(R.id.spinnerDeleteTip)

        db.collection("guidance").get().addOnSuccessListener { snapshot ->
            val categories = snapshot.documents.map { it.id }
            val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
            spinnerCategory.adapter = categoryAdapter

            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedCat = categories[position]
                    db.collection("guidance").document(selectedCat).get()
                        .addOnSuccessListener { doc ->
                            val tipsList = doc.get("tips") as? List<String> ?: emptyList()
                            val tipsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tipsList)
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
                            Toast.makeText(requireContext(), "Tip deleted successfully!", Toast.LENGTH_SHORT).show()
                            redirectToDashboard()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // ----------------------- Redirect Function -----------------------
    private fun redirectToDashboard() {
        // Replace with your actual navigation method to go back to dashboard
        requireActivity().supportFragmentManager.popBackStack()
    }
}
