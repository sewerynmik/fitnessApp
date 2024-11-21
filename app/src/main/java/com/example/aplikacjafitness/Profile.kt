package com.example.aplikacjafitness

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.text
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.semantics.setText
import androidx.core.content.edit
import kotlin.text.toFloatOrNull

class Profile : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var profEmailTextView: TextView
    private lateinit var profNameTextView: TextView
    private lateinit var profSurTextView: TextView
    private lateinit var profWeightTextView: TextView
    private lateinit var profHeightTextView: TextView
    private lateinit var profStepGoalTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        profEmailTextView = findViewById(R.id.profEmail)
        profNameTextView = findViewById(R.id.profName)
        profSurTextView = findViewById(R.id.profSur)
        profWeightTextView = findViewById(R.id.profWeight)
        profHeightTextView = findViewById(R.id.profHeight)
        profStepGoalTextView = findViewById(R.id.profStepGoal)

        loadData()

        val editButton: ImageButton = findViewById(R.id.editButton)
        editButton.setOnClickListener {
            showEditProfilePopup()
        }

        val homeButton: ImageButton = findViewById(R.id.main)
        homeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val logOutButton: TextView = findViewById(R.id.logOut)
        logOutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun loadData() {
        profEmailTextView.text = sharedPreferences.getString("EMAIL", "")
        profNameTextView.text = sharedPreferences.getString("NAME", "")
        profSurTextView.text = sharedPreferences.getString("SURNAME", "")
        profWeightTextView.text = sharedPreferences.getFloat("WEIGHT", 23F).toString()
        profHeightTextView.text = sharedPreferences.getFloat("HEIGHT", 23F).toString()
        profStepGoalTextView.text = sharedPreferences.getString("DAILY_STEP_GOAL", "6000")
    }

    private fun showEditProfilePopup() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.edit_profile_popup, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val surnameEditText = dialogView.findViewById<EditText>(R.id.surnameEditText)
        val weightEditText = dialogView.findViewById<EditText>(R.id.weightEditText)
        val heightEditText = dialogView.findViewById<EditText>(R.id.heightEditText)
        val stepsEditText = dialogView.findViewById<EditText>(R.id.stepsEditText)

        nameEditText.setText(sharedPreferences.getString("NAME", ""))
        surnameEditText.setText(sharedPreferences.getString("SURNAME", ""))
        weightEditText.setText(sharedPreferences.getFloat("WEIGHT", 0f).toString())
        heightEditText.setText(sharedPreferences.getFloat("HEIGHT", 0f).toString())
        val dailyStepGoalString = sharedPreferences.getString("DAILY_STEP_GOAL", "6000")
        stepsEditText.setText(dailyStepGoalString)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Edit Profile")
            .setPositiveButton("Save") { dialog, _ ->

                sharedPreferences.edit {
                    val name = nameEditText.text.toString()
                    if (name.isNotEmpty()) {
                        putString("NAME", name)
                    }

                    val surname = surnameEditText.text.toString()
                    if (surname.isNotEmpty()) {
                        putString("SURNAME", surname)
                    }

                    val weight = weightEditText.text.toString().toFloatOrNull()
                    if (weight != null) {
                        putFloat("WEIGHT", weight)
                    }

                    val height = heightEditText.text.toString().toFloatOrNull()
                    if (height != null) {
                        putFloat("HEIGHT", height)
                    }

                    val steps = stepsEditText.text.toString().toIntOrNull()
                    if (steps != null) {
                        putString("DAILY_STEP_GOAL", steps.toString())
                    }

                    apply()
                }

                loadData()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Do you really want to logout?")
        builder.setPositiveButton("Yes") { dialog, which ->

            sharedPreferences.edit {
                putBoolean("IS_LOGGED_IN", false)
                apply()
            }


            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("No") { dialog, which ->

            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
}