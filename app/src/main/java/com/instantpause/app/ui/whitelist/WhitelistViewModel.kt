package com.instantpause.app.ui.whitelist

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantpause.app.R
import com.instantpause.app.domain.model.WhitelistEntry
import com.instantpause.app.domain.phone.PhoneNumberAnalyzer
import com.instantpause.app.domain.repository.AddWhitelistResult
import com.instantpause.app.domain.repository.WhitelistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val numbers: PhoneNumberAnalyzer,
    @param:ApplicationContext private val appContext: Context
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
        _messages.tryEmit(appContext.getString(R.string.whitelist_picker_failure))
    }

    fun addFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val contact = withContext(Dispatchers.IO) { readContact(context, uri) }
            if (contact == null) {
                _messages.tryEmit(appContext.getString(R.string.whitelist_read_error))
                return@launch
            }

            when (whitelist.add(contact.name, contact.number, numbers.defaultRegion())) {
                AddWhitelistResult.ADDED ->
                    _messages.tryEmit(
                        appContext.getString(R.string.whitelist_added, contact.name)
                    )

                AddWhitelistResult.DUPLICATE ->
                    _messages.tryEmit(appContext.getString(R.string.whitelist_duplicate))

                AddWhitelistResult.LIMIT_REACHED ->
                    _messages.tryEmit(
                        appContext.getString(R.string.whitelist_limit_short, MAX_CONTACTS)
                    )

                AddWhitelistResult.INVALID_NUMBER ->
                    _messages.tryEmit(appContext.getString(R.string.whitelist_invalid))
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
     *
     * Toute lecture qui échoue (colonne absente, `SecurityException` faute de droit
     * sur l'URI, fournisseur indisponible) donne `null` et l'interface affiche
     * « lecture impossible » : l'ajout d'un contact ne doit jamais faire planter l'app.
     */
    private fun readContact(context: Context, uri: Uri): PickedContact? {
        val resolver = context.contentResolver

        queryFirstContact(resolver, uri, selection = null, args = null)?.let { return it }

        val contactId = runCatching { ContentUris.parseId(uri) }.getOrNull()
            ?.takeIf { it > 0 }
            ?: return null

        return queryFirstContact(
            resolver,
            Phone.CONTENT_URI,
            selection = "${Phone.CONTACT_ID} = ?",
            args = arrayOf(contactId.toString())
        )
    }

    /**
     * Première ligne exploitable, ou `null`. Les exceptions sont absorbées ici :
     * sans `READ_CONTACTS`, Android n'autorise que l'URI choisie, et une requête
     * sur un autre URI lève `SecurityException`.
     */
    private fun queryFirstContact(
        resolver: ContentResolver,
        uri: Uri,
        selection: String?,
        args: Array<String>?
    ): PickedContact? = try {
        resolver.query(
            uri,
            arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME),
            selection,
            args,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val number = cursor.getString(0)
            val name = cursor.getString(1)
            if (number.isNullOrBlank()) {
                null
            } else {
                PickedContact(name?.takeIf { it.isNotBlank() } ?: number, number)
            }
        }
    } catch (e: Exception) {
        // Jamais le numéro ni le nom dans le journal : seulement le type d'erreur.
        Log.w(TAG, "Lecture du contact impossible (${e.javaClass.simpleName})")
        null
    }

    companion object {
        private const val TAG = "WhitelistViewModel"

        /** Affichage uniquement : la limite réelle est appliquée par le dépôt. */
        const val MAX_CONTACTS = WhitelistRepository.MAX_ENTRIES
    }
}