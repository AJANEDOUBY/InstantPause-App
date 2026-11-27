# Formulaires de permissions — Play Console

Deux formulaires bloquent la release. Textes prêts à coller, **en anglais** : les
examinateurs Google lisent l'anglais, quelle que soit la langue de la Console. La version
française suit chaque bloc pour contrôle.

---

## 0. Accès à l'application (« Informations de connexion manquantes »)

**Politique → Contenu de l'application → Accès à l'application.**

Play signale ce blocage parce que son analyse automatique a vu des champs de type mot de passe
dans l'app. C'est `SetupScreen` : l'écran de **création** du code de protection, au premier
lancement. Ce n'est pas un écran de connexion — il n'y a ni compte, ni serveur, ni mot de passe
existant.

### Choix à faire

> **Certaines fonctionnalités de mon application sont restreintes**

Pas « toutes les fonctionnalités sont accessibles » : ce serait contredit par l'écran que
l'analyseur a détecté, et le bandeau reviendrait.

### Champs

| Champ | Valeur |
|---|---|
| Nom de l'instruction | `Local protection code (no account)` |
| Nom d'utilisateur | `not applicable` |
| Mot de passe | `review2026` |

### Instructions — version courte (444 caractères, limite 500)

Le champ est limité à **500 caractères**. Celle-ci en fait 444, comptés par script.

```
No account, no login, no server. At first launch, CREATE a protection code (not an existing credential): enter review2026 twice (6+ chars, one letter, one digit). It is only asked later to weaken protection. Then accept 3 normal consents: phone permission (3-min hang-up), call redirection role, full-screen intent. To test: call during an active window; the pause screen appears. Needs a SIM. Emergencies never blocked. No INTERNET permission.
```

### Secours (328 caractères)

Si la Console refuse encore, ou s'il reste une champ plus court :

```
No account, no server. On first launch, CREATE a code (not a login): type review2026 twice (6+ chars, letter+digit). It is only asked to weaken protection. Then accept phone permission, call redirection and full-screen intent. To test, call during an active window: the pause screen appears. Needs a SIM. No INTERNET permission.
```

### Version longue — pour un e-mail au support, pas pour le formulaire

```
There is no account, no login and no server. Everything runs offline on the device.

At first launch the app asks you to CREATE a local protection code. It is not an existing
credential: type the code below, twice, and it becomes the code for this installation.

  Suggested code: review2026
  Rules: at least 6 characters, at least one letter and one digit.

This code is only requested later, and only to weaken the protection: delete a time window
or remove an allowed contact. Browsing the app, creating a window or enabling Zen mode never
asks for it. If you forget it, clear the app data.

After creating the code, the onboarding asks for two system grants, which are normal Android
consents, not credentials:
  1. Call redirection role — the API that intercepts an outgoing call before it connects.
  2. Full-screen intent (optional) — shows the pause screen over the dialer.

It also offers the phone permission (ANSWER_PHONE_CALLS) to hang up at 3:00 on the trusted
contacts. That permission is optional: skipping it leaves blocking and the countdown intact,
and it can be granted later from the Security tab.

To verify the core feature, place an outgoing call to any non-emergency number during an
active time window: the pause screen appears instead of the call connecting. This requires a
device with a SIM card; call redirection is unavailable on Wi-Fi-only devices.

Emergency numbers are never intercepted, and the app has no INTERNET permission.
```

### Version française (contrôle)

Identique en substance. Le point à ne pas perdre : **le code est créé par l'examinateur**, il
n'existe pas à l'avance. Le champ « Mot de passe » sert à donner la valeur à saisir, pas une
valeur déjà enregistrée.

### Ne jamais mettre ici

- le code réellement utilisé sur votre tablette — il est propre à chaque installation, donc
  inutile, et c'est votre code personnel ;
- une adresse e-mail ou un mot de passe de compte Google : il n'y en a aucun.

---

## 1. `FOREGROUND_SERVICE_SPECIAL_USE`

