# Analyse concurrentielle — Play Store

Relevé effectué le 4 novembre 2026 sur les fiches Google Play publiques (US, anglais).
Chiffres tels qu'affichés par Google, non vérifiables autrement.

---

## 1. Le paysage réel

Le marché du « call blocker » est massivement un marché de **spam entrant**. Sept applications
sur huit dans cette liste ne bloquent que les appels reçus. Le blocage **sortant** est soit
absent, soit un accessoire de contrôle parental.

| Application | Éditeur | Installations | Note | Modèle | Entrant | Sortant |
|---|---|---|---|---|---|---|
| Calls Blacklist - Call Blocker | Enchan Li | 10 M+ | 4,5 (858 K) | pubs + IAP, Play Pass | ✅ | ❌ |
| Call Blocker - Phone - ID | Applika GmbH | 1 M+ | 4,1 (46 K) | pubs + IAP | ✅ | ✅ (PIN) |
| Call Blocker - Spam & Outgoing | Applab Studios | 100 K+ | 3,5 (662) | pubs + IAP | ✅ | ✅ (PIN) |
| Call Blocker | Backtracking Tech. | 100 K+ | 3,6 (567) | gratuit | ✅ | ❌ |
| **NoDial: Block Outgoing Calls** | XTV LLC | **1 K+** | — | **abonnement** | ❌ | ✅ **cœur** |
| CallBlocker (open source) | StefanIlchev | — | — | Apache 2.0 | ✅ | ❌ |

Deux constats qui changent la stratégie :

**Le segment « sortant » est un désert.** NoDial est le seul concurrent dont c'est le produit,
et il plafonne à 1 000 installations. Ce n'est pas un marché saturé : c'est un marché **non
encore créé**, faute de demande formulée — pas faute d'outil.

**Les 10 M+ de Calls Blacklist sont un marché différent du vôtre.** Leur utilisateur veut qu'on
ne le dérange pas. Le vôtre veut qu'on l'empêche de déranger quelqu'un. Ce ne sont pas les mêmes
mots dans la barre de recherche, ni la même promesse.

---

## 2. NoDial — le seul vrai concurrent

C'est la référence à étudier, et la seule fiche dont le discours recoupe le vôtre.

### Ce qu'il fait

- Blocage des appels **sortants** via `CallRedirectionService` — exactement votre mécanique.
- Blocage d'un **numéro à la fois**, avec la possibilité de bloquer « autour du clock » ou
  seulement certains jours et heures.
- Alternative intéressante : **quota d'appels** par jour, semaine ou mois, puis blocage du reste.
- Blocage des appels internationaux, avec pays autorisés (offre payante).
- **Aucun bouton de suppression** pour un numéro bloqué. Désactivation possible, mais elle
  **prend effet une heure après confirmation**, et reste annulable pendant cette heure.
  Réactivation immédiate.
- Urgences toujours autorisées, jamais ajoutables à la liste.
- Bandeau « Protected » en haut de l'écran dès qu'une règle existe, avec une note si certains
  numéros restent joignables.

### Ce qu'il coûte

Premier numéro gratuit. Ensuite abonnement mensuel : Premium (10 numéros), Premium Plus
(20 numéros), Premium Unlimited (tous les appels sortants). Paiement via Google Play.

### Sa faiblesse, et c'est la vôtre

NoDial **exige un compte anonyme, une connexion Internet et un serveur**. Les numéros bloqués
sont hachés sur l'appareil avant l'envoi, ce qui est honnête — mais il y a un serveur.

Votre application n'a **pas la permission `INTERNET`**. C'est vérifiable dans le manifeste par
n'importe qui, y compris un examinateur Google. C'est l'argument le plus fort de la fiche, et
aucun concurrent ne peut le copier sans réécrire son architecture.

### Ce qu'il faut lui prendre

1. **Le libellé « Protected » plutôt que « Protection active »** : il décrit un état de
   configuration, pas un état de plage horaire. Voir la section 4.
2. **Le délai avant affaiblissement** : chez lui, couper une règle prend une heure. Chez vous,
   le code suffit et agit instantanément. Un code connu de l'utilisateur n'arrête pas une
   impulsion de 23 h — un délai, si.
3. **Le quota d'appels** (N par jour/semaine). Excellent pour un usage « je veux appeler, mais
   pas dix fois ».

---

## 3. Les quatre autres — ce qu'ils nous apprennent

### Applab Studios — Call Blocker - Spam & Outgoing (100 K+, 3,5)

La fiche la plus proche fonctionnellement : entrants + sortants, PIN, plannings, règles par
préfixe. Le discours est un catalogue de fonctionnalités, sans récit.

**Leçon négative.** La fiche proclame « 100 % hors ligne, aucune donnée collectée, aucun compte ».
Mais la déclaration **Sécurité des données** du même écran indique le partage de la **localisation**
et de l'activité de l'app. Un journaliste ou un examinateur le voit en deux clics. Une promesse de
confidentialité doit être **vérifiable**, pas déclarative.

