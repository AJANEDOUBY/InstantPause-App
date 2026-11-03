package com.instantpause.app.system

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Administrateur d'appareil **sans aucune politique**.
 *
 * Son unique rôle est de bloquer la désinstallation : Android refuse de
 * désinstaller une app tant qu'un administrateur est actif. Ce n'est pas un
 * cadenas (les Paramètres permettent de le désactiver en deux taps), mais c'est
 * un ralentisseur qui fait échouer la tentative impulsive.
 */
class CoolDownDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) = Unit

    override fun onDisabled(context: Context, intent: Intent) = Unit
}
