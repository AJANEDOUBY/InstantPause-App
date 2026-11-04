# Revue produit — onboarding, permissions, feuille de route

Analyse des remarques reçues après la 1.4.0. Chaque point est confronté au code réel, pas à
l'intention.

---

## 1. Onboarding lourd — vrai, et il y a un défaut précis

Six écrans avant la première utilisation : code, administrateur d'appareil, `ANSWER_PHONE_CALLS`,
rôle Telecom, plein écran. Plus, hors onboarding et non bloquants : overlay, notifications,
exemption batterie.

Le problème n'est pas le nombre. C'est **l'ordre**.

Dans `UninstallGuardScreen`, la demande système d'administrateur est lancée **automatiquement**
dès l'affichage de l'écran :

```kotlin
// UninstallGuardScreen.kt
LaunchedEffect(Unit) { onActivate() }
```

L'utilisateur reçoit donc le dialogue Android le plus effrayant de la liste **avant d'avoir lu
une seule ligne d'explication**. Le texte est là, derrière, mais personne ne le lit : l'écran
système l'a recouvert.

**Correction (1.5.0, 10 minutes)** : supprimer le `LaunchedEffect`, laisser l'utilisateur lire
puis appuyer sur « Protéger la désinstallation ». C'est un changement d'une ligne, et c'est le
gain de confiance le plus élevé du lot.

**Ajout utile** : un bandeau « pourquoi » avant chaque écran système, avec une phrase et une
icône. Trois des cinq écrans en ont déjà un. Les deux qui manquent : `HangupPermissionScreen`
et `RoleActivationScreen`.

---

## 2. `ANSWER_PHONE_CALLS` — le vrai risque, et il est mal mesuré

La permission est justifiée par une seule chose : le raccrochage à 3:00 sur les contacts de
confiance. Si ce raccrochage ne marche pas sur l'appareil de l'utilisateur, la justification
s'effondre — et Google peut la retirer.

Or le code contient une contradiction :

- `CallTimerService` commence par un commentaire qui dit que l'app **ne coupe pas** la ligne ;
- plus bas, la même classe appelle `TelecomManager.endCall()` et journalise l'échec.

```kotlin
return runCatching { telecom.endCall() }
    .onFailure { Log.w(TAG, "endCall() a échoué", it) }
```

**À faire avant la déclaration de permissions** : appeler un contact de confiance sur la
tablette et attendre 3:00. Deux issues :

| Résultat | Conséquence |
|---|---|
| La ligne est coupée | justification solide, on garde la permission |
| La ligne n'est pas coupée | la promesse est fausse, Google a raison de la contester |

