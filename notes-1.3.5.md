## L'écran de pause ne rouvre plus les réglages à chaque lancement

Depuis Android 14, l'autorisation « affichage en plein écran » n'est plus accordée
d'office. L'app ouvrait l'écran de réglages correspondant **à chaque ouverture** tant
qu'elle manquait : plus intrusif que le problème lui-même.

Désormais, c'est une **étape d'installation**, au même endroit que les autres :

```
Code  →  Anti-désinstallation  →  Raccrochage 3 min  →  Rôle appels  →  Plein écran  →  App
```

- Le réglage Android s'ouvre **automatiquement** à l'arrivée sur l'étape.
- Contrairement au raccrochage, elle **n'est pas bloquante** : l'appel est bloqué dans
  tous les cas. Sans l'autorisation, la pause s'affiche seulement comme une notification
  qu'il faut toucher.
- « Continuer sans » ferme l'étape définitivement : elle ne revient plus.
- Autorisation accordée → l'étape disparaît d'elle-même, et ne revient pas même si elle
  est retirée plus tard.

## Pourquoi elle n'est pas obligatoire

Elle ne conditionne pas la protection : elle conditionne seulement la **façon** dont la
pause apparaît. Bloquer toute l'app pour un confort d'affichage aurait été disproportionné,
et Android n'expose aucun dialogue de permission : il faut passer par les réglages
système, que l'utilisateur doit pouvoir quitter sans être piégé.

## Détail technique

Le nouvel état `FULL_SCREEN_INTENT` s'intercale après le rôle Telecom. `MainActivity`
lit `NotificationManager.canUseFullScreenIntent()` à chaque `onResume` (avant Android 14,
la permission est normale et donc acquise), et le drapeau `fullscreen_intent_done`
empêche l'étape de revenir. 5 tests supplémentaires verrouillent cet ordre.
