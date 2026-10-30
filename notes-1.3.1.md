## L'appel autorisé se coupe vraiment à 3 minutes

Jusqu'ici, le minuteur se contentait de prévenir : à 0:00 la pastille affichait « Temps
écoulé — raccrochez » et la conversation continuait si vous ne raccrochiez pas.

Désormais, **l'appel est raccroché automatiquement** à la fin des 3 minutes.

Pour cela, Instant Pause demande une permission supplémentaire : **« passer et gérer des
appels téléphoniques »** (`ANSWER_PHONE_CALLS`). C'est la seule qui autorise une application
tiers à couper une ligne — sans elle, Android l'interdit.

- Accordée : à 0:00, la ligne est coupée, la notification affiche « Appel coupé ».
- Refusée : rien de cassé, le minuteur redevient un rappel (« Temps écoulé — raccrochez »).
  Un bandeau violet en haut de l'écran propose de l'accorder, et la question est reposée à
  chaque lancement de l'app.

## Le décompte part de la connexion

Autre correction : le compte à rebours démarrait **au moment de composer**. Une sonnerie de
40 secondes avant décroché mangeait donc 40 secondes de conversation.

Le décompte se décale maintenant au premier signe de communication établie : vous avez bien
**3 minutes de parole**, pas 3 minutes incluant la sonnerie.

## Rappel : la limite ne concerne que les contacts autorisés

Les appels vers les autres numéros restent bloqués pendant les plages. Les 3 contacts du
cercle de confiance passent, mais sont chronométrés.
