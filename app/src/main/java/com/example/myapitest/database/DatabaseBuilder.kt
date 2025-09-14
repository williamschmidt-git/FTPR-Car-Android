package com.example.myapitest.database

import android.content.Context
import androidx.room.Room

object DatabaseBuilder {
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context? = null): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            if(context == null) {
                throw Exception("Contexto não pode ser nulo")
            }

            val newInstance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).build()

            INSTANCE = newInstance
            newInstance
        }
    }
}