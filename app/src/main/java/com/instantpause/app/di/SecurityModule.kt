package com.instantpause.app.di

import com.instantpause.app.data.datastore.ProtectionPrefs
import com.instantpause.app.domain.security.PassphraseHasher
import com.instantpause.app.domain.security.PassphraseStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindPassphraseStore(impl: ProtectionPrefs): PassphraseStore

    companion object {

        @Provides
        @Singleton
        fun providePassphraseHasher(): PassphraseHasher = PassphraseHasher()

        /** Injectable pour pouvoir figer le temps dans les tests. */
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemUTC()
    }
}
