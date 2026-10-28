## Correctifs

- **Notification et rappels qui disparaissaient** — l'écran de pause automatique et la
  notification de blocage partageaient le même `PendingIntent` (même request code +
  `FLAG_UPDATE_CURRENT`). Comme `filterEquals()` ignore les extras, l'écran héritait de
  `fromUser = true` et annulait la notification ainsi que les 10 rappels. Les deux chemins
  ont désormais une action et un request code distincts.
- **Ré-armement opportuniste** — dès qu'un appel est bloqué, le watchdog est relancé :
  Telecom réveille déjà l'app à froid, elle en profite pour se réparer sans qu'on l'ouvre.
- **Alarme de sécurité** — `SCHEDULE_EXACT_ALARM` est déclarée (accordée d'office sur
  Android 13) avec repli automatique sur une alarme inexacte si le système la refuse.

## Ajouts

- Bandeau **« Protection en veille limitée »** lorsque l'app reste soumise à l'optimisation
  batterie, avec un bouton vers le réglage système.
- Les bandeaux **poussent le contenu** vers le bas au lieu de le recouvrir.
- `BootReceiver` instrumenté : watchdog et alarme sont traités séparément, un refus du
  système n'empêche plus l'autre, et chaque échec est journalisé.

## Limite connue (côté ROM)

Sur les surcouches agressives (itel/Transsion, Infinix, Xiaomi…), `BOOT_COMPLETED` peut
n'être **jamais délivré** tant que l'app n'est pas autorisée dans **Démarrage automatique**.
Ce n'est pas bloquant : l'interception d'appel continue de fonctionner après redémarrage
(Telecom relance le processus), et le watchdog se ré-arme au premier appel bloqué.

## Vérifications

- 22 tests unitaires verts.
- Testé sur itel A70 (Android 13) : blocage, écran de pause, notification, rappels 10 × 30 s,
  tap notification, chemin autorisé, historique, redémarrage.
