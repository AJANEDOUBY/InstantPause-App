package com.instantpause.app.ui.whitelist

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantpause.app.domain.model.WhitelistEntry
import com.instantpause.app.domain.phone.PhoneNumberAnalyzer
import com.instantpause.app.domain.repository.AddWhitelistResult
import com.instantpause.app.domain.repository.WhitelistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Onglet « Cercle de confiance ».
 *
 * Le contact est lu via l'URI que le répertoire Android veut bien prêter après un
 * ACTION_PICK : **aucune permission READ_CONTACTS** n'est demandée, on ne voit que
 * la fiche choisie.
 */
@HiltViewModel
class WhitelistViewModel @Inject constructor(
    private val whitelist: WhitelistRepository,
    private val numbers: PhoneNumberAnalyzer
) : ViewModel() {

    val entries: StateFlow<List<WhitelistEntry>> = whitelist.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<String> = _messages

    /** Le répertoire n'a pas pu être ouvert (aucune app Contacts, par exemple). */
    fun reportPickerFailure() {
        _messages.tryEmit("Aucune application Contacts sur cet appareil.")
    }

    fun addFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val contact = withContext(Dispatchers.IO) { readContact(context, uri) }
            if (contact == null) {
                _messages.tryEmit("Impossible de lire ce contact.")
                return@launch
            }

            when (whitelist.add(contact.name, contact.number, numbers.defaultRegion())) {
                AddWhitelistResult.ADDED ->
                    _messages.tryEmit("${contact.name} ajouté au cercle de confiance.")

                AddWhitelistResult.DUPLICATE ->
                    _messages.tryEmit("Ce numéro est déjà autorisé.")

                AddWhitelistResult.LIMIT_REACHED ->
                    _messages.tryEmit("Limite de $MAX_CONTACTS contacts atteinte.")

                AddWhitelistResult.INVALID_NUMBER ->
                    _messages.tryEmit("Numéro illisible : impossible de l'autoriser.")
            }
        }
    }

    fun remove(entry: WhitelistEntry) {
        viewModelScope.launch { whitelist.remove(entry.id) }
    }

    private data class PickedContact(val name: String, val number: String)

    /**
     * Deux lectures possibles selon ce que renvoie le répertoire du constructeur :
     *  - l'URI d'une ligne téléphone (contient NUMBER et DISPLAY_NAME) ;
     *  - l'URI d'une fiche contact (il faut alors chercher son numéro).
     */
    private fun readContact(context: Context, uri: Uri): PickedContact? {
        val resolver = context.contentResolver

        resolver.query(
            uri,
            arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val number = cursor.getString(0)
                val name = cursor.getString(1)
                if (!number.isNullOrBlank()) {
                    return PickedContact(name?.takeIf { it.isNotBlank() } ?: number, number)
                }
            }
        }

        val contactId = runCatching { ContentUris.parseId(uri) }.getOrNull()
            ?.takeIf { it > 0 }
            ?: return null

        resolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME),
            "${Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val number = cursor.getString(0)
                val name = cursor.getString(1)
                if (!number.isNullOrBlank()) {
                    return PickedContact(name?.takeIf { it.isNotBlank() } ?: number, number)
                }
            }
        }

        return null
    }

    companion object {
        /** Affichage uniquement : la limite réelle est appliquée par le dépôt. */
        const val MAX_CONTACTS = WhitelistRepository.MAX_ENTRIES
    }
}
