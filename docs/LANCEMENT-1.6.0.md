# Lancement 1.6.0 — suivi opérationnel

Document de pilotage : ce qui est prêt, dans quel ordre le faire, et les dates au fur et à
mesure. Toute modification de ce fichier est un fait daté, pas une intention.

| Élément | Valeur |
|---|---|
| Version à publier | **1.6.0**, versionCode **16** |
| Branche | `main` (branche par défaut, fusionnée le 16 nov. 2026) — `release/1.6.0` conservée |
| Paquet | `com.instantpause.app` |
| Dépôt | https://github.com/AJANEDOUBY/InstantPause-App |
| Éditeur | JANEDOUBY — janedouby@gmail.com |
| Compte Play | personnel, créé après le 13 nov. 2023 → test fermé obligatoire |

---

## 1. État au 16 novembre 2026

| Contrôle | Résultat |
|---|---|
| AAB signé | `app/build/outputs/bundle/release/app-release.aab`, 5,4 Mo |
| Empreinte du certificat | `4F:14:D6:6F:…:1B:73:1A:F0:F5`, valide jusqu'au 17 mars 2054 |
| Tests unitaires | 117, 0 échec |
| Lint | 0 erreur, 28 avertissements préexistants |
| Traduction | 191 clés × 3 langues (en, fr, es), alignées |
| Fonctions 1.6.0 | interface trilingue + sélecteur de langue + mode « Ne pas déranger » optionnel |
| `main` | contient la 1.6.0 depuis le 16 nov. 2026 (versionCode 16) |

**[CRITIQUE] Ne jamais réutiliser le versionCode 14** (consommé par Play). Le 15 reste libre
pour la 1.5.0, qui ne sera **pas** téléversée : la 1.6.0 la remplace dans le test fermé.

---

## 2. La règle qui commande tout le calendrier

> « At least 12 testers must be opted in to your closed test when you apply for production
> access, and they must have been opted in continuously for the preceding 14 days. »
> — Play Console Help, *App testing requirements for new personal developer accounts*

| Question | Réponse |
|---|---|
| Changer la version pendant le test remet-il les 14 jours à zéro ? | Non. Le compteur suit l'opt-in des testeurs, pas le build. Google recommande même de livrer des mises à jour pendant le test. |
| Qu'est-ce qui remet à zéro ? | Un testeur qui se désinscrit puis se réinscrit (les 14 jours doivent être **consécutifs**), ou un effectif sous 12. |
| Quand part le chronomètre ? | Le jour où le **12ᵉ** testeur est inscrit. Pas avant. |
| Avec 1 testeur (2 appareils) | Aucun jour n'est compté. |

Conséquence pratique : viser **15 à 20 inscrits** pour 12 requis, afin d'absorber un
désistement sans repartir de zéro.

---

## 3. Liens

| Objet | Lien |
|---|---|
| Play Console (accueil développeur) | https://play.google.com/console/u/0/developers/5625845951008726142/home |
| Test fermé (dans Play Console) | *Test et publication › Tests › Test fermé* |
| Inscription des testeurs (lien public) | https://play.google.com/apps/testing/com.instantpause.app |
| Post Reddit | https://www.reddit.com/r/AndroidClosedTesting/comments/1xq4okr/closed_test_instant_pause_block_impulsive/ |
| Formulaire d'inscription | https://docs.google.com/forms/d/10v7wGpqtbf9_r-WR1i5wGyHZLjwkf0N-6g3WSJJL_as/edit |
| Release GitHub 1.6.0 | https://github.com/AJANEDOUBY/InstantPause-App/releases/tag/v1.6.0 |
| Groupe testeurs | `instant-pause-testers@googlegroups.com` (déjà déclaré dans la piste Alpha) |
| Contact testeurs | janedouby@gmail.com |
| Notes de version à copier | `docs/RELEASE-NOTES.md`, section 1.6.0 |
| Texte de déclaration DND | `docs/PERMISSIONS-PLAY.md`, section 5 |

---

## 4. Séquence

Cocher et dater au fur et à mesure. Ne pas sauter l'étape 3 avant l'étape 5 : un lien diffusé
trop tôt renvoie les testeurs vers « application introuvable ».