**Choix dans la liste déroulante : « Autre » (Other).**

Aucun cas standard ne couvre cette application : ce n'est ni un lecteur média, ni un service
de localisation, ni un service de communication (elle ne passe ni ne reçoit d'appel — elle
les intercepte avant qu'ils partent). Choisir un cas standard serait un faux déclaratif.

### 1.a — Description de l'usage (champ texte principal)

```
Instant Pause blocks impulsive outgoing calls during time windows the user defines (for
example 10 pm to 7 am). The app runs two foreground services, both declared with the
specialUse type and two distinct subtypes:

1. blocking_impulsive_calls — started the instant a blocked outgoing call is intercepted.
   It lasts 3 seconds, shows the pause screen over the dialer, then stops itself.

2. countdown_for_trusted_contacts_calls — runs only during a call the user placed to one of
   the three trusted contacts they authorized. It displays a 3-minute countdown and, if the
   user granted the optional phone permission, hangs up at 0:00 using TelecomManager.endCall().

None of these services access the network, contacts, location, camera or microphone. The app
has no INTERNET permission at all. All data (time windows, three contacts, settings) stays in
the app's private storage on the device.
```

### 1.b — Pourquoi la tâche doit démarrer immédiatement et ne peut être ni mise en pause ni redémarrée

```
The pause service must start in the same second the call is placed. The user presses the call
button and Android Telecom is already connecting the call: if the service is delayed by even
one second, the call is connected and the pause screen appears after the fact, which defeats
the entire purpose of the app. Deferred execution (WorkManager, JobScheduler, alarms) has a
minimum latency of several minutes and cannot open an activity, so it cannot be used.

The service cannot be paused: pausing it means releasing the foreground state, at which point
Android freezes the process and the user is left on a half-shown pause screen while the call
proceeds in the background. It cannot be restarted either: it is a one-shot 3-second task. A
restart after the fact would display a stale pause screen for a call that has already been
placed or cancelled.

The countdown service must run continuously for the whole duration of an allowed call. If it
were paused or restarted, the countdown would reset and the 3-minute limit the user explicitly
configured would never be enforced.
```

### 1.c — Version française (contrôle, ne pas coller)

```
Instant Pause bloque les appels sortants impulsifs pendant des plages horaires définies par
l'utilisateur. Deux services de premier plan, tous en specialUse avec deux sous-types :
interception de l'appel (3 secondes, ouvre l'écran de pause) et minuteur de 3 minutes sur les
appels vers les contacts de confiance (raccrochage via TelecomManager.endCall si l'utilisateur
a accordé la permission facultative).
Aucun accès réseau, contacts, position, caméra ou micro. L'app n'a pas la permission INTERNET.
```

---

## 2. `USE_FULL_SCREEN_INTENT`

**Réponse à « Quelle est la fonctionnalité de base de votre appli ? » : « Autre ».**

### Pourquoi pas « Passer et recevoir des appels »

| Critère Google | Instant Pause |
|---|---|
| L'app est le composeur par défaut (`ROLE_DIALER`) | non, elle détient `ROLE_CALL_REDIRECTION` |
| Elle permet de passer **et** de recevoir des appels | non : elle empêche de passer |
| Elle affiche l'écran d'appel entrant | non |

Répondre « appels » serait un faux déclaratif pour obtenir un pré-accord indu. C'est
exactement le type de réponse qui déclenche un refus, voire une sanction de compte — sur un
compte qui a déjà un historique à construire.

### Pourquoi ce n'est pas bloquant

Le code gère déjà l'absence de pré-accord :

- `MainActivity` lit `NotificationManager.canUseFullScreenIntent()` au démarrage ;
- si l'accord manque, l'onboarding ouvre `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` pour que
  l'utilisateur l'accorde lui-même ;
- la notification de pause reste fonctionnelle sans l'accord : elle s'affiche en bannière
  (heads-up) pendant 60 secondes, et un appui ouvre l'écran de pause.

L'app fonctionne donc dans les deux cas. Le pré-accord n'apporte qu'un affichage plus direct.

### Texte de justification (si un champ apparaît)

```
Instant Pause is a call-blocking app, not a dialer and not a clock app. Its core feature is to
prevent impulsive outgoing calls during user-defined time windows.

When a call is placed inside a protected window, the app raises a full-screen pause screen so
the user can breathe and reconsider before the call goes through. This screen must appear
immediately on top of the dialer and the lock screen: a heads-up banner is not enough, because
the call is already being connected in the meantime.

The app does not hold the default-dialer role and does not place or receive calls. It only
holds the call-redirection role, which lets it intercept an outgoing call before it is
connected. The app checks canUseFullScreenIntent() at startup and sends the user to the system
settings page when the permission has not been granted, so it also works without the
pre-granted permission.
```

---

## 3. `ANSWER_PHONE_CALLS` (groupe Téléphone)

Aucun formulaire dédié n'est demandé pour cette permission tant que l'app ne lit pas le journal
d'appels. Mais elle fait partie du groupe « Téléphone » et peut être contestée à l'examen. Texte
de défense, à garder sous la main plutôt qu'à coller au hasard.

```
Instant Pause offers a hard 3-minute cap on calls to the three trusted contacts the user
explicitly authorizes. Those calls are allowed on purpose, but the user can ask the app to
guarantee they never exceed 3 minutes. The cap is optional: the user may decline the
permission during setup, in which case blocking and the countdown still work.

ANSWER_PHONE_CALLS is the only permission that grants a third-party app access to
TelecomManager.endCall(), the only API able to terminate an ongoing call. Without it the app
can only display a countdown that the user is free to ignore. There is no alternative API.

The app does not place calls, does not read or modify the call log, does not read phone state
or subscriber identifiers, and has no INTERNET permission.
```

### À vérifier avant de coller ce texte

Le raccrochage doit **réellement fonctionner** sur la tablette : appel vers un contact autorisé,
attente de 3:00. Si la ligne n'est pas coupée, ce texte est faux et Google aura raison de
refuser la permission. Voir `docs/FEUILLE-DE-ROUTE.md`, section 2.

---

## 4. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - retirée en 1.7.0

Cette permission n'est **plus déclarée ni demandée** depuis la 1.7.0. Google Play la traite
comme une permission à usage restreint et exige de prouver que l'optimisation batterie dégrade
réellement la fonction principale ; le risque d'examen ne justifiait plus le gain.

L'application n'envoie donc plus `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Quand le
système optimise encore l'app, l'écran principal affiche un bandeau qui ouvre simplement les
**réglages batterie du système** (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`) : c'est
l'utilisateur qui choisit « Sans restriction » pour Instant Pause, sans dialogue ni permission
spéciale. Il n'y a donc **aucun formulaire à remplir** pour cette permission dans la Console.

