# Formulaires de permissions — Play Console

Deux formulaires bloquent la release. Textes prêts à coller, **en anglais** : les
examinateurs Google lisent l'anglais, quelle que soit la langue de la Console. La version
française suit chaque bloc pour contrôle.

---

## 1. `FOREGROUND_SERVICE_SPECIAL_USE`

**Choix dans la liste déroulante : « Autre » (Other).**

Aucun cas standard ne couvre cette application : ce n'est ni un lecteur média, ni un service
de localisation, ni un service de communication (elle ne passe ni ne reçoit d'appel — elle
les intercepte avant qu'ils partent). Choisir un cas standard serait un faux déclaratif.

### 1.a — Description de l'usage (champ texte principal)

```
Instant Pause blocks impulsive outgoing calls during time windows the user defines (for
example 10 pm to 7 am). The app runs three foreground services, all declared with the
specialUse type and three distinct subtypes:

1. blocking_impulsive_calls — started the instant a blocked outgoing call is intercepted.
   It lasts 3 seconds, shows the pause screen over the dialer, then stops itself.

2. countdown_for_trusted_contacts_calls — runs only during a call the user placed to one of
   the three trusted contacts they authorized. It displays a 3-minute countdown and hangs up
   the line at 0:00 using TelecomManager.endCall().

3. call_protection_watchdog — a silent, minimum-importance notification that keeps the
   protection alive on devices whose manufacturer aggressively kills background apps.

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
l'utilisateur. Trois services de premier plan, tous en specialUse avec trois sous-types :
interception de l'appel (3 secondes, ouvre l'écran de pause), minuteur de 3 minutes sur les
appels vers les contacts de confiance (raccrochage via TelecomManager.endCall), et veille
silencieuse qui maintient la protection sur les téléphones qui tuent les apps en arrière-plan.
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

## 3. Script de la vidéo de démonstration (`specialUse`)

Contraintes : **montrer l'app réelle en fonctionnement**, pas de diapositives, pas de montage
trompeur. 60 à 90 secondes suffisent. Google veut voir la tâche visible par l'utilisateur
pendant qu'il n'interagit pas avec l'app.

### Préparation

1. Tablette `R5GL350ZJ9P`, version 1.4.0 installée, plage active (par exemple 05:55–07:55) et
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
| 0:10 | Volet de notifications : « Instant Pause veille — Votre protection est active. » | service 3 (`call_protection_watchdog`) visible et permanent |
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
