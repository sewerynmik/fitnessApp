package com.example.aplikacjafitness

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text

class RegisterData2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.registerdata2)

        val weight = findViewById<EditText>(R.id.registerWeight)
        val height = findViewById<EditText>(R.id.registerHeight)
        val bornDate = findViewById<EditText>(R.id.registerDate)


        val finishRegisterButton = findViewById<Button>(R.id.finishRegisterButton)
        finishRegisterButton.setOnClickListener {
            if (weight.text.toString().isNotEmpty() && height.text.toString().isNotEmpty() && bornDate.text.toString().isNotEmpty()) {
                val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("WEIGHTreg", weight.text.toString())
                editor.putString("HEIGHTreg", height.text.toString())
                editor.putString("BORNDATEreg", bornDate.text.toString())
                editor.apply()

                val dbHelper = DatabaseHelper(this)
                val email = sharedPreferences.getString("EMAILreg", "") ?: ""
                val password = sharedPreferences.getString("PASSWORDreg", "") ?: ""
                val name = sharedPreferences.getString("NAMEreg", "") ?: ""
                val surname = sharedPreferences.getString("SURNAMEreg", "") ?: ""
                val weight = sharedPreferences.getString("WEIGHTreg", "")?.toDoubleOrNull() ?: 0.0
                val height = sharedPreferences.getString("HEIGHTreg", "")?.toDoubleOrNull() ?: 0.0
                val bornDate = sharedPreferences.getString("BORNDATEreg", "") ?: ""

                dbHelper.addUserData(email, name, surname, bornDate, weight, height, 0, password)

                editor.remove("EMAILreg")
                editor.remove("PASSWORDreg")
                editor.remove("NAMEreg")
                editor.remove("SURNAMEreg")
                editor.remove("WEIGHTreg")
                editor.remove("HEIGHTreg")
                editor.remove("BORNDATEreg")
                editor.apply()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)

                val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.remove("EMAILreg")
                editor.apply()

            }

        }

        val goBack2Button = findViewById<Button>(R.id.goBack2Button)
        goBack2Button.setOnClickListener {
            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.remove("NAMEreg")
            editor.remove("SURNAMEreg")
            editor.apply()

            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }


    }



}