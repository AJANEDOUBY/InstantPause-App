package com.instantpause.app.di

import com.instantpause.app.data.datastore.ProtectionPrefs
import com.instantpause.app.domain.dnd.DndGateway
import com.instantpause.app.domain.dnd.DndPreferences
import com.instantpause.app.domain.protection.ProtectionAlarms
import com.instantpause.app.domain.protection.ProtectionState
import com.instantpause.app.domain.protection.ProtectionWindow
import com.instantpause.app.system.AndroidDndGateway
import com.instantpause.app.system.ProtectionAlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DndModule {

    @Binds
    @Singleton
    abstract fun bindDndPreferences(impl: ProtectionPrefs): DndPreferences

    @Binds
    @Singleton
    abstract fun bindDndGateway(impl: AndroidDndGateway): DndGateway

    @Binds
    @Singleton
    abstract fun bindProtectionWindow(impl: ProtectionState): ProtectionWindow

    @Binds
    @Singleton
    abstract fun bindProtectionAlarms(impl: ProtectionAlarmScheduler): ProtectionAlarms
}
