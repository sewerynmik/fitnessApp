package com.example.aplikacjafitness

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text

class RegisterData : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.registerdata)

        val name = findViewById<EditText>(R.id.registerName)
        val surname = findViewById<EditText>(R.id.registerSurr)

        val continueRegisterButton = findViewById<Button>(R.id.continueButton)
        continueRegisterButton.setOnClickListener {
            if (name.text.toString().isNotEmpty() && surname.text.toString().isNotEmpty()) {
                val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("NAMEreg", name.text.toString())
                editor.putString("SURNAMEreg", surname.text.toString())
                editor.apply()

                val intent = Intent(this, RegisterData2::class.java)
                startActivity(intent)
            }
            else {
                val intent = Intent(this, RegisterData2::class.java)
                startActivity(intent)
            }

        }

        val goBackButton = findViewById<Button>(R.id.goBackButton)
        goBackButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.remove("EMAILreg")
            editor.remove("PASSWORDreg")
            editor.apply()

            val intent = Intent(this, Register::class.java)
            startActivity(intent)
            finish()
        }

    }
}