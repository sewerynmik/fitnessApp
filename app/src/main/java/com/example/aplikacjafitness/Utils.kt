package com.example.aplikacjafitness

import android.content.Context

object Utils {

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
}