## Changement du nom de paquet : `com.instantpause.app`

Google Play a refusé l'identifiant `com.cooldown.app` — ainsi que `com.cooldown` — comme
déjà enregistrés. L'application change donc de paquet :

| | Avant | Après |
|---|---|---|
| Paquet | `com.cooldown.app` | **`com.instantpause.app`** |
| Paquet debug | `com.cooldown.app.debug` | `com.instantpause.app.debug` |
| Nom affiché | Instant Pause | Instant Pause (inchangé) |

Les 63 fichiers source ont été déplacés (`java/com/cooldown/app` → `java/com/instantpause/app`)
et leurs paquets renommés. Aucun identifiant de composant n'est absent : manifeste, service de
redirection d'appels, administrateur d'appareil, activité de pause, récepteurs de démarrage et
service de veille suivent automatiquement, car ils sont déclarés relativement au `namespace`.

## Ce que cela implique, sans détour

**Android considère qu'il s'agit d'une autre application.** Il n'existe aucun chemin de mise à
jour entre `com.cooldown.app` et `com.instantpause.app` :

- les données (code, plages, contacts autorisés) **ne suivent pas** — il faut refaire
  l'installation guidée ;
- l'ancienne version doit être désinstallée **manuellement**, en désactivant d'abord son
  administrateur d'appareil, sinon les deux apps cohabitent ;
- les personnes qui avaient l'ancienne version via GitHub ne recevront pas de mise à jour
  automatique.

C'est une contrainte d'Android, pas un choix. Rien ne permet de migrer un paquet.

## Pour Google Play

La version 1.4.0 est prête à être déposée : cible SDK 37, `minSdk 29`, signature de release
inchangée. Deux formats possibles :

- **`.aab`** (recommandé, obligatoire pour une nouvelle app) — généré par
  `gradlew :app:bundleRelease` ;
- **`.apk`** pour les installations directes depuis GitHub.

Le formulaire Play demandera aussi, au moment de la publication, la justification des
permissions sensibles : service de premier plan `specialUse` (raison : blocage d'appels
impulsifs), `USE_FULL_SCREEN_INTENT`, `ANSWER_PHONE_CALLS` et l'administrateur d'appareil
(protection anti-désinstallation).