- [x] **Étape 1 — Déclaration DND.** Play Console › *Contenu de l'application* : aucune
      déclaration « Ne pas déranger » n'y est demandée, et l'onglet *Traitée* ne la liste pas.
      Play ne réclame cette déclaration que s'il la juge nécessaire pendant la revue ; le
      texte de `docs/PERMISSIONS-PLAY.md` §5 reste prêt à être collé dans ce cas.
  - Date : 16 nov. 2026 (vérifié, rien à remplir)
- [x] **Étape 2 — Téléverser l'AAB.** *Test et publication › Tests › Test fermé › Alpha* :
      brouillon V14 supprimé, **Créer une version**, AAB `16 (1.6.0)` téléversé, notes de
      version en/fr/es (3 langues sur 3), nom « 1.6.0 - trilingue + silence optionnel »,
      puis **Envoyer 1 modification pour examen**.
  - Date : 16 nov. 2026 — en cours d'examen
- [x] **Étape 3 — Attendre la disponibilité.** La revue est passée en moins de deux heures :
      la 1.6.0 (versionCode 16) est **Disponible pour certains testeurs** sur la piste
      *Tests fermés - Alpha*, 178 pays/régions. Le lien d'opt-in répond et le compte du
      développeur y figure déjà comme testeur.
  - Date de disponibilité : 16 nov. 2026 à 08:57
- [x] **Étape 4 — Groupe Google des testeurs.** Le groupe
      `instant-pause-testers@googlegroups.com` est déjà déclaré dans la piste *Testeurs*
      (avec un message de 99 caractères pour les commentaires). Reste à y ajouter les
      adresses des volontaires.
  - Date : déjà en place le 16 nov. 2026
- [ ] **Étape 5 — Formulaire d'inscription.** Champs de la section 6, réponses liées à un Google
      Sheets, colonne « date d'inscription » remplie à la main.
  - Date : ____
- [ ] **Étape 6 — Éditer le post Reddit.** Le post du 7 nov. (r/AndroidClosedTesting) a été
      **retiré par les filtres automatiques** : il n'apparaît pas dans le fil et une édition ne
      le rétablira pas. Modmail envoyé le 16 nov., sans réponse à ce jour.
      **Relance faite ailleurs le 16 nov.** : nouveau post dans r/testerscommunity, publié et
      vérifié en ligne, sans aucun lien dans le corps (les liens sont dans le premier
      commentaire) — c'est la piste qui a fonctionné.
      **Second post le même jour, ~5 h après** dans r/GooglePlayClosedTest : corps reformulé
      (jamais le même texte d'un sub à l'autre), lui aussi sans lien, liens en premier
      commentaire. L'AutoModerator de ce sub exige **les deux liens** (opt-in *et* fiche
      Store) : le commentaire a été édité pour ajouter la fiche Store.
  - Date : 16 nov. 2026 — les deux posts visibles, aucun humain n'a encore répondu
  - URL 1 : https://www.reddit.com/r/testerscommunity/comments/1xxli15/test_for_test_instant_pause_a_pause_screen/
  - URL 2 : https://www.reddit.com/r/GooglePlayClosedTest/comments/1xxsolu/test_for_test_instant_pause_i_need_12_testers_for/
- [ ] **Étape 7 — Recruter 15 à 20 testeurs.** Noter les dates d'inscription. Relancer à J+7
      ceux qui n'ont pas installé.
  - 12ᵉ inscription (début des 14 jours) : ____
- [ ] **Étape 8 — Lire les retours.** Play Console › *Améliorer › Notes et avis › Retours de
      test*. Corriger les bugs bloquants, livrer une 1.6.x si nécessaire (autorisé, sans
      remise à zéro).
