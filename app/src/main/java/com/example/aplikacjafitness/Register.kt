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

        val email = findViewById<EditText>(R.id.loginMailReg)
        val password = findViewById<EditText>(R.id.loginPassReg)
        val passwordConfirm = findViewById<EditText>(R.id.loginPassConf)


        registerButton.setOnClickListener {
            if (email.text.toString().isEmpty() || password.text.toString().isEmpty() || passwordConfirm.text.toString().isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
            else if(email.text.toString().isNotEmpty() && password.text.toString().isNotEmpty() && passwordConfirm.text.toString().isNotEmpty() && password.text.toString() == passwordConfirm.text.toString()) {
                val intent = Intent(this, RegisterData::class.java)
                startActivity(intent)
                finish()


            }
            else {
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
            }

        }


        cancelButton.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }


    }

}