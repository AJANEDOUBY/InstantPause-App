## Le cercle de confiance arrive

Nouvel onglet **Contacts** en bas de l'écran : désignez jusqu'à **3 personnes** dont les appels
passent toujours, même en pleine plage de protection.

- Le bouton **+** ouvre le répertoire Android. L'app n'a **pas** la permission `READ_CONTACTS` :
  le sélecteur système ne prête que la fiche que vous venez de choisir.
- Le compteur `1 / 3` reste affiché ; arrivé à 3, le bouton d'ajout disparaît et un message
  explique qu'il faut en retirer un.
- **Ajouter ou retirer** un contact demande le code de protection, exactement comme couper ou
  supprimer une plage : c'est une action qui modifie la protection. Réactiver une plage, en
  revanche, reste libre.

## Les appels autorisés sont chronométrés

Un appel vers le cercle de confiance démarre un compte à rebours de **3 minutes** :

- une **pastille flottante** affiche le temps restant (`2:40 / 3:00`) ;
- une **notification** « Appel autorisé — Temps restant » suit le même décompte ;
- vibration à **0:30**, puis à **0:00** avec le message « Temps écoulé — raccrochez ».

⚠️ **Android interdit à une application tierce de raccrocher un appel.** Le minuteur ne coupe
donc pas la ligne : c'est un rappel, pas un coupe-circuit. La pastille s'efface dès que vous
raccrochez.

## Corrigé au passage

- Le message d'échec du code précise que **les majuscules et minuscules comptent** : c'était la
  cause la plus fréquente d'un « mon code ne marche plus ».
- L'APK de release exclut la stdlib Kotlin du repackaging R8 (`packageScope`) : sans cela,
  l'app plantait au démarrage (`IllegalAccessError` sur `kotlin.sequences`).

## À savoir avant d'installer

La clé de signature a été **régénérée** : Android refuse d'installer cette version par-dessus
une 1.2.x existante. Il faut désinstaller l'ancienne app, ce qui efface plages, code et
historique. C'est une opération à faire une fois.