### Enchan Li — Calls Blacklist (10 M+, 4,5)

Le poids lourd. Profils de blocage (travail, sommeil, perso), listes blanches/noires avec préfixes,
import/export CSV, sauvegarde quotidienne, mot de passe. Entrant uniquement.

**Leçon.** Ce qui fait 10 M d'installations, ce n'est pas une fonctionnalité, c'est **un modèle
mental** : des « profils » qu'on bascule. Vous avez l'équivalent avec les plannings + le mode Zen,
mais ce n'est pas nommé comme un profil.

### Applika — Call Blocker - Phone - ID (1 M+, 4,1)

Entrants + sortants protégés par code, plannings, jokers, et **composeur intégré**. Le composeur
est là pour revendiquer le rôle de dialer par défaut — et donc l'éligibilité au pré-accord
`USE_FULL_SCREEN_INTENT`.

**Leçon.** C'est exactement ce que vous avez choisi de ne pas faire. Votre dossier de permissions
est plus fragile que le leur. À assumer : vous n'êtes pas un composeur.

### Backtracking — Call Blocker (100 K+, 3,6, dernière MAJ nov. 2021)

Entrant, liste blanche, aucune donnée collectée. Simple, mort depuis cinq ans, toujours installé
100 000 fois.

**Leçon.** Dans cette catégorie, une app abandonnée garde ses utilisateurs. La barre de qualité
n'est pas haute. Une fiche propre et un produit qui marche suffisent à exister.

### CallBlocker open source (StefanIlchev, Kotlin, Apache 2.0, 20 ★)

Blocage entrant par expression régulière. Utile comme lecture de code, pas comme concurrent.

---

## 4. Diagnostic du bandeau « Protection inactive »

Ce n'est **pas un bug**. À 11 h 36, une plage 22:30–07:30 est correctement considérée comme
inactive. La logique de `Schedule.isActiveAt()` gère le passage de minuit et les jours.

Mais deux défauts réels :

**a) Le bandeau confond « hors plage » et « problème ».** Il s'affiche en orange avec une icône
d'avertissement alors que tout est configuré. L'utilisateur croit à une panne. C'est exactement
l'erreur que NoDial évite en affichant « Protected » dès qu'une règle existe.

Trois états seraient corrects :

| Situation | Affichage | Couleur |
|---|---|---|
| Une plage couvre l'instant présent | « Protection active jusqu'à 07:30 » | vert |
| Aucune plage maintenant, mais au moins une plage active | « Hors plage — reprise à 22:30 » | neutre/gris |
| Aucune plage active | « Protection inactive » | orange |

**b) Le statut ne se rafraîchit pas.** `isCurrentlyProtected` est calculé avec
`ZonedDateTime.now()` au moment de la composition, sans horloge qui la relance. Si l'écran reste
ouvert à travers 22:30 ou minuit, le bandeau reste faux jusqu'à la prochaine recomposition.

**Ne corrigez pas maintenant.** La 14 est déjà importée et en attente d'examen. Toute correction
impose un `versionCode` 15, un nouvel AAB, et relance l'examen des permissions. À embarquer dans
la 1.5.0, après le test fermé.

---

## 5. Positionnement qui en découle

Aucun concurrent ne vend **l'impulsion**. Tous vendent du filtrage. Le créneau libre est
émotionnel, pas fonctionnel :

> Ce n'est pas une app qui bloque des appels. C'est une app qui vous empêche d'appeler à 23 h.

Trois conséquences concrètes pour la fiche :

1. **Ne pas se battre sur « call blocker ».** La requête est tenue par des acteurs à 10 M
   d'installations et un utilisateur qui cherche du spam entrant. Vos mots sont : appels
   impulsifs, arrêter d'appeler, rechute, no contact, sobriété.
2. **Mettre « aucune permission Internet » dans les cinq premières lignes** de la description.
   C'est vérifiable et aucun concurrent ne peut l'affirmer.
3. **Catégorie Style de vie, pas Communication.** Se ranger avec les bloqueurs, c'est perdre
   d'avance. Se ranger avec les apps d'habitude, c'est être seul.

---

## 6. À reprendre en 1.5.0, par ordre de valeur

| Priorité | Élément | Pourquoi |
|---|---|---|
| 1 | Bandeau à trois états + horloge | corrige un malentendu vu en vrai |
| 2 | « Reprise à 22:30 » | répond à la question que se pose l'utilisateur |
| 3 | Délai d'une heure avant de couper une plage | emprunté à NoDial, cohérent avec la promesse |
| 4 | Quota d'appels par jour | différenciant, personne d'autre ne le fait sur le sortant |
| 5 | Widget ou tuile « pause 2 h » | le mode Zen mérite un accès en un geste |

Le point 1 est le seul qui vient d'un défaut constaté, pas d'une hypothèse.
