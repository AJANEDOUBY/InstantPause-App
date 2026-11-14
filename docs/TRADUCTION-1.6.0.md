# Traduction de l'interface — plan 1.6.0

État au 13 novembre 2026 (branche `release/1.5.0`, commit `9ab0449`).

## Constat

| Élément | Valeur |
|---|---|
| Chaînes réellement externalisées | 2 (`app_name`, `app_tagline`) |
| Chaînes françaises avec accents, codées en dur | **161** |
| Littéraux de 4 caractères et plus dans `ui/` | 231 (dont clés, actions, logs) |
| Fichiers concernés | 17 |
| Langues disponibles dans l'app | `values` (fr) + `values-en` (2 chaînes) |

Fichiers les plus chargés :

| Fichier | Chaînes |
|---|---|
| `ui/security/SecurityScreen.kt` | 32 |
| `ui/schedule/ScheduleScreen.kt` | 17 |
| `ui/security/PassphraseDialogs.kt` | 14 |
| `ui/security/HangupPermissionScreen.kt` | 13 |
| `system/CallTimerService.kt` | 11 |
| `ui/security/UninstallGuardScreen.kt` | 10 |
| `system/CoolDownCallRedirectionService.kt` | 10 |
| `ui/security/SetupScreen.kt` | 9 |

## Décision de structure

Le français est aujourd'hui la langue par défaut (`values/`). Pour une
publication mondiale, la langue par défaut doit être **l'anglais** :

```
res/values/strings.xml        <- anglais (fallback universel)
res/values-fr/strings.xml     <- français
res/values-es/strings.xml     <- espagnol (phase 2)
```

Raison : tout appareil dont la langue n'est pas couverte bascule sur `values/`.
Si `values/` reste en français, un utilisateur japonais verra du français.
Si `values/` est en anglais, il verra de l'anglais.

Conséquence : chaque chaîne doit être extraite avant que `values/` ne change de
langue, sinon l'app devient anglaise à moitié.

## Méthode

1. **Inventaire verrouillé** : liste `file:ligne` des 161 chaînes, convertie en
   clés (`security_title`, `zen_shorten_confirm`, …). Aucune chaîne oubliée.
2. **Extraction mécanique** dans les écrans Compose : `stringResource(R.string.x)`,
   ou `context.getString(R.string.x)` hors composition (services, notifications).
3. **Chaînes paramétrées** : `%1$s`, `%1$d`, avec `<plurals>` pour les heures
   (« 1 heure » / « 4 heures »).
4. **Bascule des dossiers** : `values/` ← anglais, nouveau `values-fr/`.
5. **Contrôle écran par écran** en anglais puis en français, sur la tablette,
   avec `adb shell cmd locale` ou le sélecteur de langue d'Android.
6. **Espagnol** dans un second temps, une fois l'anglais figé.

## Pièges identifiés

- Le message de pause personnalisable (`pause_message`) est une **donnée
  utilisateur**, pas une ressource : le défaut doit venir de `strings.xml`,
  mais une valeur déjà enregistrée ne doit pas être traduite rétroactivement.
- La notification de blocage et le service d'appel sont hors Compose :
  il faut y passer un `Context` et non un `stringResource`.
- Les textes longs (120 caractères, 14 sp) débordent sur petit écran : la
  traduction anglaise est généralement plus courte, l'espagnole plus longue.
  À revérifier après traduction.
- Les libellés courts de la barre d'onglets (`Plages`, `Sécurité`, `Journal`)
  doivent rester courts en anglais (`Windows`, `Security`, `Log`).
- Les commentaires de code restent en français : ils ne sont pas traduits.

## Découpage proposé

| Lot | Contenu | Vérification |
|---|---|---|
| 1 | Infrastructure : `values/` anglais, `values-fr/`, outillage | build + 101 tests |
| 2 | Onboarding et rôles (`MainActivity`, `RoleActivationScreen`, `HelpBanner`, `SetupScreen`) | parcours complet |
| 3 | Sécurité (`SecurityScreen`, `PassphraseDialogs`, `HangupPermissionScreen`, `UninstallGuardScreen`, `FullScreenIntentScreen`) | chaque dialogue |
| 4 | Plages et Zen (`ScheduleScreen`) | carte Zen, garde par code |
| 5 | Liste blanche et journal | ajout/suppression |
| 6 | Pause, services et notifications | appel bloqué réel |
| 7 | Espagnol | relecture native |

## Critères d'acceptation

- Aucun littéral français visible dans `ui/` et `system/` (contrôle par grep).
- `values/` entièrement anglais, `values-fr/` complet, même nombre de clés.
- Aucun `stringResource` manquant (build sans `MissingTranslation` en mode strict).
- Les 101 tests passent.
- Un appel bloqué réel s'affiche correctement dans les deux langues.

## État d'avancement — 14 novembre 2026

Réalisé sur la branche `release/1.6.0` (versionCode 16, versionName 1.6.0) :

| Lot | État |
|---|---|
| 1. Infrastructure : `values/` anglais, `values-fr/`, `locales_config.xml` | fait |
| 2. Onboarding, rôles, bandeaux d'aide | fait |
| 3. Sécurité : code, dialogues, raccrochage, plein écran, anti-désinstallation | fait |
| 4. Plages et mode Zen (dont noms des jours) | fait |
| 5. Cercle de confiance et historique | fait |
| 6. Écran de pause, services, canaux et notifications | fait |
| 7. Espagnol | à faire |

Résultat : **173 clés**, strictement identiques dans `values/` et `values-fr/`,
**100 tests, 0 échec**, vérifié sur la tablette dans les deux langues.

### Points traités pendant l'extraction

- Le message de pause n'est plus stocké avec la phrase française par défaut :
  une valeur vide est conservée et la phrase par défaut vient de
  `pause_message_default`, donc de la langue courante. Un message déjà écrit par
  l'utilisateur n'est jamais traduit.
- Le séparateur du minuteur d'appel (`« Temps restant : 2:59 / 3:00 »` contre
  `« Time left: 2:59 / 3:00 »`) vient d'une ressource, plus d'une concaténation.
- Les canaux de notification sont recréés à chaque démarrage pour que leur nom
  suive un changement de langue.
- `android:localeConfig` ajouté : l'app apparaît dans Réglages › Applications ›
  Instant Pause › Langue, et `adb shell cmd locale set-app-locales` peut la
  basculer sans changer la langue du téléphone.
- Les journaux (`Log.d`, `Log.i`…) restent en français : ils ne sont pas affichés
  à l'utilisateur.

### Reste à faire

- `values-es/` : traduction espagnole complète, puis ajout de `es` dans
  `locales_config.xml`.
- Relire les textes longs en allemand ou en espagnol si une troisième langue est
  ajoutée : ce sont les plus exposés au débordement à 14 sp.

