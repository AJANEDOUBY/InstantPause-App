package com.instantpause.app.di

import com.instantpause.app.domain.dnd.DndController
import com.instantpause.app.domain.protection.ProtectionAlarms
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Point d'entrée pour les `BroadcastReceiver` non injectés par Hilt.
 *
 * `BootReceiver` et `ProtectionAlarmReceiver` doivent pouvoir restaurer le filtre
 * « Ne pas déranger » et reprogrammer la transition suivante sans dépendre d'une
 * activité : sans cela, un téléphone redémarré après la fin d'une plage
 * resterait silencieux.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DndEntryPoint {
    fun dndController(): DndController
    fun protectionAlarms(): ProtectionAlarms
}
