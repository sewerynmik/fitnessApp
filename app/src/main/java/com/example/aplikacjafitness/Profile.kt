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
import androidx.compose.ui.semantics.text

class Profile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.profile)

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("EMAIL", "")

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
    }
}