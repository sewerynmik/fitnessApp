package com.example.aplikacjafitness

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import java.io.File
import kotlin.io.path.exists
import android.provider.MediaStore
import java.io.FileOutputStream


class Profile : BaseActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var profEmailTextView: TextView
    private lateinit var profNameTextView: TextView
    private lateinit var profSurTextView: TextView
    private lateinit var profWeightTextView: TextView
    private lateinit var profHeightTextView: TextView
    private lateinit var profStepGoalTextView: TextView
    private lateinit var profileImageView: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        profEmailTextView = findViewById(R.id.profEmail)
        profNameTextView = findViewById(R.id.profName)
        profSurTextView = findViewById(R.id.profSur)
        profHeightTextView = findViewById(R.id.profHeight)
        profStepGoalTextView = findViewById(R.id.profStepGoal)
        profileImageView = findViewById(R.id.profileImageView)
        loadProfilePicture()

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

        val progressButton: ImageButton = findViewById(R.id.progressBtn)
        progressButton.setOnClickListener {
            val intent = Intent(this, Progress::class.java)
            startActivity(intent)
        }

        val logOutButton: TextView = findViewById(R.id.logOut)
        logOutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPreferences.getString("EMAIL", "")

        val dbHelper = DatabaseHelper(this)
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT name, surname, weight, height, daily_steps_target FROM users WHERE email = ?",
            arrayOf(savedEmail)
        )
        if (cursor.moveToFirst()) {
            profEmailTextView.text = savedEmail
            profNameTextView.text = cursor.getString(0)
            profSurTextView.text = cursor.getString(1)
            profHeightTextView.text = cursor.getDouble(3).toString()
            profStepGoalTextView.text = cursor.getInt(4).toString()
        } else {
            Toast.makeText(this, "User not found in database", Toast.LENGTH_SHORT).show()
        }
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

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val userId = getUserIdFromSharedPreferences(this)
        val cursor = db.rawQuery("SELECT name, surname, weight, height, daily_steps_target FROM users WHERE id = ?", arrayOf(userId.toString()))
        if (cursor.moveToFirst()) {
            nameEditText.setText(cursor.getString(0))
            surnameEditText.setText(cursor.getString(1))
            weightEditText.setText(cursor.getFloat(2).toString())
            heightEditText.setText(cursor.getFloat(3).toString())
            stepsEditText.setText(cursor.getInt(4).toString())
        }
        cursor.close()

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
                    errorMessage.add("Surname is too short, it has to be at least 3 letters.\n")
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

                if (isValid) {
                    val name = nameEditText.text.toString()
                    val surname = surnameEditText.text.toString()
                    val weight = weightEditText.text.toString().toFloatOrNull()
                    val height = heightEditText.text.toString().toFloatOrNull()
                    val steps = stepsEditText.text.toString().toIntOrNull()

                    sharedPreferences.edit {
                        if (name.isNotEmpty()) {
                            putString("NAME", name)
                        }
                        if (surname.isNotEmpty()) {
                            putString("SURNAME", surname)
                        }
                        if (weight != null) {
                            putFloat("WEIGHT", weight)
                        }
                        if (height != null) {
                            putFloat("HEIGHT", height)
                        }
                        if (steps != null) {
                            putString("DAILY_STEP_GOAL", steps.toString())
                        }
                        apply()
                    }

                    dbHelper.updateUserData(db, userId, name, surname, weight, height, steps)

                    loadData()
                    dialog.dismiss()
                } else {
                    val errorMessageString = errorMessage.joinToString("\n")
                    Toast.makeText(this@Profile, errorMessageString, Toast.LENGTH_SHORT).show()
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

    private fun loadProfilePicture() {
        val file = File(filesDir, "profile_picture.jpg")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            profileImageView.setImageBitmap(bitmap)
        } else {
            profileImageView.setImageResource(R.drawable.logo)
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

fun getUserIdFromSharedPreferences(context: Context): Int {
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val savedEmail = sharedPreferences.getString("EMAIL", "")
    val dbHelper = DatabaseHelper(context)
    val cursor = dbHelper.readableDatabase.rawQuery("SELECT id FROM users WHERE email = ?", arrayOf(savedEmail))
    var userId = -1
    if (cursor.moveToFirst()) {
        userId = cursor.getInt(0)
    }
    cursor.close()
    return userId
}


    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                try {
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    val file = File(filesDir, "profile_picture.jpg")
                    val outputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.flush()
                    outputStream.close()

                    profileImageView.setImageBitmap(bitmap)

                    Toast.makeText(this, "Profile picture saved", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error saving profile picture", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }




}




