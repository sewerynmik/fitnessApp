package com.example.aplikacjafitness

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import android.widget.Button

class Register : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.register)

        val registerButton = findViewById<Button>(R.id.register)
        val cancelButton = findViewById<Button>(R.id.Cancel)

        registerButton.setOnClickListener {
            Toast.makeText(this, "Youre gay", Toast.LENGTH_SHORT).show()

        }


        cancelButton.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }


    }

}