Le comportement observable reste le même : processus gelé = appel non intercepté, sans message.
Si un jour la permission revient, la justification détaillée est conservée dans l'historique
Git (section 4 avant la 1.7.0).

---

## 5. `ACCESS_NOTIFICATION_POLICY` (mode « Ne pas déranger »)

Permission de niveau normal, mais l'accès reste soumis à un accord explicite de
l'utilisateur dans les réglages système. Play Console demande une déclaration
d'usage pour cette permission : la coller telle quelle en anglais.

### Texte de déclaration (anglais)

```
Instant Pause helps people avoid impulsive late-night phone calls. When the user
explicitly enables the optional "Silence during the pause" setting, the app switches
the device to the Do Not Disturb "priority only" filter while a user-defined
protection window is running, so that VoIP calls (WhatsApp, Signal, Telegram) do not
ring. The filter is restored to its exact previous value when the window ends, when
the user turns the option off, or after a reboot. The setting is off by default, is
never enabled automatically, and the app never reads notifications. Alarms and
priority conversations keep ringing, so emergency calls remain possible.
```

### À vérifier avant de soumettre

1. L'option est bien désactivée par défaut (`dnd_enabled` absent = faux).
2. Aucun écran ne demande l'accès au premier lancement : seule la carte de l'onglet
   Sécurité propose « Autoriser ».
