package com.cooldown.app.ui.whitelist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.result.contract.ActivityResultContract

/**
 * Sélecteur de **numéro** du répertoire, et non de fiche contact.
 *
 * L'URI renvoyée par `ACTION_PICK` sur [Phone.CONTENT_URI] contient directement
 * `Phone.NUMBER` : c'est la seule façon d'obtenir le numéro sans la permission
 * READ_CONTACTS (l'URI renvoyée pour une fiche contact, elle, ne l'expose pas).
 */
class PickPhoneNumber : ActivityResultContract<Unit, android.net.Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): android.net.Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}
