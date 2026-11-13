# 1.6.0 - Mode Ne pas déranger optionnel pendant les plages

Spécification. **Aucun code n'est écrit pour l'instant** : la 1.4.0 est en test fermé et la
1.5.0 attend la publication. Ce document sert à décider, puis à chiffrer.

---

## 1. Objectif

Pendant une plage de protection, ou pendant le mode Zen, **silencieux les appels et
notifications des applications VoIP** (WhatsApp, Signal, Telegram, Messenger) que
l'application ne peut pas intercepter.

Ce n'est pas du blocage : l'appel WhatsApp reste possible, mais le téléphone ne sonne pas.
L'utilisateur n'est pas mis en échec par une sonnerie à 23 h.

## 2. Ce que cela fait, ce que cela ne fait pas

| Fait | Ne fait pas |
|---|---|
| active le filtre « Ne pas déranger » pendant la plage | n'intercepte pas les appels WhatsApp |
| laisse passer alarmes et conversations prioritaires | ne coupe pas la ligne |
| restaure le filtre précédent à la fin | ne modifie pas les règles DND de l'utilisateur |
| fonctionne sans réseau et sans compte | ne bascule jamais en mode avion |
| se désactive d'un seul geste | ne demande aucune permission d'accessibilité |

## 3. Pourquoi le mode avion est exclu

| Critère | Ne pas déranger | Mode avion |
|---|---|---|
| Permission | `ACCESS_NOTIFICATION_POLICY`, accordée par l'utilisateur | `WRITE_SECURE_SETTINGS`, impossible pour une app grand public |
| Play Store | accepté, usage courant | refusé |
| Appels entrants | filtrés selon les règles de l'utilisateur | tous coupés |
| Urgences | possibles | comportement variable, non garanti |
| Réversibilité | automatique | oubli fréquent |
| Image du produit | apaise | punit |

Le mode avion est donc à la fois **impossible** et **contraire au principe fail-open** de
l'application.

## 4. Mécanique Android

Aucune nouvelle dépendance. Tout passe par `NotificationManager`.

### 4.1 Déclaration

```xml
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
```

Permission de niveau `normal`, mais **l'accès reste soumis à un accord explicite** de
l'utilisateur, donné dans les réglages système.

### 4.2 Vérification et demande

```kotlin
val nm = context.getSystemService(NotificationManager::class.java)
val granted = nm.isNotificationPolicyAccessGranted
if (!granted) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
}
```

Le retour se fait dans l'application ; l'état est relu à chaque reprise (`onResume`).

### 4.3 Application

```kotlin
nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
```

`PRIORITY` est le **seul filtre cohérent** avec la promesse des trois contacts de confiance :

- `ALARMS` couperait aussi les appels des proches ;
- `TOTAL_SILENCE` couperait les alarmes ;
- `PRIORITY` laisse passer alarmes, sonneries autorisées et conversations prioritaires.

### 4.4 Limite honnête à afficher dans l'interface

Le filtre `PRIORITY` obéit aux **règles DND de l'utilisateur**, que l'application ne modifie
jamais. Un contact de confiance ne sonnera pendant la pause que s'il est :

- marqué comme favori, ou
- configuré comme « conversation prioritaire » dans les réglages DND d'Android.

L'écran d'activation doit le dire explicitement, avec un lien vers ces réglages. Sans cela,
l'application promet « vos trois contacts restent joignables » et ne le tient pas.

## 5. Cycle de vie et restauration

Le filtre DND **survit à la fermeture de l'application** : c'est le point le plus risqué.
Il faut donc le traiter comme un état persistant, pas comme un simple effet.

### 5.1 Données à conserver (DataStore)

| Clé | Rôle |
|---|---|
| `dnd_enabled` | l'utilisateur a activé l'option |
| `dnd_active` | l'application a posé le filtre et doit le retirer |
| `dnd_previous_filter` | valeur exacte de `interruptionFilter` avant modification |

### 5.2 Règles

1. **Avant d'appliquer** : si `dnd_active` est déjà vrai, ne pas écraser
   `dnd_previous_filter`. Sinon, enregistrer le filtre courant.
2. **À la fin** de la plage ou du Zen : restaurer `dnd_previous_filter`, puis remettre
   `dnd_active` à faux.
3. **Au redémarrage** : un `BOOT_COMPLETED` relit `dnd_active`. Si la plage est finie,
   restaure immédiatement ; sinon réapplique.
4. **Si l'accès est retiré** en cours de route : ne plus appliquer, mais ne pas prétendre
   avoir restauré. Afficher un avertissement dans l'onglet Sécurité.
5. **Désactivation manuelle** par l'utilisateur dans les réglages rapides : le service de
   veille détecte `interruptionFilter != PRIORITY` et remet `dnd_active` à faux, sans
   réappliquer — l'utilisateur reprend la main.
6. **Désinstallation** : le filtre peut rester. Le documenter dans l'aide ; un écran de
   désinstallation peut proposer « Rendre le son » avant de partir.

### 5.3 Point d'intégration

