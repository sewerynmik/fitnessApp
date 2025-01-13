package com.example.aplikacjafitness

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.Button
import pl.droidsonroids.gif.GifImageView
import androidx.appcompat.app.AlertDialog
import pl.droidsonroids.gif.GifDrawable

class Login : BaseActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase

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


        dbHelper = DatabaseHelper(this)
        db = dbHelper.writableDatabase

        val isDatabaseConnected = dbHelper.isDatabaseConnected()
//        val connectionInfo = if (isDatabaseConnected) "Database connected" else "Database connection failed"
//        Toast.makeText(this, connectionInfo, Toast.LENGTH_SHORT).show()
//        val userEmail = dbHelper.getUserEmailById(1)
//        if (userEmail != null) {
//            Toast.makeText(this, "User email: $userEmail", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(this, "User with id = 1 not found", Toast.LENGTH_SHORT).show()
//        }

        loginButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.loginMail).text.toString()
            val password = findViewById<EditText>(R.id.loginPass).text.toString()

            val dbHelper = DatabaseHelper(this)
            val credentialsValid = dbHelper.checkCredentials(email, password)

            if (credentialsValid) {
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

    override fun onDestroy() {
        super.onDestroy()
        db.close()
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