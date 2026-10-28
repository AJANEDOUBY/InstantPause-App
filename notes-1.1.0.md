## Le problème

Les appels impulsifs ne sont pas seulement une question d'heure : quand la protection gêne,
la tentation de la couper — ou de désinstaller l'app — devient très forte. Un réglage qu'on
peut désactiver en trois taps ne protège personne le soir où ça compte.

## Ajouts

### 🔒 Verrou par code alphanumérique

Un code (6 caractères minimum, au moins une lettre et un chiffre) est désormais demandé
**uniquement pour les actions qui affaiblissent la protection** :

- couper une plage active ;
- supprimer une plage (y compris une plage Zen en cours) ;
- changer ou supprimer le code ;
- retirer la protection anti-désinstallation.

Tout le reste reste libre : bloquer un appel, ajouter une plage, activer le Zen, consulter
l'historique.

- **PBKDF2-HMAC-SHA256**, sel de 16 octets, 120 000 itérations, 256 bits ;
  le code n'existe jamais en clair sur le disque.
- Comparaison à temps constant ; compteur d'échecs persisté.
- **Temporisation progressive** : 3 essais libres, puis 30 s, 1 min, 2 min… jusqu'à 15 min.
  Tuer l'app ou redémarrer le téléphone ne remet pas le compteur à zéro.
- L'état « déverrouillé » vit **en mémoire** et retombe après 60 s en arrière-plan.

### 🛡️ Friction anti-désinstallation

Activation facultative d'un **administrateur d'appareil sans aucune politique** : Android
refuse alors la désinstallation tant qu'il est actif. Ce n'est pas un cadenas — deux taps
dans les Paramètres suffisent — mais c'est deux taps qui laissent le temps de réfléchir.

### 🧭 Nouvel onglet « Sécurité »

État du verrou, création / changement / suppression du code, activation de l'administrateur,
et une explication honnête de ce que le code protège… et ne protège pas.

## Correctifs

- Le glyphe `→` s'affichait `ñ` sur certaines ROM (police système sans la flèche) :
  les plages utilisent désormais un tiret demi-cadratin.

## Limites (assumées)

- Le verrou ne s'oppose ni à `adb`, ni à un effacement des données, ni au mode sans échec,
  ni à la désactivation de l'administrateur depuis les Paramètres système.
- **Code oublié = aucune récupération** : il faut effacer les données de l'app (les plages
  sont perdues) ou désinstaller après avoir retiré l'administrateur.

## Vérifications

- **59 tests unitaires** verts (37 nouveaux : politique de code, PBKDF2, temporisations,
  cycle de vie du verrou, défaillances du stockage).
- Testé sur itel A70 (Android 13) : création du code, mauvais code, verrouillage manuel,
  reverrouillage automatique après 60 s, plage bloquée puis débloquée, suppression gatée,
  administrateur actif, état verrouillé après mort du processus.
- Aucun crash R8 ; interception d'appel inchangée.
