package com.instantpause.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [ScheduleEntity::class, WhitelistEntity::class, BlockedLogEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(BlockStatusConverters::class)
abstract class CoolDownDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun blockedLogDao(): BlockedLogDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CoolDownDatabase =
        Room.databaseBuilder(context, CoolDownDatabase::class.java, "cooldown.db").build()

    @Provides fun provideScheduleDao(db: CoolDownDatabase) = db.scheduleDao()
    @Provides fun provideWhitelistDao(db: CoolDownDatabase) = db.whitelistDao()
    @Provides fun provideBlockedLogDao(db: CoolDownDatabase) = db.blockedLogDao()
}