# Notes de version — Play Console

Limite imposée par Google : **500 caractères par langue**. Les textes ci-dessous sont des
saisies à copier telles quelles, balises `<langue>` incluses.

---

## 1.4.0 — première publication

### Français — `fr-FR`

```
<fr-FR>
Première version sur Google Play.

Instant Pause intercepte vos appels sortants pendant les plages que vous définissez : l'écran de pause s'affiche, vous respirez, puis vous décidez.

Trois contacts de confiance restent joignables à tout moment, avec une limite à 3 minutes. Les numéros d'urgence ne sont jamais bloqués.

Aucun compte, aucune publicité, aucune connexion réseau : tout reste sur votre téléphone.
</fr-FR>
```

### Anglais — `en-US`

```
<en-US>
First release on Google Play.

Instant Pause intercepts your outgoing calls during the windows you set: the pause screen appears, you breathe, then you decide.

Three trusted contacts can always be reached, capped at 3 minutes. Emergency numbers are never blocked.

No account, no ads, no network connection: everything stays on your phone.
</en-US>
```

---

## 1.5.0 - mode Zen et message personnel

### Français - `fr-FR`

```
<fr-FR>
Le mode Zen ne crée plus de plage permanente : il s'arrête tout seul, et vous choisissez sa durée d'un seul appui (4 h à 12 h).

Vous pouvez maintenant écrire votre propre phrase sur l'écran de pause, depuis l'onglet Sécurité.

Correctifs : notifications accentuées, explication affichée avant le dialogue anti-désinstallation.
</fr-FR>
```

### Anglais - `en-US`

```
<en-US>
Zen mode no longer creates a permanent schedule: it ends by itself, and you pick its duration in one tap (4 h to 12 h).

You can now write your own sentence on the pause screen, from the Security tab.

Fixes: accented notifications, explanation shown before the uninstall-protection dialog.
</en-US>
```

### Espagnol - `es-ES`

```
<es-ES>
El modo Zen ya no crea una franja permanente: termina solo, y eliges su duración con un solo toque (de 4 h a 12 h).

Ahora puedes escribir tu propia frase en la pantalla de pausa, desde la pestaña Seguridad.

Correcciones: notificaciones con acentos, explicación antes del diálogo de anti-desinstalación.
</es-ES>
```

---

## 1.6.0 - interface multilingue

### Français - `fr-FR`

```
<fr-FR>
L'application est maintenant disponible en anglais, en français et en espagnol. Vous pouvez changer sa langue directement dans l'onglet Sécurité, ou depuis Réglages > Applications > Instant Pause > Langue.

Pendant vos plages, vous pouvez aussi couper les sonneries WhatsApp, Signal et Telegram. Vos appels d'urgence et vos alarmes passent toujours. L'option est désactivée par défaut.

Correctifs : dates de l'historique et noms des canaux de notification traduits.
</fr-FR>
```

### Anglais - `en-US`

```
<en-US>
The app is now available in English, French and Spanish. You can change its language right in the Security tab, or from Settings > Apps > Instant Pause > Language.

During your windows, you can also mute WhatsApp, Signal and Telegram ringtones. Emergency calls and alarms still get through. The option is off by default.

Fixes: translated history dates and notification channel names.
</en-US>
```

### Espagnol - `es-ES`

```
<es-ES>
La aplicación ya está disponible en inglés, francés y español. Puedes cambiar su idioma desde la pestaña Seguridad, o en Ajustes > Aplicaciones > Instant Pause > Idioma.

Durante tus franjas, también puedes silenciar los tonos de WhatsApp, Signal y Telegram. Las llamadas de emergencia y las alarmas siguen pasando. La opción está desactivada por defecto.

Correcciones: fechas del historial y nombres de los canales de notificación traducidos.
</es-ES>
```

---

## 1.7.0 - retrait de l'administrateur d'appareil

### Français - `fr-FR`

```
<fr-FR>
Instant Pause ne demande plus l'administrateur d'appareil : la désinstallation redevient une action Android normale.

Ce retrait répond aux règles Google Play. Le verrouillage par code, les plages, les contacts de confiance et le raccrochage à 3 minutes restent inchangés.

L'exemption d'optimisation batterie n'est plus demandée : un bandeau ouvre désormais les réglages batterie du système.
</fr-FR>
```

### Anglais - `en-US`

```
<en-US>
Instant Pause no longer requests device administrator: uninstalling is a normal Android action again.

This removal follows Google Play rules. The code lock, time windows, trusted contacts and the 3-minute hang-up are unchanged.

The battery optimisation exemption is no longer requested: a banner now opens the system battery settings.
</en-US>
```

### Espagnol - `es-ES`

```
<es-ES>
Instant Pause ya no solicita el administrador de dispositivos: desinstalar vuelve a ser una acción normal de Android.

Esta retirada cumple las reglas de Google Play. El bloqueo por código, las franjas, los contactos de confianza y el colgado a los 3 minutos no cambian.

Ya no se solicita la exención de optimización de batería: un aviso abre ahora los ajustes de batería del sistema.
</es-ES>
```

---

## Modèle pour la prochaine version

Garde la même structure : trois phrases maximum, ce qui change pour l'utilisateur, aucun détail
technique. Les notes de version ne sont **pas** un journal de commits.

```
<fr-FR>
[Nouveauté en une phrase.]

[Deuxième phrase : ce que ça change concrètement.]

[Facultatif : correctifs.]
</fr-FR>
```

```
<en-US>
[What's new in one sentence.]

[What it changes for the user.]

[Optional: fixes.]
</en-US>
```