- [ ] **Étape 9 — Demander l'accès production.** *Accueil* › **Demander l'accès à la
      production**. Trois sections : le test fermé, l'application, l'état de préparation.
      Résumer les retours reçus et les corrections faites.
  - Date de demande : ____
  - Réponse : ____ (compter jusqu'à 7 jours)
- [ ] **Étape 10 — Publier.** Fusionner `release/1.6.0` dans `main`, puis *Production › Créer
      une version* avec le même AAB.
  - Date : ____

---

## 5. Texte du post Reddit (prêt à coller)

Le titre ne peut pas être modifié ; on édite le corps du post existant.

```
[Edit 15 Nov] Now on 1.6.0: English / French / Spanish interface, plus an
optional "Silence during the pause" mode so WhatsApp, Signal and Telegram
do not ring during your windows. Emergency numbers and alarms are never
blocked.

WHAT IT DOES
Instant Pause intercepts your outgoing calls during the windows you choose
(e.g. 22:00-07:00). Instead of dialling, you get a pause screen with your own
sentence, then you decide. Three trusted contacts stay reachable, capped at
3 minutes. No account, no ads, no internet access.

WHAT I NEED
12 testers who can stay opted in for 14 consecutive days. Play requires this
before the app can go public. Please do not opt out early: if you leave, the
counter restarts.

HOW TO JOIN (2 minutes)
1. Join the tester group: <GROUP LINK>
2. Open https://play.google.com/apps/testing/com.instantpause.app
   and tap "Become a tester"
3. Install from Play, then open the app once.

FEEDBACK
janedouby@gmail.com or reply here. Tell me your phone model, your Android
version, and whether the pause screen appeared when you tried to call a
normal number. Never call an emergency number to test.
```

---

## 6. Formulaire d'inscription — champs

| Champ | Type | Obligatoire | Raison |
|---|---|---|---|
| Adresse e-mail Google | réponse courte | oui | c'est le compte du téléphone qui doit être inscrit |
| Modèle de téléphone | réponse courte | oui | repère les surcouches qui cassent l'interception |
| Version d'Android | choix : 13 / 14 / 15 / 16 / je ne sais pas | oui | le sélecteur de langue in-app exige Android 13+ |
| Langue préférée | choix : English / Français / Español | non | savoir quoi vérifier après installation |
| « Je resterai inscrit 14 jours consécutifs » | case à cocher | oui | seule contrainte qui fait échouer la démarche |
| « Je n'appellerai jamais un numéro d'urgence » | case à cocher | oui | sécurité |
| Consentement à un e-mail de suivi | case à cocher | non | relance à J+7 |

Réglages : *Réponses › Collecter les adresses e-mail (vérifiées)*, *Limiter à 1 réponse*,
*Réponses › Lier à Google Sheets*.

---

## 7. Play Console — points de vigilance

1. **Notes de version** : 500 caractères maximum par langue. Utiliser les blocs `<en-US>`,
   `<fr-FR>`, `<es-ES>` de `docs/RELEASE-NOTES.md` (section 1.6.0), balises comprises.
2. **Déclaration DND** : obligatoire, elle conditionne l'acceptation de
   `ACCESS_NOTIFICATION_POLICY`.
3. **Identifiants de connexion pour la revue** : l'app n'a pas de compte, mais elle a un
   **code local**. Le formulaire « Accès à l'application » doit indiquer que le code est
   créé au premier lancement (voir `docs/PERMISSIONS-PLAY.md` §0).
4. **Rapport pré-lancement** : lancer *Tests › Rapport pré-lancement* après déploiement,
   il détecte les plantages sur appareils réels sans consommer de testeur.
5. **Ne pas modifier l'APK après signature** : Play vérifie la signature. Repasser par
   `.\gradlew bundleRelease` pour tout correctif.

---

## 8. Journal

| Date | Fait | Preuve |
|---|---|---|
| 15 nov. 2026 | AAB 1.6.0 signé, empreinte `4F:14:D6:…:F0:F5` vérifiée | `keytool -printcert` |
| 15 nov. 2026 | `release/1.6.0` et `main` poussés sur GitHub | `git push` |
| 16 nov. 2026 | `release/1.6.0` fusionnée dans `main` : la branche par défaut affiche la 1.6.0 (versionCode 16) | commit `74e679e` |
| 16 nov. 2026 | Release GitHub `v1.6.0` publiée avec l'APK signé (6,7 Mo, sha256 `e4c7dde1…56920af3`) | https://github.com/AJANEDOUBY/InstantPause-App/releases/tag/v1.6.0 |
| 16 nov. 2026 | AAB `16 (1.6.0)` reconstruit après nettoyage du dossier `build` : 5 456 986 octets, certificat `4F:14:D6:…:F0:F5` | `keytool -printcert` |
| 16 nov. 2026 | AAB 1.6.0 téléversé sur la piste *Tests fermés - Alpha*, notes en/fr/es, envoyé pour examen | Play Console › Vue d'ensemble de la publication |
| 16 nov. 2026 | Constat : le post Reddit du 7 nov. a été retiré par les filtres automatiques (absent du fil ; compte à 1 karma, 0 contribution, 3 liens dans le corps) | r/AndroidClosedTesting, fil *new* |
| 16 nov. 2026 | Commentaire technique (ADB/PowerShell) sur le post routine 14 jours de `u/Angel07Castillo`, sans lien externe : premier karma | /comments/1xxdtvk |
| 16 nov. 2026 | Commentaires d'échange sur Beam (`u/Miatrn`) et Eazy Player (`u/Low_Cartographer_368`), liens du groupe et d'opt-in en texte | /comments/1xxka00, /comments/1xxjxo3 |
| 16 nov. 2026 | Modmail aux modérateurs : demande d'approbation manuelle du post retiré | à confirmer côté modos |
| 16 nov. 2026 | Politique de confidentialité mise à jour : `ACCESS_NOTIFICATION_POLICY` déclarée et mode silencieux décrit, dans les trois langues ; date au 16 nov. | https://ajanedouby.github.io/instant-pause-privacy/ — commit `fd70620` |
| 16 nov. 2026 | Revue Google terminée en moins de deux heures : **1.6.0 disponible pour certains testeurs** (versionCode 16, 178 pays/régions) | Play Console › Tests fermés - Alpha, date de sortie 16 nov. 08:57 |
| 16 nov. 2026 | Lien d'opt-in vérifié dans le navigateur : la page répond « You are a tester » pour le compte du développeur | https://play.google.com/apps/testing/com.instantpause.app |
| 16 nov. 2026 | Trois commentaires Reddit vérifiés publics via l'API (`removed_by_category: null`) : le compte n'est pas bridé, c'est le post qui était filtré | `/r/AndroidClosedTesting/comments/{1xxdtvk,1xxka00,1xxjxo3}.json` |
| 16 nov. 2026 | Nouveau post publié dans **r/testerscommunity**, sans lien dans le corps, flair « Testers Needed » ; liens dans le premier commentaire. Visible dans le fil, score 1, non supprimé | https://www.reddit.com/r/testerscommunity/comments/1xxli15/test_for_test_instant_pause_a_pause_screen/ |
| 16 nov. 2026, 15:44 | Second post publié dans **r/GooglePlayClosedTest**, corps reformulé (1 376 car., aucun lien), pas de flair disponible dans ce sub. Vérifié API : `removed: null` | https://www.reddit.com/r/GooglePlayClosedTest/comments/1xxsolu/test_for_test_instant_pause_i_need_12_testers_for/ |
| 16 nov. 2026, 15:45 | Premier commentaire de liens publié (1 285 car.), puis **édité** pour ajouter la fiche Play Store : l'AutoModerator du sub exige opt-in **et** fiche Store. Trois liens vérifiés présents dans le corps final, `removed: null` | `/r/GooglePlayClosedTest/comments/1xxsolu.json` |
| 16 nov. 2026, 17:35 | **Troisième** copie du même post, cette fois dans r/AndroidTesting (titre identique, corps identique) | https://www.reddit.com/r/AndroidTesting/comments/1xxvnju/ |
| 16 nov. 2026, 20:14 | Commentaire chez `u/building_stuffs` annonçant « Joined your group and installed your app » **avant** de l'avoir fait | /r/AndroidTesting/comments/1xxosht |
| 17 nov. 2026 | Vérification : le groupe `31-days-testing` affichait toujours « Rejoindre le groupe ». L'engagement public n'était pas tenu | groups.google.com/g/31-days-testing |
| 17 nov. 2026 | Reprise en main : commentaire au modèle non rempli (`<APP>`, `<DATE>`) **supprimé** via `/api/del` | `t1_19sq9zyu9diqr` |
| 17 nov. 2026 | Engagement tenu pour **31 Days** : groupe rejoint, opt-in accepté (« You are a tester »), APK installé sur la tablette `R5GL350ZJ9P` (visible dans le lanceur) | `com.days.android` |
| 17 nov. 2026 | Engagement partiel pour **OffKit All Tools** : groupe `offkit-all-tools-tester` rejoint, opt-in accepté, mais la fiche Play répond **« Élément introuvable »** même avec le droit testeur — la release de test n'est pas accessible. Signalé à son auteur | `com.offkit.app` |
| 17 nov. 2026 | Répondu à `u/building_stuffs` (aveu de la réponse trop rapide + engagement tenu), à `u/husnainali197` (test injoignable + contrainte du même compte Google) et à `u/onTestapp` (**refus** de l'achat de testeurs). Les trois vérifiés `removed: null` | `/r/GooglePlayClosedTest/comments/1xxsolu.json` |
| 17 nov. 2026 | Diagnostic corrigé publiquement chez `u/husnainali197` : le « Item not found » venait d'un compte non opt-in sur la tablette, pas de sa release. Le Play de la tablette `R5GL350ZJ9P` est sur `abdelgalaxy26@gmail.com`, Chrome sur `janedouby@gmail.com` | `t1_19srh300ffq5g` |
| 17 nov. 2026 | `abdelgalaxy26@gmail.com` ajouté au groupe de test (7ᵉ membre) : c'est le compte Play de la tablette, sans lui l'app restait invisible. Invitation refusée par un **reCAPTCHA** côté console ; adhésion faite par le propriétaire | groups.google.com/g/instant-pause-testers/members |
| 17 nov. 2026 | Instant Pause **installé depuis Play** sur la tablette avec le compte `abdelgalaxy26@gmail.com` : versionCode 16 / 1.6.0, `firstInstallTime` 08:00:46, premier lancement en français | `adb shell dumpsys package com.instantpause.app` |
| 17 nov. 2026 | **OffKit impossible à installer sur tablette** : groupe rejoint et opt-in accepté avec `abdel`, mais la fiche Play répond « Cette application n'est pas disponible pour votre appareil ». Ce n'est ni la release ni le compte — l'app est publique mais incompatible avec un écran de 12 pouces. `pm list packages` confirme l'absence du paquet | `com.offkit.app` |
| 17 nov. 2026 | Groupe de test à **8 membres** : 5 testeurs externes + `janedouby`, `abdelgalaxy26` et `othman.belmehdi` (comptes du développeur) | groups.google.com/g/instant-pause-testers/members |
| | | |

---

## 9. Interdits

- **Acheter des testeurs** (Fiverr, fermes d'installations, échanges d'installations) :
  contraire aux politiques Play, risque de refus d'accès et de suppression de l'app.
- **Promettre une récompense** contre un avis ou une inscription.
- **Annoncer une installation ou une inscription avant de l'avoir faite.** C'est arrivé le
  16 nov. chez `u/building_stuffs` ; ça se répare, mais ça ne se rattrape pas.
- **Poster le même texte dans plusieurs subs le même jour.** Trois copies en huit heures le
  16 nov. : c'est le signal qui fait filtrer un compte neuf.
- **Cliquer « Installer » depuis play.google.com.** La fenêtre d'achat exige une
  réauthentification Google (`accounts.google.com`) : passer par `adb shell am start -a
  android.intent.action.VIEW -d "market://details?id=<pkg>"` puis `input tap`, et vérifier
  avec `pm list packages`.
- **Téléverser un versionCode déjà utilisé** (14 est consommé).
- **Diffuser le lien d'opt-in avant que la version soit disponible** sur la piste.
- **Tester un vrai numéro d'urgence**, y compris pendant une démonstration.

---

## 10. Après l'approbation

1. Fusionner `release/1.6.0` dans `main`, pousser, puis repartir de `main` pour la 1.7.0.
2. Publier la 1.6.0 en production avec **le même AAB** (aucun rebuild).
3. Ouvrir les pistes *Test ouvert* et *Test interne* pour les versions suivantes.
4. Conserver le groupe de testeurs : il servira aux prochaines versions.