Si la coupure échoue, deux options : retirer `ANSWER_PHONE_CALLS` et assumer un simple
décompte avec bouton « Raccrocher » (l'utilisateur appuie), ou changer la promesse. La première
supprime d'un coup un tiers du risque Play.

---

## 3. Administrateur d'appareil optionnel — **je ne le ferais pas**

C'est le seul point de la revue que je conteste.

L'argument « beaucoup d'utilisateurs refusent » est vrai. Mais l'utilisateur qui refuse une
protection anti-désinstallation est exactement celui qui désinstallera à 23 h 40, en pleine
impulsion. Le rendre optionnel, c'est garantir qu'il sera sauté par la personne à qui l'app
sert.

Le produit ne vend pas du blocage, il vend une décision prise **à froid**. L'administrateur
d'appareil est cette décision, matérialisée. Le retirer, c'est retirer le produit.

**Compromis acceptable** : garder l'étape obligatoire, mais permettre un refus **explicite**
après un second écran qui énonce la conséquence (« l'app pourra être désinstallée en trois
gestes »), avec un bandeau permanent dans l'onglet Plages. On garde la friction sur la
mauvaise décision, pas sur la bonne.

À trancher avec les 12 testeurs : s'ils bloquent tous à cette étape, le compromis devient la
règle. Pas avant.

---

## 4. Le verrou n'arrête pas une personne déterminée — exact, et déjà assumé

`adb`, effacement des données, mode sans échec, désactivation de l'administrateur : tout cela
reste possible. C'est déjà écrit dans la fiche et dans la politique de sécurité. La bonne
posture est celle-ci, et il n'y a rien à corriger.

Une seule nuance à ne pas perdre : **le code n'est pas une serrure, c'est un délai**. C'est ce
qui justifie qu'il soit obligatoire, et c'est ce qui justifie qu'on ne prétende jamais à
davantage.

---

## 5. Changement d'identifiant — moins grave qu'annoncé, mais le besoin est réel

Il n'y a **aucun utilisateur public** : l'app n'est jamais sortie du test fermé. La perte de
données est donc théorique. Le seul coût concret est la coexistence de deux paquets sur la
tablette de développement.

En revanche, l'export/import local reste utile pour deux raisons futures :

- changer de téléphone sans compte ni cloud ;
- permettre une réinstallation propre après un test.

**Spécification** :

- export JSON via `ACTION_CREATE_DOCUMENT` (aucune permission) ;
- contenu : plages, contacts autorisés en E.164, préférences, version de format ;
- **exclure** : code de protection (même haché), journal des appels, jetons ;
- import : validation stricte, refus si version inconnue, jamais d'écrasement silencieux.

`allowBackup="false"` reste : l'export est un geste volontaire, pas une synchronisation.

---

## 6. Les cinq suggestions, notées

| # | Suggestion | Verdict | Coût | Permission nouvelle |
|---|---|---|---|---|
| 1 | Pause de 180 s même hors plage, volontaire | ✅ bonne | moyen | aucune |
| 2 | Choix après la pause | ✅ bonne, **avec un piège** | moyen | voir ci-dessous |
| 3 | Statistiques bienveillantes | ✅ bonne | faible | aucune |
| 4 | Widget / tuile Zen | ✅ la plus rentable | faible | aucune |
| 5 | Texte personnalisé sur l'écran de pause | ✅ **la meilleure** | très faible | aucune |
| 6 | Vidéo de démonstration Play | déjà scénarisée | fait | — |

### Le piège du point 2

« Appeler quand même » suppose que l'app replace l'appel. Aujourd'hui, `PauseActivity` ne
propose que « Fermer » : l'appel a été **annulé** par `cancelCall()`, il n'est pas en attente.

Deux façons de replacer l'appel :

- `ACTION_CALL` → exige `CALL_PHONE`, **nouvelle permission sensible**, nouveau dossier Play.
  À éviter absolument.
- `ACTION_DIAL` prérempli → aucune permission, l'utilisateur appuie une fois sur l'appel.
  Un geste de plus, zéro dossier.

**Choisir `ACTION_DIAL`.** « Appeler quand même » qui rouvre le composeur est honnête : la
pause a fait son travail, l'utilisateur confirme.

« Reporter dans 1 heure » ne doit **pas** passer un appel plus tard : une notification locale
suffit, sans permission et sans réseau.

### Pourquoi le point 5 est le meilleur

L'écran de pause affiche aujourd'hui « Cet appel peut attendre demain. » — une phrase choisie
par le développeur. Une phrase écrite par l'utilisateur **quand il est calme** aura dix fois
plus de poids au moment de la crise, parce qu'elle vient de lui.

Coût : un champ texte, une préférence, un remplacement de ligne. Aucune permission, aucun
risque Play. C'est le meilleur rapport valeur/effort de toute la liste.

### Pourquoi le point 4 est le plus utilisé

Le mode Zen existe et vaut 2 heures. Aujourd'hui il faut ouvrir l'app, aller sur Plages,
appuyer. Une tuile dans le volet de réglages rapides le met à un geste. `TileService`, une
classe, aucune permission.

---

## 7. Ordre d'exécution

### Maintenant — rien
La 14 est en attente d'examen. Toute modification impose un `versionCode` 15 et relance le
dossier de permissions. **Ne pas toucher au code avant la publication du test fermé.**

### Pendant les 14 jours — mesurer
- Question 1 aux testeurs : à quel écran avez-vous hésité ?
- Question 2 : avez-vous refusé l'administrateur d'appareil ?
- Vérifier que le raccrochage à 3:00 fonctionne sur au moins deux marques de téléphone.

### 1.5.0 — corrections issues du terrain
1. Bandeau à trois états + horloge (défaut constaté).
2. Supprimer le `LaunchedEffect` de `UninstallGuardScreen`.
3. Écrans « pourquoi » sur raccrochage et rôle Telecom.
4. Texte personnalisé de l'écran de pause.
5. Tuile Zen.

### 1.6.0 — produit
6. Pause volontaire hors plage.
7. Choix après la pause (`ACTION_DIAL`, notification de report).
8. Statistiques locales.
9. Export / import JSON.

Rien de tout cela ne justifie de retarder la 1.4.0.
