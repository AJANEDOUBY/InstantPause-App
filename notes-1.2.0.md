## Le problème

En 1.1.0, le verrou était une **option**. Sur une installation neuve, on pouvait activer la
protection puis la couper sans jamais définir de code : il suffisait de ne pas aller dans
l'onglet Sécurité. Le garde-fou n'existait que pour ceux qui pensaient à l'activer — c'est-à-dire
pas ceux à qui il sert.

## Correctif

### Le code est obligatoire dès la première ouverture

Au premier lancement, l'app n'affiche qu'une chose : **choisir le code**. Pas de plage, pas de
rôle Telecom, pas d'onglet Sécurité tant que le code n'existe pas. L'écran ne se contourne pas.

- Le code est créé **verrouillé** : la toute première action qui affaiblit la protection
  (couper une plage, retirer l'administrateur…) redemande immédiatement le code. Pas de
  fenêtre de quelques secondes où tout serait encore gratuit.
- Le code **ne peut plus être supprimé** depuis l'app. Le bouton « Supprimer » a disparu ;
  seul un **changement** est possible, et il exige l'ancien code.
- Les appareils qui ont déjà un code passent directement en mode verrouillé : pas de nouvelle
  configuration, pas de perte de données.

### Cause du bug signalé

Deux défauts cumulés sur l'installation neuve :

1. `Status.DISABLED` (aucun code) n'empêchait rien — l'onglet Sécurité proposait simplement de
   définir un code, et l'app était pleinement utilisable sans.
2. Même après création, l'état passait à `UNLOCKED` : on pouvait couper la protection dans la
   seconde sans retaper quoi que ce soit.

Les deux sont corrigés : `DISABLED` bloque l'accès à l'app, et `setPassphrase` laisse l'état
`LOCKED`.

## Vérifications

- **60 tests unitaires** verts, dont un nouveau test qui fige le comportement :
  « définir un code laisse la session verrouillée ».
- Build release R8 signé, `versionCode 4`, `versionName 1.2.0`.
- À vérifier sur appareil après installation : écran de création imposé, impossible d'atteindre
  les plages sans code, puis coupure d'une plage → code demandé même juste après la création.

## Limites (inchangées)

- Le verrou ne s'oppose ni à `adb`, ni à un effacement des données, ni au mode sans échec,
  ni à la désactivation de l'administrateur depuis les Paramètres système.
- **Code oublié = aucune récupération** : il faut effacer les données de l'app (les plages
  sont perdues) ou désinstaller après avoir retiré l'administrateur. C'est le prix d'un code
  qui n'est jamais stocké en clair.
- Mise à jour depuis la 1.1.0 : même signature, donc installation par-dessus. Les appareils
  sans code verront l'écran de création au prochain lancement.