3. Après un refus, l'app n'insiste pas et le téléphone sonne normalement (fail-open).
4. La valeur d'origine est restaurée à l'identique (`dnd_previous_filter`).
5. Une coupure manuelle de l'utilisateur n'est jamais annulée par l'app.

### Cohérence avec la politique de confidentialité

Depuis le 16 novembre 2026, la page publique
<https://ajanedouby.github.io/instant-pause-privacy/> décrit cette permission et le stockage
de `dnd_previous_filter`, dans les trois langues (dépôt `instant-pause-privacy`, commit
`fd70620`). Si le texte ci-dessus change, mettre la page à jour dans le même mouvement :
un examinateur peut comparer les deux.

---

## 6. Script de la vidéo de démonstration (`specialUse`)

Contraintes : **montrer l'app réelle en fonctionnement**, pas de diapositives, pas de montage
trompeur. 60 à 90 secondes suffisent. Google veut voir la tâche visible par l'utilisateur
pendant qu'il n'interagit pas avec l'app.

### Préparation

1. Tablette `R5GL350ZJ9P`, version 1.7.0 installée, plage active (par exemple 05:55-07:55) et
   les trois contacts autorisés configurés.
2. Mode avion **désactivé** (l'app n'utilise pas le réseau, mais un écran d'erreur réseau
   parasite la démonstration).
3. Préparer un numéro **fictif** à composer (`0123456789`). Ne jamais appeler un vrai contact
   ni un numéro d'urgence pendant l'enregistrement.
4. Mettre l'interface en français ou en anglais — peu importe, l'examinateur lit les sous-titres.

### Enregistrement

```
adb -s R5GL350ZJ9P shell screenrecord --bit-rate 8000000 --time-limit 120 /sdcard/Download/fgs-demo.mp4
```

### Déroulé

| Temps | Ce qu'on voit | Ce que ça prouve |
|---|---|---|
| 0:00 | Écran d'accueil de l'app, protection active, plage horaire affichée | l'app est configurée |
| 0:10 | Écran d'accueil de l'app, plage horaire affichée | l'app est configurée |
| 0:20 | Ouvrir le composeur, taper `0123456789`, appuyer sur Appeler | geste utilisateur réel |
| 0:24 | L'écran de pause s'affiche **par-dessus** le composeur, avec la notification de service | service 1 (`blocking_impulsive_calls`) + `USE_FULL_SCREEN_INTENT` |
| 0:32 | Revenir à l'app, montrer le journal : « appel bloqué » | l'interception a bien eu lieu |
| 0:40 | Appeler un contact de confiance (numéro de test) | cas autorisé |
| 0:46 | Pastille de décompte « 3:00 → 2:xx » par-dessus l'écran d'appel | service 2 (`countdown_for_trusted_contacts_calls`) |
| 1:10 | Raccrocher manuellement, arrêter l'enregistrement | pas de démonstration d'un raccrochage automatique si le constructeur le refuse |

### Récupération

```
adb -s R5GL350ZJ9P pull /sdcard/Download/fgs-demo.mp4
```

Puis déposer le fichier dans le champ vidéo du formulaire. Format MP4 accepté, taille
raisonnable (quelques Mo). Ne pas flouter : la vidéo doit rester lisible.

### Deux pièges

- **Ne pas couper l'enregistrement pendant l'écran de pause** : c'est le seul moment qui
  justifie la permission. Il doit durer au moins 5 secondes à l'écran.
- **Ne pas montrer de numéro réel** dans le journal ni dans les contacts : un examen qui voit
  un vrai numéro personnel peut demander des comptes sur la vie privée des tiers.