Le calcul « une plage est-elle active maintenant ? » existe déjà dans
`ShouldBlockCallUseCase`, mais il est privé et lié à une décision d'appel. Il faut extraire
un `ProtectionState` (plage active, Zen actif) réutilisé par :

- `ShouldBlockCallUseCase` ;
- le bandeau de `ScheduleScreen` ;
- le futur contrôleur DND.

Cela évite trois définitions divergentes de « protection active ».

## 6. Interface

Dans l'onglet **Sécurité**, sous la carte du message de pause :

- titre : « Silence pendant la pause » ;
- interrupteur, désactivé par défaut ;
- sous-titre : « Coupe les sonneries WhatsApp, Signal et Telegram pendant vos plages. Vos appels d'urgence et vos alarmes restent possibles. » ;
- si l'accès n'est pas accordé : bouton « Autoriser » qui ouvre les réglages système ;
- si accordé : « Accès accordé » ;
- lien « Configurer mes contacts prioritaires » vers les réglages DND d'Android.

Aucune demande automatique au premier lancement : l'option est proposée, jamais imposée.

## 7. Justification Play Console

À saisir dans la déclaration d'accès au mode Ne pas déranger, en anglais :

```
Instant Pause helps people avoid impulsive late-night phone calls. When the user
explicitly enables the optional "Silence during pause" setting, the app switches the
device to the Do Not Disturb interruption filter for the duration of the protection
window the user has configured.

The app never changes the user's Do Not Disturb rules, never enables DND without an
in-app toggle, and restores the previous interruption filter when the window ends.
Alarms, emergency calls and priority conversations remain allowed. The feature is off
by default and can be disabled at any time.

No accessibility service is used. No call content is read. No data leaves the device.
```

Version courte, si le champ est limité :

```
Optional user-enabled Do Not Disturb during the user's own protection schedule:
silences VoIP notifications (WhatsApp, Signal, Telegram) without blocking calls.
Restores the previous filter afterwards. Off by default, no accessibility, no data
collection.
```

## 8. Tests

### 8.1 Tests unitaires (JVM)

| Test | Vérifie |
|---|---|
| filtre précédent mémorisé | `previous = RING`, application de `PRIORITY` |
| double activation | `previous` n'est pas écrasé par `PRIORITY` |
| fin de plage | retour exact au filtre précédent |
| accès refusé | aucun appel à `setInterruptionFilter` |
| redémarrage avec plage finie | restauration immédiate |
| désactivation manuelle | `dnd_active` retombe à faux |

Le contrôleur doit être un objet pur, testable avec un faux `InterruptionFilterStore` et
une horloge injectée, comme `AppLockManager`.

### 8.2 Tests manuels

1. Activer l'option, vérifier le dialogue système, revenir.
2. Pendant une plage : appel WhatsApp entrant → pas de sonnerie.
3. Appel SIM d'un contact favori → sonne (filtre PRIORITY).
4. Alarme → sonne.
5. Fin de plage → le téléphone resonne normalement, y compris WhatsApp.
6. Redémarrage pendant la plage → DND réappliqué.
7. Redémarrage après la plage → DND retiré.
8. Retirer l'accès depuis les réglages → l'app le signale, ne bloque rien.
9. Désactiver DND depuis le volet → l'app ne le réactive pas.

Appareils : au moins un Samsung (One UI), un Android stock et une version Android 14+.

## 9. Risques

| Risque | Gravité | Mitigation |
|---|---|---|
| DND laissé actif après un arrêt brutal | **élevée** | restauration au démarrage + veille + avertissement |
| Contacts de confiance silencieux | **élevée** | explication et lien vers les réglages prioritaires |
| Utilisateur croit que WhatsApp est bloqué | moyenne | texte explicite « silence, pas blocage » |
| Refus Play de la permission | faible | usage courant, justification fournie, opt-in |
| Filtre modifié par une autre app | faible | détection et abandon, jamais de réapplication |

## 10. Estimation

| Tâche | Durée |
|---|---|
| Extracteur `ProtectionState` + tests | 0,5 j |
| Contrôleur DND + DataStore + tests | 1 j |
| Interface Sécurité + onboarding de l'accès | 0,5 j |
| Redémarrage, veille, restauration | 0,5 j |
| Tests manuels multi-appareils | 1 j |
| **Total** | **3 à 4 jours** |

Aucune permission sensible, aucun service d'accessibilité, aucun risque R8 particulier.

## 11. Non-objectifs

- Intercepter ou raccrocher un appel WhatsApp.
- Modifier le mode avion.
- Modifier les règles DND de l'utilisateur.
- Lire le contenu des notifications.
- Demander l'accès à l'accessibilité.

## 12. Décision attendue

À valider après le test fermé, avec les réponses des testeurs :

- combien utilisent WhatsApp pour leurs appels impulsifs ?
- combien accepteraient de configurer leurs contacts prioritaires ?

Si la seconde réponse est faible, l'option reste utile mais ne doit pas être présentée
comme une promesse de joignabilité des proches.
