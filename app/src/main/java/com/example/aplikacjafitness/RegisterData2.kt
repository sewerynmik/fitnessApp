package com.example.aplikacjafitness

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterData2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.registerdata2)

        val weight = findViewById<EditText>(R.id.registerWeight)
        val height = findViewById<EditText>(R.id.registerHeight)
        val bornDate = findViewById<EditText>(R.id.registerDate)


        val finishRegisterButton = findViewById<Button>(R.id.finishRegisterButton)
        finishRegisterButton.setOnClickListener {
            if (weight.text.toString().isEmpty() || height.text.toString().isEmpty() || bornDate.text.toString().isEmpty()){
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
            else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)

            }
        }

        val goBack2Button = findViewById<Button>(R.id.goBack2Button)
        goBack2Button.setOnClickListener {
            val intent = Intent(this, RegisterData::class.java)
            startActivity(intent)
        }


    }



}