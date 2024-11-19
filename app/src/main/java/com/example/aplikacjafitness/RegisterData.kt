package com.example.aplikacjafitness

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterData : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.registerdata)

        val name = findViewById<EditText>(R.id.registerName)
        val surname = findViewById<EditText>(R.id.registerSurr)

        val finishRegisterButton = findViewById<Button>(R.id.continueButton)
        finishRegisterButton.setOnClickListener {
            if (name.text.toString().isEmpty() || surname.text.toString().isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
            else {
                val intent = Intent(this, RegisterData2::class.java)
                startActivity(intent)
            }

        }

        val goBackButton = findViewById<Button>(R.id.goBackButton)
        goBackButton.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

    }
}