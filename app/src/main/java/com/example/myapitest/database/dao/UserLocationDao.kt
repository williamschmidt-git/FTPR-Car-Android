package com.example.myapitest.database.dao

import androidx.room.Dao
import androidx.room.Insert
import com.example.myapitest.database.model.UserLocation

@Dao
interface UserLocationDao {
    @Insert suspend fun insert(userLocation: UserLocation)
}