package com.cooldown.app.data.local

import com.cooldown.app.domain.model.BlockStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockStatusConvertersTest {

    private val converters = BlockStatusConverters()

    @Test
    fun `aller-retour sur toutes les valeurs`() {
        BlockStatus.entries.forEach { status ->
            assertEquals(status, converters.toStatus(converters.fromStatus(status)))
        }
    }

    @Test
    fun `valeur inconnue retombe sur BLOCKED`() {
        assertEquals(BlockStatus.BLOCKED, converters.toStatus("VALEUR_INCONNUE"))
    }
}
