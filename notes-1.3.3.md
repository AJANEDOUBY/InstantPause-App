## La protection anti-désinstallation s'active à l'installation

Avant, il fallait penser à aller dans l'onglet **Sécurité** pour activer la protection
contre la désinstallation. Rien n'obligeait à le faire : un utilisateur pouvait traverser
toute l'installation sans jamais l'activer, puis désinstaller l'app en trois taps — et
perdre ses plages avec elle.

Désormais, l'étape **« Protéger la désinstallation »** fait partie de l'installation :
elle arrive juste après la création du code, avant l'autorisation des appels, et l'app ne
va pas plus loin tant qu'elle n'est pas franchie.

À noter, honnêtement : **Android interdit à une application de s'activer elle-même** comme
administrateur. La confirmation système reste obligatoire — c'est une protection contre les
apps malveillantes. Instant Pause l'enchaîne donc automatiquement dans le parcours
d'installation plutôt que d'attendre que vous la trouviez dans un onglet.

- L'écran de confirmation Android s'ouvre **tout seul** après la création du code.
- Si vous refusez, l'app reste bloquée sur cette étape (le bouton permet de réessayer).
- Une fois franchie, l'étape ne revient plus jamais — même si vous retirez
  l'administrateur plus tard avec votre code, depuis l'onglet Sécurité.

## Les installations existantes y passent aussi

Les appareils où l'app est déjà installée (et où l'administrateur n'a jamais été activé)
verront cette étape une seule fois au prochain lancement. Ceux qui l'avaient déjà activée
ne voient rien.

## Détail pour les développeurs

Les builds `debug` utilisent maintenant l'identifiant `com.cooldown.app.debug` : on peut
tester un onboarding à neuf sur un appareil où la version release est installée, sans
toucher à ses données.
