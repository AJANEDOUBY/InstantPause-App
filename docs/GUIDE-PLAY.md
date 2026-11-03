# Publier Instant Pause sur Google Play — pas à pas

Guide écrit pour être suivi sans rien connaître à la Play Console. Chaque étape dit **où
cliquer**, **ce qu'on écrit** et **combien de temps ça prend**.

> Version concernée : **1.4.0** (`com.instantpause.app`), bundle `InstantPause-1.4.0.aab`.
> Compte : personnel, créé après le 13 novembre 2023 — les règles « 12 testeurs » s'appliquent.

---

## Étape 0 — Sortir les affaires sur la table

Avant de toucher à la Play Console, réunir ces fichiers. Sans eux, on s'arrête en route.

| Élément | Où | État |
|---|---|---|
| Le bundle | `D:\AndroidStudioProjects.V2\InstantPause-1.4.0.aab` (5,09 Mo) | ✅ prêt |
| URL de confidentialité | `https://ajanedouby.github.io/instant-pause-privacy/` | ✅ en ligne |
| Icône 512 × 512 px, PNG | à produire depuis `mipmap-anydpi/ic_launcher.xml` | ❌ manquant |
| Visuel « bannière » 1024 × 500 px | à produire | ❌ manquant |
| 2 à 8 captures d'écran du téléphone | `adb shell screencap` | ❌ manquant |
| Un compte Google + carte bancaire | 25 $ **une seule fois**, pas un abonnement | — |
| Un téléphone Android 10 ou plus | pour la validation d'appareil | la tablette suffit |

**Ce que Google ne demande pas** : pas de dépôt de code, pas de justificatif de société, pas de
diplôme. Un particulier peut publier.

---

## Étape 1 — Créer le compte développeur (30 min + 1 à 3 jours d'attente)

