package com.example.aplikacjafitness

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "FitnessApp.db"
        private const val DATABASE_VERSION = 7 // jak sie cos robi odnoscnie tabel itp to zmienic numerek tutaj
    }

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

        db.execSQL("INSERT INTO users (email, name, surname, born_date, weight, height, daily_steps_target, password) VALUES ('email@mail.com', 'John', 'Doe', '1990-01-01', 70.5, 180.0, 8000, 'pass')")
        db.execSQL("INSERT INTO users (email, name, surname, born_date, weight, height, daily_steps_target, password) VALUES ('email2@mail.com', 'John2', 'Doe2', '1990-01-01', 70.5, 180.0, 8000, 'pass')")
        db.execSQL("INSERT INTO daily_steps (date, steps, user_id) VALUES ('22-11-2024', 0, 1)")
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
    // Add other database operations here (e.g., update, delete, query)
}