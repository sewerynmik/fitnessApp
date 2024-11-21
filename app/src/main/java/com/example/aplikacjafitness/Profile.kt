package com.example.aplikacjafitness

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.text
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import kotlin.text.toFloatOrNull

class Profile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.profile)

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("EMAIL", "")
        var name = sharedPreferences.getString("NAME", "")
        var surname = sharedPreferences.getString("SURNAME", "")
        var weight = sharedPreferences.getFloat("WEIGHT", 23F)
        var height = sharedPreferences.getFloat("HEIGHT", 23F)

        val logOutButton: TextView = findViewById(R.id.logOut)

        val profEmailTextView: TextView = findViewById(R.id.profEmail)
        profEmailTextView.text = email

        val profNameTextView: TextView = findViewById(R.id.profName)
        profNameTextView.text = name

        val profSurTextView: TextView = findViewById(R.id.profSur)
        profSurTextView.text = surname

        val profWeightTextView: TextView = findViewById(R.id.profWeight)
        profWeightTextView.text = weight.toString()

        val profHeightTextView: TextView = findViewById(R.id.profHeight)
        profHeightTextView.text = height.toString()

//        val editor = sharedPreferences.edit()
//        sharedPreferences.edit { putString("NAME", name) }
//        sharedPreferences.edit { putString("SURNAME", surname) }

        val editButton: ImageButton = findViewById(R.id.editButton)

        // In Profile.kt

        editButton.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.edit_profile_popup, null)
            val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
            val surnameEditText = dialogView.findViewById<EditText>(R.id.surnameEditText)
            val weightEditText = dialogView.findViewById<EditText>(R.id.weightEditText)
            val heightEditText = dialogView.findViewById<EditText>(R.id.heightEditText)

            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("Edit Profile")
                .setPositiveButton("Save") { dialog, _ ->
                    // Save changes to Shared Preferences
                    val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit {
                        val name = nameEditText.text.toString()
                        if (name.isNotEmpty()) {
                            putString("NAME", name)
                        }

                        val surname = surnameEditText.text.toString()
                        if (surname.isNotEmpty()) {
                            putString("SURNAME", surname)
                        }

                        val weight = weightEditText.text.toString().toFloatOrNull()
                        if (weight != null) {
                            putFloat("WEIGHT", weight)
                        }

                        val height = heightEditText.text.toString().toFloatOrNull()
                        if (height != null) {
                            putFloat("HEIGHT", height)
                        }

                        apply() // Save changes
                    }

                    dialog.dismiss()
                }

            val dialog = builder.create()
            dialog.show()
        }

        val homeButton : ImageButton = findViewById(R.id.main)

        homeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        logOutButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Logout")
            builder.setMessage("Do you really want to logout?")
            builder.setPositiveButton("Yes") { dialog, which ->
                // Perform logout actions here
                val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putBoolean("IS_LOGGED_IN", false)
                editor.apply()

                // Navigate to the login screen
                val intent = Intent(this, Login::class.java)
                startActivity(intent)
                finish()
            }
            builder.setNegativeButton("No") { dialog, which ->
                // Dismiss the dialog
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }
    }
}