1. Aller sur **https://play.google.com/console** et se connecter avec `janedouby@gmail.com`.
2. Choisir **« Personnel »** comme type de compte (pas « Organisation » : ça exige un numéro
   d'entreprise).
3. Remplir : nom de développeur **JANEDOUBY**, adresse, téléphone.
4. Payer les **25 $** par carte.
5. **Vérifier son identité** : Google demande une pièce d'identité (photo recto/verso) et parfois
   un justificatif de domicile. C'est la partie la plus lente : compter 1 à 3 jours ouvrés.
6. **Valider l'appareil** — obligatoire pour tout nouveau compte personnel :
   - dans la Play Console, page **Accueil**, repérer la tâche
     « Confirmer que vous avez accès à un appareil mobile Android » ;
   - cliquer **Afficher les détails**, scanner le QR code avec un téléphone Android 10+ ;
   - installer **l'application Play Console** (pas le Play Store classique), se connecter avec le
     même compte, choisir le compte développeur, appuyer sur **Valider**. Moins d'une minute.

> Sans cette validation, la production reste bloquée. Un téléphone non rooté, c'est tout.

---

## Étape 2 — Créer l'application (5 min)

1. **Toutes les applications** → **Créer une application**.
2. Nom de l'app : **Instant Pause**.
3. Langue par défaut : **Français (France)**.
4. Type : **Application**. Gratuite.
5. Cocher les deux déclarations (règles du programme, lois sur l'exportation).
6. **Créer l'application**.

À partir d'ici, la Console affiche une liste de tâches. On les traite dans l'ordre de ce guide.

---

## Étape 3 — Remplir la fiche Play Store (1 h)

Menu **Développer → Présence sur le Play Store → Fiche principale**.

### Textes prêts à copier

**Nom de l'application** (30 caractères max) :

```
Instant Pause
```

**Description courte** (80 caractères max) :

```
Bloquez les appels impulsifs pendant vos plages : respirez, puis décidez.
```

**Description complète** (4 000 caractères max) :

```
Il est 23 h. Vous savez très bien que vous ne devriez pas appeler. Vous appelez quand même.

Instant Pause existe pour cette minute-là.

Pendant les plages horaires que vous définissez, chaque appel sortant est intercepté et remplacé
par un écran de pause : respirer, se calmer, puis décider. Pas un mur. Pas une punition. Un
espace de quelques secondes entre l'impulsion et l'acte.

CE QUE FAIT L'APPLICATION
• Vous choisissez des plages horaires (par exemple 22 h – 7 h, tous les jours).
• Tout appel sortant pendant une plage ouvre l'écran de pause au lieu de composer.
• Un mode Zen peut bloquer les appels à la demande, pour 2 heures.
• Trois contacts de confiance peuvent toujours être joints : leur appel est coupé
  automatiquement à 3 minutes.

CE QUI LA REND DIFFÉRENTE
• Le code de protection est créé à froid, avant tout le reste, et il n'est jamais stocké en clair.
• Couper une plage, supprimer un contact autorisé ou retirer l'anti-désinstallation exige ce code.
• Aucun compte. Aucune publicité. Aucun analytics. Aucun serveur.
• L'application ne demande pas la permission de lire votre répertoire : vous désignez un contact
  avec le sélecteur d'Android, et seule cette fiche est enregistrée.

LES NUMÉROS D'URGENCE NE SONT JAMAIS BLOQUÉS

En cas d'erreur ou de doute, l'appel passe. Toujours. Mieux vaut laisser filer un appel que
bloquer une urgence.

VOS DONNÉES RESTENT CHEZ VOUS

Pas de connexion réseau, pas de sauvegarde dans le cloud. Plages, contacts et réglages vivent
dans le stockage privé de l'application, sur votre téléphone. La politique complète est ici :
https://ajanedouby.github.io/instant-pause-privacy/

CONFIGURATION EN 6 ÉTAPES

1. Choisissez votre code.
2. Activez la protection anti-désinstallation.
3. Autorisez le raccrochage à 3 minutes.
4. Acceptez le rôle de redirection d'appels.
5. Autorisez l'affichage par-dessus les appels.
6. Créez une plage horaire. C'est tout.

Une correction de code, un doute, un plantage : écrivez à janedouby@gmail.com.
```

### Images

| Zone | Format | Contenu conseillé |
|---|---|---|
| Icône | 512 × 512, PNG 32 bits, ≤ 1 Mo | le logo, fond plein |
| Bannière | 1024 × 500, PNG ou JPEG, sans transparence | logo + « Attendez demain avant d'appeler » |
| Téléphone | 2 minimum, 8 maximum, 16:9 ou 9:16 | écran d'accueil + écran de pause + onglet Contacts |

### Autres champs

- **Coordonnées** : e-mail `janedouby@gmail.com`, site `https://ajanedouby.github.io/instant-pause-privacy/`.
- **Politique de confidentialité** : l'URL ci-dessus.
- **Catégorie** : *Style de vie* (ou *Santé et remise en forme*, moins juste).
- **Étiquettes** : bien-être, productivité, téléphone.

---

## Étape 4 — Les formulaires obligatoires (45 min)

Menu **Politique → Contenu de l'application**. Chaque ligne doit finir en vert.

| Formulaire | Réponse pour Instant Pause |
|---|---|
| **Politique de confidentialité** | l'URL de l'étape 3 |
| **Accès à l'application** | « Toutes les fonctionnalités sont disponibles sans accès particulier » |
| **Publicités** | « Non, l'application ne contient pas d'annonces » |
| **Classification du contenu** | questionnaire : non violent, aucun contenu sensible. Résultat attendu : 3 ans et plus |
| **Public cible** | 18 ans et plus (ce n'est pas une app pour enfants) |
| **Sécurité des données** | **« Aucune donnée collectée »**, et « aucune donnée partagée » |
| **Fonctionnalités financières** | non |
| **Application gouvernementale** | non |
| **Actualités** | non |
| **Santé** | non (ce n'est pas un dispositif médical) |

> **Sécurité des données** est le formulaire le plus scruté : une déclaration fausse entraîne un
> retrait. Ici la réponse est simple et vraie — l'app n'a même pas la permission `INTERNET`.

---

## Étape 5 — Envoyer le bundle (30 min, puis quelques heures)

1. Menu **Tester et publier → Tests → Tests internes** → **Créer une release**.
   (Les tests internes sont facultatifs, mais ils évitent de griller une soumission en production.)
2. **Importer** `InstantPause-1.4.0.aab`.
3. La Console affiche alors les **déclarations de permissions sensibles**. C'est le moment
   délicat de tout le processus.

### Les quatre déclarations à préparer

| Sujet | Ce que Google demande | Ce qu'on répond |
|---|---|---|
| `ANSWER_PHONE_CALLS` | à quoi sert la permission | « Raccrocher à 3 minutes un appel vers un contact autorisé, pendant une plage de protection » |
| Administrateur d'appareil | justifier l'usage | « Empêcher une désinstallation impulsive. Aucune politique n'est imposée à l'appareil, aucun effacement à distance » |
| Service de premier plan `specialUse` | décrire l'usage | « Maintien du minuteur d'appel et de la protection après redémarrage » |
| `USE_FULL_SCREEN_INTENT` | catégorie d'app | « Application d'appels / de blocage d'appels : afficher l'écran de pause sur l'écran verrouillé » |

Google peut exiger une **vidéo de démonstration** (lien YouTube non répertorié, 1 à 2 minutes) :
montrer un appel intercepté, l'écran de pause, le raccrochage à 3:00. À filmer sur la tablette,
avec un numéro neutre — **jamais** un vrai numéro d'urgence.

4. Ajouter les testeurs (voir étape 6), puis **Enregistrer et publier**.

L'examen des permissions peut prendre **plusieurs semaines**. C'est le point de friction du
projet, pas le code.

---

## Étape 6 — Le test fermé : 12 personnes, 14 jours (le vrai délai)

C'est la règle qui surprend tout le monde, et elle est incontournable pour un compte personnel.

1. **Tester et publier → Tests → Tests fermés → Créer un canal.**
2. Créer une liste de testeurs avec **au moins 12 adresses e-mail** (amis, famille, collègues —
   comptes Google réels).
3. Publier la release sur ce canal. Google fournit un **lien d'inscription** à leur envoyer.
4. Chaque testeur doit : ouvrir le lien, accepter, installer l'app depuis Play, **puis la garder
   installée 14 jours d'affilée**. S'il se désinscrit, le compteur repart à zéro pour lui.
5. Attendre 14 jours pleins. En profiter pour corriger ce qu'ils signalent — Google posera des
   questions sur les retours reçus.

> Prévoir 3 à 4 semaines au total entre « je veux publier » et « c'est en ligne ».

---

## Étape 7 — Demander l'accès à la production (20 min + 1 semaine d'examen)

1. **Tableau de bord** → **Demander à publier en production**.
2. Répondre aux trois séries de questions :
   - *le test fermé* : combien de testeurs, ont-ils utilisé toutes les fonctionnalités, qu'ont-ils
     dit, qu'avez-vous corrigé ;
   - *l'application* : public visé (adultes qui veulent réduire leurs appels impulsifs),
     valeur apportée, estimation du nombre d'installations la première année ;
   - *la capacité à passer en production* : ce qui a changé grâce aux tests.
3. Envoyer. Réponse sous une semaine en général.

Une fois approuvé : **Production → Créer une release → Importer l'AAB → Publier**.

---

## Étape 8 — Après la publication

- Les mises à jour suivantes passent par **Production → Nouvelle release**, avec un
  `versionCode` supérieur (`15`, `16`…) : `./release.ps1` le fait, il suffit de changer
  `versionCode` et `versionName` dans `app/build.gradle.kts`.
- Garder le keystore `keystore/cooldown-release.jks` **en lieu sûr, hors du dépôt** : le perdre
  oblige à repasser par le support Google.
- Play App Signing conserve la clé de distribution : votre keystore devient la *clé d'upload*.
- Surveiller **Notes et avis** et **Politique → Statut de l'application** pendant les premières
  semaines.

---

## Récapitulatif des délais

| Étape | Durée |
|---|---|
| Compte + identité | 1 à 3 jours |
| Fiche + formulaires | une demi-journée |
| Examen des permissions | 1 à 4 semaines |
| Test fermé | **14 jours minimum**, 12 testeurs |
| Examen production | ~1 semaine |
| **Total réaliste** | **6 à 8 semaines** |
