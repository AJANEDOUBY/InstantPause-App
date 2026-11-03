# Security Policy

## Supported Versions

Seules les versions publiées depuis le renommage du paquet sont maintenues.

| Version | Supportée          | Notes |
| ------- | ------------------ | ----- |
| 1.4.x   | :white_check_mark: | Paquet `com.instantpause.app` |
| 1.3.x   | :x:                | Paquet `com.cooldown.app`, remplacé — aucune mise à jour possible vers 1.4.x |
| < 1.3   | :x:                | Obsolète |

Une faille corrigée n'est rétroportée que sur la dernière version mineure. Les versions
antérieures ne reçoivent aucun correctif : il faut installer la dernière release.

## Reporting a Vulnerability

**Ne créez pas d'issue publique.** Utilisez le formulaire privé de GitHub :

> **https://github.com/AJANEDOUBY/instant-pause-privacy/security/advisories/new**
>
> Onglet *Security* → *Report a vulnerability*

Le dépôt du code est privé et son plan GitHub ne permet pas les avis de sécurité : le
formulaire est donc ouvert sur le dépôt public `instant-pause-privacy`, seul point de contact
anonyme. Le rapport y reste **privé** et visible de l'auteur uniquement jusqu'à publication.

Si vous préférez l'e-mail : **janedouby@gmail.com**. Les rapports y sont traités avec la même
confidentialité, mais GitHub reste préférable pour le suivi et le crédit.

### Délais

| Étape | Délai visé |
| --- | --- |
| Accusé de réception | 72 h |
| Évaluation et gravité (CVSS) | 7 jours |
| Correctif, ou décision de ne pas corriger | 30 jours pour une faille critique, 90 jours sinon |

Vous serez informé à chaque étape, y compris si le rapport est refusé, avec la raison.

### Ce qu'il faut inclure

- version de l'application et d'Android, modèle d'appareil ;
- étapes de reproduction, précises et minimales ;
- comportement attendu / obtenu ;
- un enregistrement ou une capture si possible — **jamais** de données personnelles réelles
  (numéros, noms, captures d'écran de votre répertoire).

### Divulgation coordonnée

Nous demandons un délai de **90 jours** avant toute publication, ou moins si un correctif est
publié plus tôt. Le rapport est crédité dans les notes de version, sauf demande d'anonymat.

Aucune récompense financière n'est offerte : le projet est bénévole et sans revenus.

## Périmètre

L'ennemi visé par cette application n'est pas un attaquant distant, c'est **vous, dans un
mauvais jour**. Le modèle de sécurité est celui d'un garde-fou, pas d'un coffre-fort. Les
vulnérabilités qui nous intéressent sont donc les contournements **sans le code**.

### Dans le périmètre

| Sujet | Exemple de faille valable |
| --- | --- |
| Contournement du code | Couper une plage, supprimer un contact autorisé ou retirer l'administrateur d'appareil sans saisir le code |
| Faille du hachage | Empreinte réversible, sel prévisible, comparaison non constante, code stocké en clair quelque part |
| Temporisation | Contournement du verrouillage après plusieurs essais (redémarrage, changement d'heure, réinstallation partielle) |
| Plage horaire | Appel bloqué hors plage, ou appel non bloqué pendant une plage active (hors fail-open, voir plus bas) |
| Numéros d'urgence | Un numéro d'urgence bloqué, ou un appel de test routé vers un vrai service d'urgence |
| Contacts autorisés | Dépassement de la limite de 3, ou accès au répertoire sans permission |
| Fuite de données | Écriture de données de l'app hors de son stockage privé, sauvegarde cloud non déclarée, requête réseau |
| Élévation de privilèges | Un composant exporté permettant à une autre app de désactiver la protection |
| Secret exposé | Clé, mot de passe ou jeton versionné dans ce dépôt |

### Hors périmètre

- **Un appareil rooté, ou un débogage USB autorisé** : le bac à sable Android ne protège plus rien,
  ce n'est pas une faille de l'application.
- **Quelqu'un qui connaît votre code** : il est fait pour ça.
- **Un accès physique à l'appareil déverrouillé** : sortir de l'app, vider les données depuis les
  réglages Android ou révoquer une permission est un choix de l'utilisateur, pas un contournement.
- **Le fail-open** : en cas d'erreur ou de délai dépassé, l'appel passe. C'est **volontaire** —
  mieux vaut laisser passer un appel que bloquer une urgence. Un rapport qui demande un fail-closed
  sera refusé.
- **Le blocage des appels WhatsApp / VoIP** : non couvert à ce jour, ce n'est pas une régression.
- Vulnérabilités des dépendances sans impact démontrable sur l'application.
- Absence de signature, de certificat ou de durcissement anti-reverse sur un APK de debug.
