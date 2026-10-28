## Ce qui a été trouvé sur la tablette

La 1.2.0 a été installée sur la Galaxy Tab A11 (Android 16 / One UI 8.5). Le verrou fait bien son
travail : création du code imposée au premier lancement, puis « Code requis » dès qu'on touche à
une plage, mauvais code → « Encore 2 essai(s) ». Deux défauts d'affichage sont apparus pendant
ce test.

## Correctifs

### Les plages disparaissaient sous les bandeaux

Sur un écran en paysage, les deux bandeaux d'aide (« Autoriser l'affichage » et « Autoriser en
arrière-plan ») occupaient plus de 500 px. Résultat : la liste des plages recevait une hauteur
nulle. On pouvait ajouter une plage — elle était bien enregistrée — mais rien ne s'affichait,
ce qui donnait l'impression que l'app était cassée.

- Les bandeaux sont désormais **compacts** : une icône, deux lignes, un bouton sur la même rangée.
- Tout l'écran des plages (statut, mode Zen, liste) défile dans **une seule liste** : la première
  plage est toujours visible, le reste se rejoint en faisant défiler.

### Vérifications sur la tablette

- Installation neuve (`pm clear`) → écran de création du code, impossible d'aller plus loin.
- Couper une plage juste après la création → code demandé immédiatement.
- Mauvais code → « Code incorrect. Encore 2 essai(s) avant une temporisation. »
- Bon code → la plage se coupe, la session reste déverrouillée 60 s.
- Onglet Sécurité : « Changer le code » seulement (plus de suppression), version affichée en bas.
- Aucun crash R8.

## Rappel

Le code est **obligatoire et non supprimable** depuis l'app. Sur un appareil qui n'en a pas
encore (mise à jour depuis la 1.1.0), l'écran de création s'affiche au prochain lancement.
