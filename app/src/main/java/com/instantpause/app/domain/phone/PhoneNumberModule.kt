package com.instantpause.app.domain.phone

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PhoneNumberModule {

    @Binds
    @Singleton
    abstract fun bindPhoneNumberAnalyzer(impl: PhoneNumberNormalizer): PhoneNumberAnalyzer
}
