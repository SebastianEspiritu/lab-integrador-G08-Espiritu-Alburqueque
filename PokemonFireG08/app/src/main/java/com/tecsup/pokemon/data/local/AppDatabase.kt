package com.tecsup.pokemon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ItemEntity::class], version = 1, exportSchema = true)
@TypeConverters(TiposConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
