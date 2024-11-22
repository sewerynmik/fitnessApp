package com.example.aplikacjafitness

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
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
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.setText
import androidx.core.content.edit
import java.util.regex.Pattern
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
        profWeightTextView.text = sharedPreferences.getFloat("WEIGHT", 2F).toString()
        profHeightTextView.text = sharedPreferences.getFloat("HEIGHT", 2F).toString()
        profStepGoalTextView.text = sharedPreferences.getString("DAILY_STEP_GOAL", "6000")
    }

    private fun showEditProfilePopup() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.edit_profile_popup, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val surnameEditText = dialogView.findViewById<EditText>(R.id.surnameEditText)
        val weightEditText = dialogView.findViewById<EditText>(R.id.weightEditText)
        val heightEditText = dialogView.findViewById<EditText>(R.id.heightEditText)
        val stepsEditText = dialogView.findViewById<EditText>(R.id.stepsEditText)

        weightEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        weightEditText.filters = arrayOf(InputFilter.LengthFilter(5), WeightInputFilter())

        heightEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        heightEditText.filters = arrayOf(InputFilter.LengthFilter(5), HeightInputFilter())

        nameEditText.filters = arrayOf(InputFilter.LengthFilter(20), NameInputFilter())
        surnameEditText.filters = arrayOf(InputFilter.LengthFilter(20), NameInputFilter())

        nameEditText.setText(sharedPreferences.getString("NAME", ""))
        surnameEditText.setText(sharedPreferences.getString("SURNAME", ""))
        weightEditText.setText(sharedPreferences.getFloat("WEIGHT", 0f).toString())
        heightEditText.setText(sharedPreferences.getFloat("HEIGHT", 1f).toString())
        val dailyStepGoalString = sharedPreferences.getString("DAILY_STEP_GOAL", "6000")
        stepsEditText.setText(dailyStepGoalString)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Edit Profile")
            .setPositiveButton("Save") { dialog, _ ->
                var isValid = true
                var errorMessage = mutableListOf<String>()

                if (nameEditText.text.toString().length < 3) {
                    errorMessage.add("Name is too short, it has to be at least 3 letters.\n")
                    isValid = false
                } else {
                    nameEditText.error = null
                }

                if (surnameEditText.text.toString().length < 3) {
                    errorMessage.add( "Surname is too short, it has to be at least 3 letters.\n")
                    isValid = false
                } else {
                    surnameEditText.error = null
                }

                val height = heightEditText.text.toString().toFloatOrNull()
                if (height == null || height <= 100 || height > 250) {
                    errorMessage.add("Please input real height")
                    isValid = false
                } else {
                    heightEditText.error = null
                }

                if(isValid){
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
                if(!isValid){
                    val errorMessage = errorMessage.joinToString("\n")
                    Toast.makeText(this@Profile, errorMessage, Toast.LENGTH_SHORT).show()
                }
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

class HeightInputFilter : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        val input = dest.toString() + source.toString()
        val pattern = Pattern.compile("^\\d{1,3}(\\.\\d{0,1})?$")
        return if (pattern.matcher(input).matches()) {
            null
        } else {
            ""
        }
    }
}

class NameInputFilter : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        for (i in start until end) {
            if (!Character.isLetter(source[i]) && source[i] != ' ') {
                return ""
            }
        }
        return null
    }
}

class WeightInputFilter : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        val input = dest.toString() + source.toString()
        val pattern = Pattern.compile("^\\d{1,3}(\\.\\d{0,2})?$")
        return if (pattern.matcher(input).matches()) {
            null
        } else {
            ""
        }
    }
}
