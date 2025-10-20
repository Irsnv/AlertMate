package com.example.alertmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import android.widget.TextView
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
    }
