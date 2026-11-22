## 1.6.1 — correctifs de fiabilité

Version de correction, sans nouvelle fonction : trois défauts trouvés à l'audit de la 1.6.0 sont
corrigés.

## Ne pas déranger : plus de blocage possible

Avec l'option « Silence pendant la pause », plusieurs écrans et récepteurs pouvaient resynchroniser
le filtre en même temps. Dans ce cas, le filtre posé par l'app pouvait être mémorisé comme
« réglage précédent », et le téléphone restait alors en Ne pas déranger après la fin de la plage.

- Les opérations sur le filtre sont maintenant **sérialisées**.
- Si le téléphone est **déjà** en « Prioritaires seulement » au début d'une plage, l'app n'y touche pas
  et ne le modifie pas à la fin : votre réglage reste intact.
- Un état enregistré par la 1.6.0 qui aurait gardé « Prioritaires seulement » comme réglage précédent
  est ramené au **mode normal** à la fin de la plage.

## Code de protection : l'heure du téléphone ne compte plus

La temporisation après plusieurs codes faux, et le délai d'arrière-plan de 60 secondes, se
mesuraient avec l'horloge du téléphone. Avancer l'heure dans les réglages Android permettait de
les raccourcir.

- Ces délais sont désormais mesurés avec le **temps écoulé depuis le démarrage**, indépendant de
  l'heure affichée.
- Après un **redémarrage**, la temporisation est comptée depuis le démarrage : elle ne repart pas
  de zéro et ne dépasse jamais le délai d'origine (15 minutes au maximum).
- Un état enregistré par une version antérieure reste lisible ; son échéance est plafonnée à
  15 minutes.

## Cercle de confiance : ajout d'un contact plus robuste

Si le répertoire du téléphone renvoie une réponse inattendue ou refuse la lecture, l'app affiche
« lecture impossible » au lieu de s'arrêter.

## Documents

- La **politique de confidentialité** décrit le mode silencieux : filtre « Prioritaires seulement »,
  réglage précédent mémorisé localement, ce que fait Android si l'app est désinstallée pendant
  une plage, et le fait que l'app ne lit jamais le contenu des notifications.
- `SECURITY.md` : la 1.6.x est la version supportée, le dépôt est public.

## Sous le capot

- `DndController` : `Mutex` autour de `sync`, `setEnabled` et `restoreNow`.
- `AppLockManager` : nouvelle interface `MonotonicClock` (`SystemClock.elapsedRealtime()` en
  production) ; `LockoutState` enregistre l'instant monotone et la durée de la temporisation.
- 11 tests unitaires ajoutés : `DndControllerConcurrencyTest` (4) et `AppLockManagerClockTest` (7).

## Mise à jour

Aucune migration de base de données : les installations 1.4.0 à 1.6.0 passent en 1.6.1 sans perte
de données. Les deux nouvelles valeurs du verrou sont facultatives. La signature est inchangée
(`4F:14:D6:…:1B:73:1A:F0:F5`).

> Si une version **debug** est déjà installée, la désinstaller d'abord : la signature diffère
> (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
