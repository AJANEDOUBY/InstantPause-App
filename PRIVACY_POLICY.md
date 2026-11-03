# Politique de confidentialité — Instant Pause

**Dernière mise à jour : 3 novembre 2026**

Instant Pause (identifiant `com.instantpause.app`) est une application Android qui vous aide à
résister aux appels impulsifs. Cette politique explique, sans détour, quelles données
l'application utilise et ce qu'elle n'en fait pas.

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

Vous choisissez un contact avec le **sélecteur de contacts d'Android**. L'application ne
demande **pas** la permission `READ_CONTACTS` : Android ne lui transmet que la fiche que vous
avez explicitement désignée. Le répertoire complet reste hors de portée de l'application.

### L'identifiant d'appel

Pour décider si un appel doit être intercepté, l'application reçoit de la part d'Android le
numéro composé, au moment de l'appel, via le rôle **Redirection d'appels** que vous avez
accordé. Ce numéro est analysé en mémoire, puis conservé dans le journal local décrit
ci-dessus. Il n'est transmis à personne.

## 3. Ce que l'application ne fait pas

- **Aucune connexion réseau.** L'application ne déclare pas la permission `INTERNET` et
  n'embarque aucun client HTTP.
- **Aucun compte.** Pas d'inscription, pas d'e-mail, pas d'identifiant.
- **Aucun analytics, aucun rapport de crash, aucun SDK publicitaire.**
- **Aucun partage avec un tiers.** Aucune donnée n'est vendue, louée ni transmise.
- **Aucune publicité, aucun profilage.**
- **Aucun accès à vos SMS, votre position, votre micro, votre caméra ou votre répertoire.**

## 4. Permissions demandées et raisons

| Permission | Pourquoi |
|---|---|
| Rôle **Redirection d'appels** (Telecom) | Intercepter l'appel sortant pour afficher l'écran de pause |
| `ANSWER_PHONE_CALLS` | Raccrocher automatiquement à 3 minutes un appel vers un contact autorisé |
| Administrateur d'appareil | Freiner une désinstallation impulsive. **Aucune politique** n'est imposée à l'appareil, aucun effacement à distance n'est possible |
| `SYSTEM_ALERT_WINDOW` / `USE_FULL_SCREEN_INTENT` | Afficher l'écran de pause par-dessus l'écran d'appel ou de verrouillage |
| `POST_NOTIFICATIONS`, `VIBRATE` | Notification permanente de la protection et retours haptiques |
| `RECEIVE_BOOT_COMPLETED` | Réactiver la protection après un redémarrage |
| `SCHEDULE_EXACT_ALARM` | Déclencher les plages à l'heure prévue |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Empêcher Android d'endormir le service de protection |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Maintenir la protection et le minuteur d'appel actifs |

Chaque permission peut être retirée depuis les réglages Android. Retirer le rôle Redirection
d'appels ou la permission de raccrochage désactive la protection correspondante : c'est
volontaire, l'application ne peut pas vous en empêcher.

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

Base légale : exécution du service que vous demandez (article 6.1.b du RGPD). Aucun
traitement n'a lieu sans votre action.

## 7. Sécurité

Le code est conservé sous forme d'empreinte PBKDF2 avec sel aléatoire et comparaison à temps
constant. Les données résident dans le bac à sable Android. L'application ne contient aucune
clé d'API, aucun secret serveur, et donc rien à dérober à distance.

## 8. Enfants

L'application n'est pas destinée aux enfants de moins de 13 ans et ne collecte aucune donnée
les concernant.

## 9. Modifications

Toute évolution de cette politique sera publiée à cette adresse et datée en tête de document.
Si une version future introduisait un traitement de données (par exemple la synchronisation
optionnelle), elle le signalerait explicitement avant activation.

## 10. English summary

Instant Pause does **not** collect, transmit or share any data. The app has no `INTERNET`
permission, no account, no analytics, no ads and no third-party SDK. Your passphrase (stored
as a salted PBKDF2 hash), schedules, allow-listed contacts and a block log (purged after 90
days) stay in the app's private storage on your device. All permissions are used only to
intercept calls, show the pause screen, hang up after 3 minutes and keep the protection alive;
each can be revoked in Android settings. Contact: **janedouby@gmail.com** —
<https://github.com/AJANEDOUBY/instant-pause-privacy/issues>
