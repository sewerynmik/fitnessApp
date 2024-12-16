package com.example.aplikacjafitness

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.icu.util.Calendar
import androidx.compose.foundation.layout.add
import androidx.compose.ui.text.intl.Locale
import java.text.SimpleDateFormat
import kotlin.text.format
import kotlin.text.indexOf
import kotlin.text.indexOfFirst
import kotlin.text.toFloat

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "FitnessApp.db"
        private const val DATABASE_VERSION = 8 // jak sie cos robi odnoscnie tabel itp to zmienic numerek tutaj
    }

    private val context: Context = context

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE \"users\" (\n" +
                "\t\"id\"\tINTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "\t\"email\"\tTEXT NOT NULL,\n" +
                "\t\"name\"\tTEXT NOT NULL,\n" +
                "\t\"surname\"\tTEXT NOT NULL,\n" +
                "\t\"born_date\"\tTEXT,\n" +
                "\t\"weight\"\tREAL,\n" +
                "\t\"height\"\tREAL,\n" +
                "\t\"daily_steps_target\"\tINTEGER,\n" +
                "\t\"password\"\tTEXT NOT NULL\n" +
                ")")

        db.execSQL("CREATE TABLE \"daily_steps\" (\n" +
                "\t\"id\"\tINTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "\t\"date\"\tTEXT,\n" +
                "\t\"steps\"\tINTEGER,\n" +
                "\t\"user_id\"\tINTEGER NOT NULL,\n" +
                "\tCONSTRAINT \"userId__stepsDaily\" FOREIGN KEY(\"user_id\") REFERENCES \"users\"(\"id\")\n" +
                ")")

        db.execSQL("CREATE TABLE \"weight_progress\" (\n" +
                "\t\"id\"\tINTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,\n" +
                "\t\"weight\"\tNUMERIC,\n" +
                "\t\"date\"\tTEXT,\n" +
                "\t\"user_id\"\tINTEGER,\n" +
                "\t\"pic_name\"\tTEXT,\n" +
                "\tCONSTRAINT \"weight_users\" FOREIGN KEY(\"user_id\") REFERENCES \"users\"(\"id\")\n" +
                ");")

        db.execSQL("INSERT INTO users (email, name, surname, born_date, weight, height, daily_steps_target, password) VALUES ('email@mail.com', 'John', 'Doe', '1990-01-01', 70.5, 180.0, 8000, 'pass')")
        db.execSQL("INSERT INTO users (email, name, surname, born_date, weight, height, daily_steps_target, password) VALUES ('email2@mail.com', 'John2', 'Doe2', '1990-01-01', 70.5, 180.0, 8000, 'pass')")

        db.execSQL("INSERT INTO daily_steps (date, steps, user_id) VALUES ('23-11-2024', 567, 1)")
        db.execSQL("INSERT INTO daily_steps (date, steps, user_id) VALUES ('22-11-2024', 1234, 1)")
        db.execSQL("INSERT INTO daily_steps (date, steps, user_id) VALUES ('21-11-2024', 2312, 1)")
        db.execSQL("INSERT INTO daily_steps (date, steps, user_id) VALUES ('19-11-2024', 1876, 1)")
        db.execSQL("INSERT INTO daily_steps (date, steps, user_id) VALUES ('18-11-2024', 1927, 1)")
        db.execSQL("INSERT INTO daily_steps (date, steps, user_id) VALUES ('17-11-2024', 2534, 1)")

        db.execSQL("INSERT INTO weight_progress (weight, date, user_id) VALUES (88.5, '22-11-2024', 1)")
        db.execSQL("INSERT INTO weight_progress (weight, date, user_id) VALUES (85, '20-11-2024', 1)")
        db.execSQL("INSERT INTO weight_progress (weight, date, user_id) VALUES (72, '15-11-2024', 1)")
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS users")
            db.execSQL("DROP TABLE IF EXISTS daily_steps")
            onCreate(db)
        }
    }

    fun insertStepCount(db: SQLiteDatabase, date: String, steps: Int) {
        val values = ContentValues().apply {
            put("date", date)
            put("steps", steps)
        }
        db.insert("daily_steps", null, values)
    }

    fun checkCredentials(email: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.query("users", arrayOf("id"), "email = ? AND password = ?", arrayOf(email, password), null, null, null)
        val credentialsMatch = cursor.moveToFirst()
        cursor.close()
        return credentialsMatch
    }

    fun isDatabaseConnected(): Boolean {
        val db = writableDatabase
        return db != null && db.isOpen
    }

    fun getUserEmailById(userId: Int): String? {
        val db = readableDatabase
        val cursor = db.query("users", arrayOf("email"), "id = ?", arrayOf(userId.toString()), null, null, null)
        var userEmail: String? = null
        if (cursor.moveToFirst()) {
            userEmail = cursor.getString(0)
        }
        cursor.close()
        return userEmail
    }

    fun addUserData(email: String, name: String, surname: String, bornDate: String, weight: Double, height: Double, dailyStepsTarget: Int, password: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("email", email)
            put("name", name)
            put("surname", surname)
            put("born_date", bornDate)
            put("weight", weight)
            put("height", height)
            put("daily_steps_target", 6000)
            put("password", password)
        }
        db.insert("users", null, values)

    }

    fun updateDailySteps(db: SQLiteDatabase, date: String, steps: Int, userId: Int) {
        val cursor = db.rawQuery("SELECT steps FROM daily_steps WHERE date = ? AND user_id = ?", arrayOf(date, userId.toString()))
        if (cursor.moveToFirst()) {

            val values = ContentValues().apply {
                put("steps", steps)
            }
            db.update("daily_steps", values, "date = ? AND user_id = ?", arrayOf(date, userId.toString()))
        } else {

            val values = ContentValues().apply {
                put("date", date)
                put("steps", steps)
                put("user_id", userId)
            }
            db.insert("daily_steps", null, values)
        }
        cursor.close()
    }

    fun getStepsForToday(date: String, userId: Int): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT steps FROM daily_steps WHERE date = ? AND user_id = ?", arrayOf(date, userId.toString()))
        var steps = -1
        if (cursor.moveToFirst()) {
            steps = cursor.getInt(0)
        }
        cursor.close()

        return steps
    }

    fun updateUserData(db: SQLiteDatabase, userId: Int, name: String, surname: String, weight: Float?, height: Float?, dailyStepsTarget: Int?) {
        val values = ContentValues().apply {
            put("name", name)
            put("surname", surname)
            if (weight != null) {
                put("weight", weight)
            }
            if (height != null) {
                put("height", height)
            }
            if (dailyStepsTarget != null) {
                put("daily_steps_target", dailyStepsTarget)
            }
        }
        db.update("users", values, "id = ?", arrayOf(userId.toString()))
    }

    fun getLast7DaysSteps(userId: Int): List<Int> {
        val stepsList = mutableListOf<Int>()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())

        for (i in 0..6) {
            val date = dateFormat.format(calendar.time)
            val steps = getStepsForToday(date, userId)
            stepsList.add(steps)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return stepsList.reversed()
    }

    fun getWeightProgress(userId: Int): Pair<List<Float>, List<String>> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT weight, date FROM weight_progress WHERE user_id = ? ORDER BY date ASC",
            arrayOf(userId.toString())
        )
        val weights = mutableListOf<Float>()
        val dates = mutableListOf<String>()
        while (cursor.moveToNext()) {
            weights.add(cursor.getFloat(cursor.getColumnIndexOrThrow("weight")))
            dates.add(cursor.getString(cursor.getColumnIndexOrThrow("date")))
        }
        cursor.close()
        return weights to dates
    }

    fun insertWeightProgress(userId: Int, date: String, weight: Float, picName: String? = null) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("date", date)
            put("weight", weight)
            put("pic_name", picName)
        }
        db.insert("weight_progress", null, values)
    }

    fun insertProgressPhoto(userId: Int, filename: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("filename", filename)
        }
        db.insert("progress_photos", null, values)
    }

    fun getDataForDate(date: String): WeightProgressData {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT weight, pic_name FROM weight_progress WHERE user_id = ? AND date = ?",
            arrayOf(Utils.getUserIdFromSharedPreferences(context).toString(), date)
        )
        var weight = 0f
        var picName: String? = null
        if (cursor.moveToFirst()) {
            weight = cursor.getFloat(cursor.getColumnIndexOrThrow("weight"))
            picName = cursor.getString(cursor.getColumnIndexOrThrow("pic_name"))
        }
        cursor.close()
        return WeightProgressData(weight, picName)
    }

    data class WeightProgressData(val weight: Float, val picName: String?)

    fun getUserData(userId: Int): UserData {
        val db = this.readableDatabase
        val cursor = db.query(
            "users",
            arrayOf("height", "weight"),
            "id = ?",
            arrayOf(userId.toString()),
            null,
            null,
            null
        )

        var height = 0f
        var weight = 0f

        if (cursor != null && cursor.moveToFirst()) {
            height = cursor.getFloat(cursor.getColumnIndexOrThrow("height"))
            weight = cursor.getFloat(cursor.getColumnIndexOrThrow("height"))
            cursor.close()
        }

        db.close()
        return UserData(height, weight)
    }

    fun getLastRecordedWeightBeforeDate(userId: Int, date: String): Float? {
        val db = this.readableDatabase
        val query = """
        SELECT weight FROM weight_progress
        WHERE user_id = ? AND date < ?
        ORDER BY date DESC
        LIMIT 1
    """
        val cursor = db.rawQuery(query, arrayOf(userId.toString(), date))
        return if (cursor.moveToFirst()) {
            cursor.getFloat(cursor.getColumnIndexOrThrow("weight"))
        } else {
            null
        }.also { cursor.close() }
    }

    fun getLastWeightEntry(userId: Int): WeightAllData? {
        val db = this.readableDatabase
        val query = """
        SELECT weight, date FROM weight_progress
        WHERE user_id = ?
        ORDER BY date DESC
        LIMIT 1
    """
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        val lastEntry = if (cursor.moveToFirst()) {
            val weight = cursor.getFloat(cursor.getColumnIndexOrThrow("weight"))
            val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
            WeightAllData(weight, date)
        } else {
            null
        }
        cursor.close()
        return lastEntry
    }

    data class WeightAllData(val weight: Float, val date: String)

}

    data class UserData(val height: Float, val weight: Float)



// Add other database operations here (e.g., update, delete, query)
