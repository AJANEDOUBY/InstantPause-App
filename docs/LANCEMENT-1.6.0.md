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
- [ ] **Étape 3 — Attendre la disponibilité.** La revue prend de quelques heures à 2 jours.
      Vérifier que la 1.6.0 apparaît comme version active du test fermé.
  - Date de disponibilité : ____
- [x] **Étape 4 — Groupe Google des testeurs.** Le groupe
      `instant-pause-testers@googlegroups.com` est déjà déclaré dans la piste *Testeurs*
      (avec un message de 99 caractères pour les commentaires). Reste à y ajouter les
      adresses des volontaires.
  - Date : déjà en place le 16 nov. 2026
- [ ] **Étape 5 — Formulaire d'inscription.** Champs de la section 6, réponses liées à un Google
      Sheets, colonne « date d'inscription » remplie à la main.
  - Date : ____
- [ ] **Étape 6 — Éditer le post Reddit.** Texte de la section 5, avec le lien du groupe et le
      lien d'opt-in. Répondre à chaque commentaire.
  - Date : ____
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
| | | |

---

## 9. Interdits

- **Acheter des testeurs** (Fiverr, fermes d'installations, échanges d'installations) :
  contraire aux politiques Play, risque de refus d'accès et de suppression de l'app.
- **Promettre une récompense** contre un avis ou une inscription.
- **Téléverser un versionCode déjà utilisé** (14 est consommé).
- **Diffuser le lien d'opt-in avant que la version soit disponible** sur la piste.
- **Tester un vrai numéro d'urgence**, y compris pendant une démonstration.

---

## 10. Après l'approbation

1. Fusionner `release/1.6.0` dans `main`, pousser, puis repartir de `main` pour la 1.7.0.
2. Publier la 1.6.0 en production avec **le même AAB** (aucun rebuild).
3. Ouvrir les pistes *Test ouvert* et *Test interne* pour les versions suivantes.
4. Conserver le groupe de testeurs : il servira aux prochaines versions.
