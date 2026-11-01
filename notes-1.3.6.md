## Une version de nettoyage, sans changement de comportement

Pas de nouvelle fonctionnalité ici : uniquement du code mort enlevé et des avertissements
du linter traités. Le but est que la prochaine évolution (blocage des appels WhatsApp)
parte d'une base propre plutôt que d'un projet qui traîne 41 avertissements.

**Ce que vous pouvez constater :** rien. Strictement rien. Les chemins d'exécution sont
identiques, les 81 tests passent, et le lint passe de **41 à 17 avertissements** sans
aucune erreur.

## Ce qui a été enlevé

L'app cible au minimum **Android 10** (`minSdk 29`). Toutes les précautions prises pour
Android 8 et antérieur ne pouvaient donc jamais s'exécuter :

- plus de repli sur `TYPE_PHONE` (déprécié depuis Android 8) pour la pastille du minuteur ;
- plus de `startService()` de secours avant chaque `startForegroundService()` ;
- plus de `Vibrator.vibrate(long)` déprécié dans les trois endroits qui vibraient encore ;
- plus de test `SDK_INT` avant de créer un canal de notification (les canaux existent
  depuis Android 8).

Au total **10 vérifications de version mortes** dans 6 fichiers.

## Deux cas traités différemment

**`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`** n'existe que depuis Android 14, et le linter
le signale. Mais c'est une constante `static final int` : elle est recopiée à la
compilation, et `ServiceCompat` ignore le type sur les Android antérieurs. Aucune garde
de version n'est utile — la seule chose honnête était de l'écrire, via un
`@SuppressLint("InlinedApi")` commenté. Une garde `SDK_INT` ici aurait été du décor.

**`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** reste tel quel. Google Play encadre cette
permission, mais l'app n'est pas publiée sur le Play Store. Le dialogue système direct
est bien plus clair que la liste générale des réglages. À rouvrir le jour où Play devient
un objectif.

## Ressources mortes

- `values/colors.xml` : les sept couleurs du template Android Studio, jamais utilisées ;
- `drawable/ic_launcher_foreground.xml` : remplacé depuis longtemps par les `.webp` ;
- `mipmap-anydpi-v26/` renommé en `mipmap-anydpi/` : le suffixe `-v26` ne sert plus à
  rien avec un minimum Android 10.

## Sur l'audit qui a motivé ce travail

L'audit signalait aussi deux choses qui n'en sont pas :

- **un risque de threading** dans `CallTimerOverlay` : tout passe par un `Handler` sur le
  thread principal, de `onStartCommand` à `onDestroy`. Rien à corriger ;
- **`tools:ignore="FullScreenIntentRule"`** : cet identifiant de lint n'existe pas. Les
  deux avertissements d'écran plein écran viennent des *Play Policy Insights* d'Android
  Studio, pas du lint Gradle, et n'apparaissent pas dans le rapport. L'ajouter aurait
  donné l'illusion d'un correctif.
