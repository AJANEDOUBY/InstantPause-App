# Fiche Play Store — textes à copier-coller

Chaque bloc ci-dessous se copie **tel quel** dans la Play Console. Les limites de caractères
sont indiquées et respectées.

> Repère : dans `docs/GUIDE-PLAY.md`, ces textes se trouvent à l'**étape 3** (lignes 69 à 126).

---

## Nom de l'application — 30 caractères max

```
Instant Pause
```

*13 caractères.*

---

## Description courte — 80 caractères max

```
Bloquez les appels impulsifs pendant vos plages : respirez, puis décidez.
```

*73 caractères.*

---

## Description complète — 4 000 caractères max

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

*1 996 caractères sur 4 000 — il reste de la place pour une section « Nouveautés » plus tard.*

---

## Champs simples

| Champ | Valeur |
|---|---|
| Adresse e-mail | `janedouby@gmail.com` |
| Site Web | `https://ajanedouby.github.io/instant-pause-privacy/` |
| Politique de confidentialité | `https://ajanedouby.github.io/instant-pause-privacy/` |
| Catégorie | Style de vie |
| Type | Application · Gratuite |
| Langue par défaut | Français (France) |

### Étiquettes (5 maximum)

À saisir dans **Développer → Présence sur le Play Store → Fiche principale → Étiquettes**
(ou *Paramètres du Store → Étiquettes* selon la version de la Console).

| # | Étiquette | Pourquoi celle-là |
|---|---|---|
| 1 | **Santé et remise en forme** | Le cœur du produit : reprendre la main sur un comportement compulsif |
| 2 | **Style de vie** | La catégorie principale, cohérente avec la fiche |
| 3 | **Productivité** | L'angle « je protège mon temps et mon sommeil » |
| 4 | **Outils** | C'est un utilitaire qui agit sur le téléphone, pas un contenu |
| 5 | **Communication** | L'app intervient sur les appels : c'est factuellement vrai |

Remplacements possibles si une étiquette n'existe pas dans la liste proposée :
**Bien-être**, **Personnalisation**, **Outils de téléphone**, **Minuteries**.

À éviter absolument : *Médical* (l'app n'est pas un dispositif médical et la déclaration Santé
deviendrait obligatoire), *Social*, *Dating*, *Parentalité* (aucune fonction pour enfants).


---

## Justifications de permissions sensibles

À coller dans le formulaire de déclaration, au moment de l'import du bundle.

### Raccrochage à 3 minutes — `ANSWER_PHONE_CALLS`

```
Instant Pause laisse l'utilisateur joindre trois contacts de confiance pendant ses plages de
protection, mais limite volontairement ces appels à 3 minutes : c'est la règle que l'utilisateur
a lui-même fixée pour éviter de rester une heure au téléphone avec la personne qui déclenche son
impulsivité. ANSWER_PHONE_CALLS sert uniquement à raccrocher ce type d'appel, arrivé à 3:00, via
TelecomManager.endCall(). L'application ne lit ni le journal d'appels, ni les SMS, ne compose
aucun numéro et ne connaît pas l'identité de l'appelant au-delà du numéro composé.
```

### Administrateur d'appareil

```
L'application empêche une désinstallation impulsive : c'est le cœur de sa protection, au même
titre que le code. L'administrateur d'appareil n'est utilisé que pour cette fonction. Aucune
politique de sécurité n'est imposée à l'appareil (pas de mot de passe obligatoire, pas de
chiffrement), aucun effacement à distance n'est possible, et l'utilisateur désactive lui-même
l'administrateur depuis les réglages Android avec son code.
```

### Service de premier plan — `specialUse`

```
Maintien du minuteur d'appel (qui doit s'exécuter pendant que l'écran d'appel est au premier plan)
et de la protection qui survit au redémarrage de l'appareil. Le service n'accède à aucune donnée
et n'effectue aucun traitement en arrière-plan hors de ces deux usages déclenchés par une action
de l'utilisateur.
```

### Écran plein écran — `USE_FULL_SCREEN_INTENT`

```
Application de blocage d'appels : l'écran de pause doit s'afficher immédiatement par-dessus
l'écran d'appel et l'écran verrouillé, au moment où l'utilisateur compose un numéro pendant une
plage de protection. Sans cet affichage immédiat, l'appel partirait avant que la pause ne
s'affiche, et la protection n'aurait aucun effet.
```

---

## Réponses au formulaire « Sécurité des données »

| Question | Réponse |
|---|---|
| L'application collecte-t-elle des données ? | **Non** |
| L'application partage-t-elle des données ? | **Non** |
| Données chiffrées en transit ? | sans objet |
| L'utilisateur peut-il demander la suppression ? | oui, en désinstallant ou via les réglages Android |
