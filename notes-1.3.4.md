## Le raccrochage à 3 minutes n'est plus une option

Un contact autorisé peut toujours vous joindre pendant une plage protégée. Mais la limite
de 3 minutes devait être **garantie**, pas seulement affichée : sans l'autorisation
« Passer et gérer des appels téléphoniques », l'app pouvait compter le temps à l'écran
sans jamais couper la ligne. On pouvait donc parler une heure à un contact autorisé.

Désormais, cette autorisation fait partie de l'installation, au même titre que la
protection anti-désinstallation. L'app **ne s'ouvre pas** tant qu'elle manque.

- L'écran de demande Android s'ouvre **automatiquement** à cette étape.
- Refus ou retour arrière → l'app reste bloquée là, avec un bouton pour réessayer.
- Refus définitif → Android ne réaffiche plus la demande ; l'écran propose alors
  d'ouvrir les réglages de l'app.
- La vérification est **permanente** : retirer la permission depuis les réglages Android
  ramène à cette étape au lancement suivant.

Le bandeau violet « Raccrochage automatique » disparaît : il n'avait de sens que tant que
la permission était facultative.

## Ordre des étapes

```
Code de protection  →  Anti-désinstallation  →  Raccrochage à 3 minutes  →  Rôle appels
```

Chaque étape est verrouillée par des tests (`OnboardingStageTest`) : l'ordre est la
protection elle-même, pas un détail d'interface.

## Pourquoi Android impose une étape

`TelecomManager.endCall()`, la seule API qui permette à une app tierce de couper une ligne,
exige `ANSWER_PHONE_CALLS`. Une application ne peut pas s'octroyer une permission
dangereuse : le dialogue système est obligatoire. Instant Pause l'enchaîne donc dans le
parcours d'installation plutôt que de le laisser en option.
