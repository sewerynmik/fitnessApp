package com.example.aplikacjafitness

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.Button
import android.widget.ImageView
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.text
import pl.droidsonroids.gif.GifImageView
import androidx.appcompat.app.AlertDialog
import pl.droidsonroids.gif.GifDrawable

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.login)
        val loginLayout = findViewById<View>(R.id.LoginCard)
        loginLayout.visibility = View.INVISIBLE

        ShowWelcomePopup()

        Handler(Looper.getMainLooper()).postDelayed({
            loginLayout.visibility = View.VISIBLE
        }, 1500)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        loginButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.loginMail).text.toString()
            val password = findViewById<EditText>(R.id.loginPass).text.toString()

            if (email == "email@mail.com" && password == "pass") {

                val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("EMAIL", email)
                editor.putLong("LOGIN_TIMESTAMP", System.currentTimeMillis())
                editor.putBoolean("IS_LOGGED_IN", true)
                editor.apply()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            }
        }


        registerButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.remove("EMAILreg")
            editor.apply()
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun ShowWelcomePopup() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.welcome, null)
        val welcomeGif = dialogView.findViewById<GifImageView>(R.id.welcomeGif)
        (welcomeGif.drawable as? GifDrawable)?.start()

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val dialog = builder.create()
        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, 1400)
    }


}