# Privacy Policy — Instant Pause

**English** · [Español](#política-de-privacidad--instant-pause) · [Français](#politique-de-confidentialité--instant-pause)

**Last updated: 3 November 2026**

Instant Pause (`com.instantpause.app`) is an Android app that helps you resist impulsive calls.
This policy explains, plainly, what data the app uses and what it does not do with it.

## In one sentence

**Instant Pause collects nothing, sends nothing and has no Internet access.** Everything the app
knows stays on your phone, in its private storage.

## 1. Data controller

The app's publisher is **JANEDOUBY** (Instant Pause project, formerly CoolDown).

Contact: **janedouby@gmail.com** — or, for a public report,
<https://github.com/AJANEDOUBY/instant-pause-privacy/issues>

As no data is transmitted to the publisher, there is no processing register to consult: the app
has no server.

## 2. Data processed, and only on your device

The app stores locally the information it needs to work:

| Data | Purpose | Storage |
|---|---|---|
| Protection code | Verify that it is really you before weakening the protection | Salted PBKDF2 hash (the code is **never** stored in plain text) |
| Time windows | Know when to intercept calls | Private Room database (`cooldown.db`) |
| Allowed contacts | Let certain calls through, hung up at 3 minutes | Name, international number, displayed number, region |
| Block log | Show you the intercepted calls | Number, timestamp, reason — **purged after 90 days** |
| Settings | Protection state, Zen mode, preferences | Private DataStore (`cooldown_prefs`) |

This data is stored in the app's **private directory**, which other apps cannot read on a
non-rooted device.

### Allowed contacts

You pick a contact with the **Android contact picker**. The app does **not** request the
`READ_CONTACTS` permission: Android only hands it the entry you explicitly selected. The full
address book stays out of the app's reach.

### The call identifier

To decide whether a call should be intercepted, the app receives the dialled number from Android,
at the time of the call, through the **Call redirection** role you granted. That number is
analysed in memory, then kept in the local log described above. It is not transmitted to anyone.

## 3. What the app does not do

- **No network connection.** The app does not declare the `INTERNET` permission and embeds no
  HTTP client.
- **No account.** No sign-up, no email address, no identifier.
- **No analytics, no crash reporting, no advertising SDK.**
- **No sharing with third parties.** No data is sold, rented or transmitted.
- **No advertising, no profiling.**
- **No access** to your SMS, location, microphone, camera or address book.

## 4. Permissions requested and why

| Permission | Why |
|---|---|
| **Call redirection** role (Telecom) | Intercept the outgoing call to show the pause screen |
| `ANSWER_PHONE_CALLS` | Automatically hang up at 3 minutes a call to an allowed contact |
| Device administrator | Slow down an impulsive uninstall. **No policy** is imposed on the device, no remote wipe is possible |
| `SYSTEM_ALERT_WINDOW` / `USE_FULL_SCREEN_INTENT` | Show the pause screen over the call screen or the lock screen |
| `POST_NOTIFICATIONS` / `VIBRATE` | Persistent protection notification and haptic feedback |
| `RECEIVE_BOOT_COMPLETED` | Re-enable protection after a reboot |
| `SCHEDULE_EXACT_ALARM` | Trigger windows at the scheduled time |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent Android from putting the protection service to sleep |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep protection and the call timer active |

Each permission can be revoked in the Android settings. Removing the Call redirection role or the
hang-up permission disables the corresponding protection: this is deliberate, the app cannot
prevent it.

## 5. Retention

- Windows, contacts and settings: **until the app is uninstalled**.
- Block log: **90 days** maximum, purged automatically.
- Protection code: hash kept until it is changed or the app is uninstalled.

## 6. Your rights (GDPR)

You have the right to access, rectify, erase, restrict and object. As no data is transmitted to
the publisher, these rights are exercised **directly in the app**:

- **Access and rectification**: windows, contacts and settings are visible and editable in the app
  (with the code).
- **Erasure**: uninstall the app, or clear its data from
  *Settings → Apps → Instant Pause → Storage*. Everything disappears with the private storage.
- The anti-uninstall protection prevents an impulsive uninstall: first deactivate the device
  administrator in *Settings → Security → Device administration*, with your code. It is a guardrail
  against yourself, not data retention.

Legal basis: performance of the service you request (Article 6(1)(b) GDPR). No processing takes
place without your action.

## 7. Security

The code is kept as a PBKDF2 hash with a random salt and constant-time comparison. Data resides in
the Android sandbox. The app contains no API key, no server secret, and therefore nothing to steal
remotely.

## 8. Children

The app is not intended for children under 13 and collects no data about them.

## 9. Changes

Any change to this policy will be published at this address and dated at the top of the document.
If a future version introduced data processing (for example optional synchronisation), it would
state so explicitly before activation.

---

# Política de privacidad — Instant Pause

[English](#privacy-policy--instant-pause) · **Español** · [Français](#politique-de-confidentialité--instant-pause)

**Última actualización: 3 de noviembre de 2026**

Instant Pause (`com.instantpause.app`) es una aplicación Android que te ayuda a resistir las
llamadas impulsivas. Esta política explica, sin rodeos, qué datos utiliza la aplicación y qué no
hace con ellos.

## En una frase

**Instant Pause no recoge nada, no envía nada y no tiene acceso a Internet.** Todo lo que la
aplicación sabe se queda en tu teléfono, en su espacio de almacenamiento privado.

## 1. Responsable del tratamiento

El editor de la aplicación es **JANEDOUBY** (proyecto Instant Pause, anteriormente CoolDown).

Contacto: **janedouby@gmail.com** — o, para una denuncia pública,
<https://github.com/AJANEDOUBY/instant-pause-privacy/issues>

Como no se transmite ningún dato al editor, no existe ningún registro de tratamiento que
consultar: la aplicación no tiene servidor.

## 2. Datos tratados, y únicamente en tu dispositivo

La aplicación guarda localmente la información necesaria para funcionar:

| Dato | Finalidad | Almacenamiento |
|---|---|---|
| Código de protección | Comprobar que eres realmente tú antes de debilitar la protección | Huella PBKDF2 con sal (el código **nunca** se guarda en claro) |
| Franjas horarias | Saber cuándo interceptar las llamadas | Base Room privada (`cooldown.db`) |
| Contactos autorizados | Dejar pasar ciertas llamadas, colgadas a los 3 minutos | Nombre, número en formato internacional, número mostrado, región |
| Registro de bloqueos | Mostrarte las llamadas interceptadas | Número, fecha y hora, motivo — **purgado a los 90 días** |
| Ajustes | Estado de la protección, modo Zen, preferencias | DataStore privado (`cooldown_prefs`) |

Estos datos se guardan en el **directorio privado de la aplicación**, que las demás aplicaciones
no pueden leer en un dispositivo no rooteado.

### Los contactos autorizados

Eliges un contacto con el **selector de contactos de Android**. La aplicación **no** solicita el
permiso `READ_CONTACTS`: Android solo le entrega la ficha que has designado explícitamente. La
agenda completa queda fuera del alcance de la aplicación.

### El identificador de llamada

Para decidir si una llamada debe interceptarse, la aplicación recibe de Android el número marcado,
en el momento de la llamada, mediante el rol **Redirección de llamadas** que has concedido. Ese
número se analiza en memoria y luego se conserva en el registro local descrito arriba. No se
transmite a nadie.

## 3. Lo que la aplicación no hace

- **Ninguna conexión de red.** La aplicación no declara el permiso `INTERNET` y no incluye ningún
  cliente HTTP.
- **Ninguna cuenta.** Sin registro, sin correo electrónico, sin identificador.
- **Sin analítica, sin informes de fallos, sin SDK publicitario.**
- **Ningún intercambio con terceros.** Ningún dato se vende, se alquila ni se transmite.
- **Sin publicidad, sin perfilado.**
- **Ningún acceso** a tus SMS, tu ubicación, tu micrófono, tu cámara ni tu agenda.

## 4. Permisos solicitados y motivos

| Permiso | Motivo |
|---|---|
| Rol **Redirección de llamadas** (Telecom) | Interceptar la llamada saliente para mostrar la pantalla de pausa |
| `ANSWER_PHONE_CALLS` | Colgar automáticamente a los 3 minutos una llamada a un contacto autorizado |
| Administrador de dispositivos | Frenar una desinstalación impulsiva. **Ninguna política** se impone al dispositivo y no es posible ningún borrado remoto |
| `SYSTEM_ALERT_WINDOW` / `USE_FULL_SCREEN_INTENT` | Mostrar la pantalla de pausa sobre la pantalla de llamada o de bloqueo |
| `POST_NOTIFICATIONS` / `VIBRATE` | Notificación permanente de la protección y respuestas hápticas |
| `RECEIVE_BOOT_COMPLETED` | Reactivar la protección tras un reinicio |
| `SCHEDULE_EXACT_ALARM` | Activar las franjas a la hora prevista |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Impedir que Android duerma el servicio de protección |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Mantener activas la protección y el temporizador de llamada |

Cada permiso puede retirarse desde los ajustes de Android. Retirar el rol de Redirección de
llamadas o el permiso de colgado desactiva la protección correspondiente: es voluntario, la
aplicación no puede impedirlo.

## 5. Plazo de conservación

- Franjas, contactos y ajustes: **hasta la desinstalación** de la aplicación.
- Registro de bloqueos: **90 días** como máximo, purgado automáticamente.
- Código de protección: huella conservada hasta su modificación o la desinstalación.

## 6. Tus derechos (RGPD)

Tienes derecho de acceso, rectificación, supresión, limitación y oposición. Como no se transmite
ningún dato al editor, estos derechos se ejercen **directamente en la aplicación**:

- **Acceso y rectificación**: las franjas, los contactos y los ajustes son visibles y modificables
  en la aplicación (con el código).
- **Supresión**: desinstala la aplicación o borra sus datos desde
  *Ajustes → Aplicaciones → Instant Pause → Almacenamiento*. Todo desaparece con el almacenamiento
  privado.
- La protección anti-desinstalación impide una desinstalación impulsiva: desactiva primero el
  administrador de dispositivos en *Ajustes → Seguridad → Administración del dispositivo*, con tu
  código. Es una barrera contra ti mismo, no una retención de datos.

Base legal: ejecución del servicio que solicitas (artículo 6.1.b del RGPD). Ningún tratamiento
tiene lugar sin tu acción.

## 7. Seguridad

El código se conserva como una huella PBKDF2 con sal aleatoria y comparación en tiempo constante.
Los datos residen en el entorno aislado de Android. La aplicación no contiene ninguna clave de API
ni ningún secreto de servidor, y por tanto nada que robar a distancia.

## 8. Menores

La aplicación no está destinada a menores de 13 años y no recoge ningún dato sobre ellos.

## 9. Cambios

Cualquier evolución de esta política se publicará en esta dirección y se fechará en la cabecera del
documento. Si una versión futura introdujera un tratamiento de datos (por ejemplo, una
sincronización opcional), lo indicaría explícitamente antes de activarse.

---

# Politique de confidentialité — Instant Pause

[English](#privacy-policy--instant-pause) · [Español](#política-de-privacidad--instant-pause) · **Français**

**Dernière mise à jour : 3 novembre 2026**

Instant Pause (`com.instantpause.app`) est une application Android qui vous aide à résister aux
appels impulsifs. Cette politique explique, sans détour, quelles données l'application utilise et
ce qu'elle n'en fait pas.

## En une phrase

**Instant Pause ne collecte rien, n'envoie rien et n'a pas accès à Internet.** Tout ce que
l'application sait reste sur votre téléphone, dans son espace de stockage privé.

## 1. Responsable du traitement

L'éditeur de l'application est **JANEDOUBY** (projet Instant Pause, anciennement CoolDown).

Contact : **janedouby@gmail.com** — ou, pour un signalement public,
<https://github.com/AJANEDOUBY/instant-pause-privacy/issues>

Aucune donnée n'étant transmise à l'éditeur, il n'existe pas de registre de traitement à
consulter : l'application n'a aucun serveur.

## 2. Données traitées, et uniquement sur votre appareil

L'application enregistre localement les informations nécessaires à son fonctionnement :

| Donnée | Finalité | Stockage |
|---|---|---|
| Code de protection | Vérifier que c'est bien vous avant d'affaiblir la protection | Empreinte PBKDF2 salée (le code n'est **jamais** stocké en clair) |
| Plages horaires | Savoir quand intercepter les appels | Base Room privée (`cooldown.db`) |
| Contacts autorisés | Laisser passer certains appels, coupés à 3 minutes | Nom, numéro au format international, numéro affiché, région |
| Journal des blocages | Vous montrer les appels interceptés | Numéro, horodatage, motif — **purgé au-delà de 90 jours** |
| Réglages | État de la protection, mode Zen, préférences | DataStore privé (`cooldown_prefs`) |

Ces données sont stockées dans le **répertoire privé de l'application**, que les autres
applications ne peuvent pas lire sur un appareil non rooté.

### Les contacts autorisés

Vous choisissez un contact avec le **sélecteur de contacts d'Android**. L'application ne demande
**pas** la permission `READ_CONTACTS` : Android ne lui transmet que la fiche que vous avez
explicitement désignée. Le répertoire complet reste hors de portée de l'application.

### L'identifiant d'appel

Pour décider si un appel doit être intercepté, l'application reçoit de la part d'Android le numéro
composé, au moment de l'appel, via le rôle **Redirection d'appels** que vous avez accordé. Ce
numéro est analysé en mémoire, puis conservé dans le journal local décrit ci-dessus. Il n'est
transmis à personne.

## 3. Ce que l'application ne fait pas

- **Aucune connexion réseau.** L'application ne déclare pas la permission `INTERNET` et n'embarque
  aucun client HTTP.
- **Aucun compte.** Pas d'inscription, pas d'e-mail, pas d'identifiant.
- **Aucun analytics, aucun rapport de crash, aucun SDK publicitaire.**
- **Aucun partage avec un tiers.** Aucune donnée n'est vendue, louée ni transmise.
- **Aucune publicité, aucun profilage.**
- **Aucun accès** à vos SMS, votre position, votre micro, votre caméra ou votre répertoire.

## 4. Permissions demandées et raisons

| Permission | Pourquoi |
|---|---|
| Rôle **Redirection d'appels** (Telecom) | Intercepter l'appel sortant pour afficher l'écran de pause |
| `ANSWER_PHONE_CALLS` | Raccrocher automatiquement à 3 minutes un appel vers un contact autorisé |
| Administrateur d'appareil | Freiner une désinstallation impulsive. **Aucune politique** n'est imposée à l'appareil, aucun effacement à distance n'est possible |
| `SYSTEM_ALERT_WINDOW` / `USE_FULL_SCREEN_INTENT` | Afficher l'écran de pause par-dessus l'écran d'appel ou de verrouillage |
| `POST_NOTIFICATIONS` / `VIBRATE` | Notification permanente de la protection et retours haptiques |
| `RECEIVE_BOOT_COMPLETED` | Réactiver la protection après un redémarrage |
| `SCHEDULE_EXACT_ALARM` | Déclencher les plages à l'heure prévue |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Empêcher Android d'endormir le service de protection |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Maintenir la protection et le minuteur d'appel actifs |

Chaque permission peut être retirée depuis les réglages Android. Retirer le rôle Redirection
d'appels ou la permission de raccrochage désactive la protection correspondante : c'est volontaire,
l'application ne peut pas vous en empêcher.

## 5. Durée de conservation

- Plages, contacts et réglages : **jusqu'à la désinstallation** de l'application.
- Journal des blocages : **90 jours** au maximum, purgé automatiquement.
- Code de protection : empreinte conservée jusqu'à sa modification ou la désinstallation.

## 6. Vos droits (RGPD)

Vous disposez d'un droit d'accès, de rectification, d'effacement, de limitation et d'opposition.
Comme aucune donnée n'est transmise à l'éditeur, ces droits s'exercent **directement dans
l'application** :

- **Accès et rectification** : plages, contacts et réglages sont visibles et modifiables dans
  l'application (avec le code).
- **Effacement** : désinstallez l'application, ou effacez ses données depuis
  *Paramètres → Applications → Instant Pause → Stockage*. Tout disparaît avec le stockage privé.
- L'anti-désinstallation empêche une désinstallation impulsive : désactivez d'abord
  l'administrateur d'appareil dans *Paramètres → Sécurité → Administration de l'appareil*, avec
  votre code. C'est un garde-fou contre vous-même, pas une rétention de données.

Base légale : exécution du service que vous demandez (article 6.1.b du RGPD). Aucun traitement n'a
lieu sans votre action.

## 7. Sécurité

Le code est conservé sous forme d'empreinte PBKDF2 avec sel aléatoire et comparaison à temps
constant. Les données résident dans le bac à sable Android. L'application ne contient aucune clé
d'API, aucun secret serveur, et donc rien à dérober à distance.

## 8. Enfants

L'application n'est pas destinée aux enfants de moins de 13 ans et ne collecte aucune donnée les
concernant.

## 9. Modifications

Toute évolution de cette politique sera publiée à cette adresse et datée en tête de document. Si
une version future introduisait un traitement de données (par exemple la synchronisation
optionnelle), elle le signalerait explicitement avant activation.
