## 1.6.0 — anglais, français, espagnol

L'interface est désormais **trilingue**. L'anglais est la langue par défaut ; le français et
l'espagnol sont livrés dans la même application. La langue se choisit :

- dans l'app, **onglet Sécurité › Langue de l'application** (Android 13 et plus) ;
- dans **Réglages › Applications › Instant Pause › Langue** ;
- sinon l'app suit la langue du téléphone.

191 chaînes sont traduites dans les trois langues, sans exception. Les dates de l'historique
et les noms des canaux de notification suivent la langue choisie.

## Silence pendant la pause (optionnel, désactivé par défaut)

Pendant une plage ou un mode Zen, l'app peut aussi couper les **sonneries WhatsApp, Signal et
Telegram** grâce à l'accès au mode Ne pas déranger. Le réglage sonore précédent est rétabli à
la fin de la plage.

- **Les appels d'urgence et les alarmes ne sont jamais coupés.**
- Une coupure manuelle du mode Ne pas déranger pendant une plage n'est **pas** réappliquée
  avant la fin de cette plage.
- Si l'accès est retiré dans les réglages Android, l'app l'indique au lieu de faire semblant.

## Mode Zen

Le mode Zen se règle de **4 à 12 heures**. Le démarrer ou le prolonger est libre ; le
raccourcir ou l'arrêter exige le code.

## Sous le capot

- `LocaleManager` et `locales_config.xml` : l'app apparaît dans le sélecteur de langue Android.
- Nouveau module `domain/dnd` : état, plage, restauration après redémarrage.
- `ProtectionState` / `ProtectionWindow` : une seule source de vérité pour « suis-je protégé ».
- 117 tests unitaires, 0 échec ; `lintDebug` sans erreur.

## Mise à jour

Aucune migration de base de données : les installations 1.4.0 et 1.5.0 passent en 1.6.0 sans
perte de données. La signature est inchangée
(`4F:14:D6:…:1B:73:1A:F0:F5`).

> Si une version **debug** est déjà installée, la désinstaller d'abord : la signature diffère
> (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
