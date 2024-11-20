package com.example.aplikacjafitness

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.text
import androidx.appcompat.app.AlertDialog

class Profile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.profile)

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("EMAIL", "")

        val logOutButton: TextView = findViewById(R.id.logOut)

        val profEmailTextView: TextView = findViewById(R.id.profEmail)
        profEmailTextView.text = email

        val profNameTextView: TextView = findViewById(R.id.profName)
        profNameTextView.text = "Jan"

        val profSurTextView: TextView = findViewById(R.id.profSur)
        profSurTextView.text = "Kowalski"

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
