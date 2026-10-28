## CoolDown s'appelle maintenant Instant Pause

Le nom affiché change partout : icône, écran d'accueil, notifications, écran Sécurité. Le
**paquet reste `com.cooldown.app`**, donc la mise à jour s'installe par-dessus l'ancienne version
sans perdre vos plages, votre code ni l'historique.

Rien à refaire : vos données sont déjà là après l'installation.

## Une seule notification par appel bloqué

Avant, l'alerte « Appel bloqué » était re-postée **10 fois à 30 secondes d'intervalle**, avec une
vibration à chaque fois. C'était inutile — l'appel est déjà refusé, re-sonner n'y change rien — et
surtout énervant : au bout de trois alertes, on coupe les notifications de l'app, et on ne voit
plus jamais l'alerte utile.

Désormais :

- **une** notification, au moment du blocage ;
- **une** vibration ;
- elle reste dans le tiroir jusqu'à ce que vous la tapiez ou la balayiez ;
- plus de service de rappel en arrière-plan (il est supprimé du code et du manifeste).

## Vérifié sur la tablette

Testé sur la Galaxy Tab A11 (Android 16 / One UI 8.5) par-dessus la 1.2.1, données conservées :

- après mise à jour, l'app s'ouvre avec le titre **Instant Pause** et la plage 14:52–16:52 intacte ;
- appel vers `000000` → écran de pause + **une** notification « Appel bloqué » ;
- surveillance du tiroir et des journaux pendant **94 secondes** : aucune nouvelle notification,
  aucune vibration, aucun service de rappel relancé.

## Rappel

Le code de protection reste **obligatoire et non supprimable**. Sur un appareil qui n'en a pas
encore (mise à jour depuis la 1.1.0), l'écran de création s'affiche au prochain lancement.
