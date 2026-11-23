## 1.7.0 — retrait de l'administrateur d'appareil

Version de conformité : l'application ne demande plus à devenir **administrateur d'appareil**.
C'est une exigence de la revue Google Play en production, et le bénéfice réel était mince.

## Pourquoi ce retrait

Depuis la 1.0, un récepteur `DeviceAdminReceiver` déclarait un administrateur **sans aucune
politique** (aucun verrouillage d'écran, aucun effacement à distance). Son seul effet était
qu'Android refuse une désinstallation tant que l'administrateur est actif : deux taps de friction
pour quelqu'un de décidé.

Or Google Play considère comme un usage abusif un administrateur dont l'unique fonction est de
bloquer la désinstallation. Le risque de rejet en production — et de suspension d'un compte
développeur qui vient d'obtenir ses 12 testeurs — n'était pas justifié par un ralentisseur
cosmétique.

## Ce qui change

- Le blocage de désinstallation n'existe plus. L'application se désinstalle comme n'importe
  quelle autre.
- L'écran d'installation « Protégez la désinstallation » est supprimé : après la création du
  code, l'application passe directement au raccrochage à 3 minutes, puis au rôle Telecom.
- La carte « Désinstallation » de l'onglet **Sécurité** est supprimée.
- La préférence interne `uninstall_guard_done` des versions 1.6.x n'est plus lue : elle est
  simplement ignorée, sans migration ni erreur.

## Ce qui ne change pas

- **Le verrouillage par code alphanumérique reste intact** : `AppLockManager` est inchangé.
  Couper ou supprimer une plage, changer le code, tout passe toujours par le code, avec
  temporisation progressive et délai d'arrière-plan mesuré en temps monotone.
- Aucune autre permission n'est retirée : `ANSWER_PHONE_CALLS`, le rôle Telecom, le mode
  Ne pas déranger optionnel et le service de premier plan `specialUse` restent identiques.

## Utilisateurs venant d'une version antérieure

Si l'administrateur Instant Pause avait été activé en 1.6.x, Android peut continuer à
l'afficher dans *Paramètres → Sécurité → Applications d'administration* après la mise à jour,
puisque la 1.7.0 ne déclare plus le composant. Il suffit de le désactiver une fois à cet
endroit ; l'application ne peut plus le faire elle-même.

## Documents

- `PRIVACY_POLICY.md` (anglais, espagnol, français) : la ligne « Device administrator » et la
  référence à sa désactivation sont retirées.
- `docs/FICHE-PLAY.md` et `docs/GUIDE-PLAY.md` : la déclaration d'usage correspondante est
  retirée des textes à coller dans la Play Console.

## Sous le capot

- Supprimés : `system/CoolDownDeviceAdminReceiver.kt`, `res/xml/device_admin.xml` et le
  `<receiver>` du manifeste.
- `OnboardingStage` perd `PROTECT_UNINSTALL` et les paramètres `adminActive` /
  `guardAcknowledged` ; `OnboardingStageTest` est réécrit en conséquence.
- `SecurityScreen` perd la carte de désinstallation et le dialogue associé.
- `versionCode` 18, `versionName` 1.7.0.

## Mise à jour

Aucune migration de base de données : les installations 1.4.0 à 1.6.1 passent en 1.7.0 sans perte
de données. La signature est inchangée (`4F:14:D6:…:1B:73:1A:F0:F5`).

> Si une version **debug** est déjà installée, la désinstaller d'abord : la signature diffère
> (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
