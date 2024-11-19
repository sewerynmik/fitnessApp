package com.example.aplikacjafitness

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import android.widget.Button

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.login)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        loginButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.loginMail).text.toString()
            val password = findViewById<EditText>(R.id.loginPass).text.toString()

            if (email == "email@mail.com" && password == "pass") {

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()

                val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("EMAIL", email)
                editor.apply()

            } else {

                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            }
        }


        registerButton.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
            finish()
        }
    }


}