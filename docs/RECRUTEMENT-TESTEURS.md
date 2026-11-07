# Recruter les 12 testeurs — Reddit et règles Play

## Réponse courte

**`r/AndroidClosedTesting` : oui, c'est fait pour ça.**
**`r/androiddev` : non, votre message sera supprimé.**

Et il y a trois problèmes Play à régler **avant** de poster, sinon les inscriptions ne
compteront pas.

---

## 1. `r/androiddev` — le mauvais endroit

Ce subreddit est un espace de **discussion sur le développement Android**, pas d'acquisition
d'utilisateurs. La promotion de sa propre application y est encadrée : elle va dans le fil
hebdomadaire de présentation (« Saturday Showcase »), pas dans un post autonome.

Un post « cherchez testeurs pour mon app » y est traité comme de l'auto-promotion : suppression,
et au mieux un avertissement. Répété, c'est un bannissement du sub, voire un signalement
anti-spam au niveau du compte Reddit.

**Ce qui marcherait là-bas** : un retour d'expérience technique, pas une demande. Par exemple
« comment j'ai implémenté `CallRedirectionService` et ce que Google exige pour
`FOREGROUND_SERVICE_SPECIAL_USE` ». L'app est mentionnée en fin de message, le lien de test
n'est pas le sujet. C'est du contenu, pas de la collecte.

**À vérifier avant de poster** : l'édition en cours des règles du sub (barre latérale). Elles
changent, et les modérateurs appliquent la version du jour, pas celle d'un guide.

---

## 2. `r/AndroidClosedTesting` — le bon endroit, avec un format

Le sub existe précisément pour les tests fermés Play. Ses usagers sont des développeurs qui
cherchent des testeurs **et** qui en fournissent. Trois conséquences pratiques :

- **Ils connaissent le sujet.** Ils savent qu'une app qui demande l'administrateur d'appareil
  est suspecte par défaut. Préparez l'explication avant de poster, pas après la première
  question.
- **Ils attendent de la réciprocité.** Beaucoup testent votre app en échange de votre test de
  la leur. Ce n'est pas une règle Play, mais c'est l'usage du sub. Refuser systématiquement
  vous coûtera des testeurs.
- **Ils vérifient le lien avant de cliquer.** Un lien mort (« App not available ») grille votre
  crédibilité en une seconde. Publiez la release **avant** de poster.

---

## 3. Les trois problèmes Play à régler d'abord

### a. Les pays

Un testeur ne peut installer que si **son pays** figure dans la disponibilité du canal fermé.
Si vous n'avez coché que la France et le Maroc, un Redditor aux États-Unis verra « indisponible
dans votre pays » — et vous l'aurez recruté pour rien.

**À faire** : `Tester et publier → Tests fermés → Gérer le canal → Pays et régions`. Ajouter au
minimum les pays de vos testeurs. Le réglage s'applique à tous les canaux, mais reste modifiable
avant la production.

### b. Les adresses e-mail

Le canal fermé accepte deux modes :

| Mode | Ce que le testeur doit faire | Adapté à Reddit ? |
|---|---|---|
| Adresse e-mail | vous donner son adresse, vous l'ajoutez, il accepte | ❌ un inconnu ne donne pas son Gmail |
| **Google Groupes** | rejoindre un groupe public avec son compte Google | ✅ oui |

Créez un groupe Google (`instantpause-testers@googlegroups.com`), mettez-le comme accès testeur
du canal, et les Redditors n'ont plus qu'à le rejoindre. C'est le mode standard du sub.

**Si vous tenez à la liste d'e-mails**, une étape est obligatoire dans le post : dire au
testeur d'**attendre votre réponse avant de cliquer**. Sans ça, il clique dans la seconde, voit
« App not available », conclut que c'est cassé et ne revient pas. Vous avez perdu un testeur
pour un délai d'ajout de dix minutes.

Précisez aussi que l'adresse donnée doit être **le compte Google connecté sur son téléphone**,
pas une adresse de contact.

### c. Les 14 jours consécutifs

Chaque testeur doit rester **inscrit 14 jours d'affilée**. S'il quitte le programme, **son**
compteur repart à zéro — les autres ne sont pas affectés.

Un inconnu de Reddit s'inscrit souvent puis se désintéresse. **Recrutez 20 pour viser 12
stables.** Tenez un tableau : pseudo, date d'inscription, date d'installation.

---

## 4. Ce qu'il faut assumer devant des développeurs

Votre app demande l'administrateur d'appareil et `ANSWER_PHONE_CALLS`. Sur un sub de
développeurs, c'est un drapeau rouge, et ils le diront.

**Ne fuyez pas.** Trois phrases suffisent :

- aucun compte, aucun serveur, **aucune permission `INTERNET`** — vérifiable dans le manifeste ;
- l'administrateur sert **uniquement** à freiner la désinstallation, aucune politique imposée ;
- `ANSWER_PHONE_CALLS` sert **uniquement** à couper à 3:00 les appels vers les trois contacts
  autorisés.

Le fait vérifiable (« pas de permission INTERNET ») vaut mieux que dix promesses. C'est aussi
la seule chose que les autres bloqueurs de la catégorie ne peuvent pas dire.

---

## 5. Modèle de post — anglais

À adapter, pas à copier tel quel : le sub repère les messages génériques.

```
[Closed test] Instant Pause — block impulsive outgoing calls, 12 testers needed (14 days)

What it does
During time windows you choose (e.g. 22:30–07:30), any outgoing call is intercepted and
replaced by a pause screen: breathe, then decide. Three trusted contacts stay reachable,
capped at 3 minutes. Emergency numbers are never intercepted.

Why it is different
No account. No ads. No analytics. No server. The app has no INTERNET permission at all —
everything stays in the app's private storage. It is a self-control tool, not parental
controls: it slows down an impulse, it does not lock a device.

Permissions, and why (this is the part you will ask about)
• Call redirection role — Android's own API to intercept an outgoing call before it connects.
• Device admin — used only to make uninstalling take a few extra taps. No policy, no wipe.
• ANSWER_PHONE_CALLS — used only to hang up at 3:00 on the three trusted contacts.
• Full-screen intent — to show the pause screen over the dialer.

What I need
12 testers who stay opted in for 14 consecutive days. Install and keep the app installed.
Feedback by email or in this thread — I will fix what you report.

How to join
1. Join the Google Group: <lien du groupe>
2. Open: https://play.google.com/apps/testing/com.instantpause.app
3. Accept, install, keep it 14 days.

Available in: France, Morocco, <ajouter les pays>.
Privacy policy: https://ajanedouby.github.io/instant-pause-privacy/

Happy to test your app in return — post your link.
```

---

## 6. Ce qui ne se fait pas

| Interdit | Pourquoi |
|---|---|
| Payer des testeurs | incentive contraire aux règles Play, et Google vérifie la genuine-ness |
| Demander un avis 5 étoiles en échange | manipulation des avis, sanction possible |
| Poster le même lien dans 10 subreddits le même jour | spam au niveau du compte Reddit |
| Promettre « testez la mienne, je teste la vôtre » avec installation sans ouverture | ne compte pas comme test réel, et la question de production le révèlera |
| Recruter avant que la release soit publiée | liens morts, réputation grillée |

---

## 7. Ordre des opérations

1. Publier la release fermée, attendre le statut **Publiée**.
2. Vérifier que le lien répond sur un compte **non testeur** (message d'erreur normal) et sur
   un compte testeur (« Accepter »).
3. Ajouter les pays nécessaires.
4. Créer le Google Group, le brancher comme accès testeur, enregistrer.
5. Poster dans `r/AndroidClosedTesting` uniquement.
6. Pour `r/androiddev` : viser le fil de présentation hebdomadaire, avec un angle technique.
7. Suivre les installations dans un tableau, viser 20 inscrits pour 12 stables.

La question de production demandera **comment vous avez trouvé vos testeurs et ce que vous avez
corrigé grâce à eux**. Répondez franchement : « Reddit, 20 inscrits, 14 retours, 3 corrections »
est une bonne réponse. « 12 inconnus n'ont jamais ouvert l'app » en est une mauvaise.

---

## 8. Le brouillon « [MUTUAL TEST] … Default PIN 1234 » — à ne pas publier tel quel

Cinq problèmes, dont trois éliminatoires.

| # | Ce qui est écrit | Problème |
|---|---|---|
| 1 | « Default PIN (if prompted): 1234 » | **Faux.** L'app demande de **créer** un code : 6 caractères minimum, au moins une lettre et un chiffre. `1234` est refusé par `PassphrasePolicy`. Le testeur reste bloqué au premier écran. |
| 2 | « leave a review » / « provide … a 5-star review » | **Manipulation d'avis.** Échanger une note contre une installation est contraire aux règles Play, sanctionnable sur l'app et le compte. Et matériellement impossible : les testeurs **ne peuvent pas** laisser d'avis public sur une version de test. |
| 3 | « block distractions, mute unwanted calls » | **Descriptif faux.** L'app ne touche pas aux appels entrants et ne « mute » rien. C'est un bloqueur d'appels **sortants**. Un testeur venu pour de l'anti-spam désinstalle. |
| 4 | « Google Group / Join Test » sans lien de groupe | Le canal fermé n'accepte que les comptes de la liste **ou** les membres du groupe. Sans groupe, le clic donne « App not available ». |
| 5 | Lien `details?id=…` pour une app en test fermé | Un non-testeur voit « introuvable ». Seul le lien `/apps/testing/…` a du sens avant la production. |

Deux manques qui coûtent des testeurs :

- **les permissions** (administrateur d'appareil, `ANSWER_PHONE_CALLS`) ne sont pas annoncées ;
  sur ce sub, les gens les découvrent à l'écran système et abandonnent ;
- **les pays**. Un testeur hors France/Maroc ne peut pas installer.

### Version corrigée

```
[Closed test] Instant Pause — stop impulsive outgoing calls (12 testers, 14 days)
Happy to test your app in return.

WHAT IT DOES
During time windows you choose (e.g. 22:30–07:30), any outgoing call is intercepted and
replaced by a pause screen: breathe, then decide. Three trusted contacts stay reachable,
capped at 3 minutes. Emergency numbers are never intercepted.

WHAT IT IS NOT
Not a spam blocker. It never touches incoming calls, never mutes anything, and is not a
parental control tool. It slows down one specific impulse: calling someone you will regret
calling.

PRIVACY
No account, no ads, no analytics, no server. The app has no INTERNET permission at all,
so everything stays in its private storage on the device.

PERMISSIONS YOU WILL BE ASKED FOR (and why)
• Call redirection role — Android's own API to intercept an outgoing call before it connects.
• Device admin — only to make uninstalling take a few extra taps. No policy, no wipe.
• Phone permission (ANSWER_PHONE_CALLS) — only to hang up at 3:00 on the three trusted contacts.
• Full-screen intent — to show the pause screen over the dialer.

SETUP
On first launch the app asks you to CREATE a protection code — there is no existing PIN.
Rules: at least 6 characters, with at least one letter and one digit. For example: review2026
You will only be asked for it again to weaken the protection.

WHAT I NEED
12 testers who stay opted in for 14 consecutive days. Please install and keep the app.

HOW TO JOIN
1. Join the Google Group: <lien du groupe>
2. Open: https://play.google.com/apps/testing/com.instantpause.app
3. Accept, install, keep it 14 days.

Available in: France, Morocco, <autres pays>.
Privacy policy: https://ajanedouby.github.io/instant-pause-privacy/

FEEDBACK
Reply in this thread or email janedouby@gmail.com. I will fix what you report.

IN RETURN
Drop your link and I will join your test and keep your app installed for 14 days.
I will not promise reviews: test builds cannot receive public reviews anyway.

Thanks!
```

Différences qui comptent : le PIN devient « créez un code », l'échange d'avis disparaît, le
descriptif redevient exact, les permissions sont annoncées avant le clic, le groupe et les pays
sont explicites.

### Si le post est déjà en ligne

Modifiez-le dans l'heure. Les deux lignes à retirer en priorité : le PIN `1234` et la promesse
d'avis. La première bloque vos testeurs, la seconde vous expose.

---

## 9. Créer le Google Groupe des testeurs

Un compte `@gmail.com` suffit. Aucun abonnement Workspace, aucun nom de domaine à acheter : le
groupe sera créé dans le domaine public `googlegroups.com`.

### a. Création

1. Ouvrir <https://groups.google.com> avec `janedouby@gmail.com` (le même compte que la Play
   Console, pour éviter de chercher où est le groupe plus tard).
2. En haut de la page : **Créer un groupe**.
3. Renseigner :

| Champ | Valeur |
|---|---|
| Nom du groupe | `Instant Pause Testers` |
| E-mail du groupe | `instant-pause-testers` (le domaine `googlegroups.com` est imposé) |
| Description | `Closed testers for Instant Pause (com.instantpause.app). Join, then open the Play testing link.` |

L'adresse complète devient `instant-pause-testers@googlegroups.com`. Si le nom est déjà pris,
ajouter une lettre : `instant-pause-test` ou `instantpause-testers`. L'adresse exacte n'a aucune
importance ; seul le lien du groupe compte.

### b. Les paramètres qui comptent

C'est ici que se joue l'inscription. Un réglage trop strict et le Redditor ne peut pas rejoindre
seul ; il abandonne avant de vous avoir écrit.

| Paramètre | Valeur | Pourquoi |
|---|---|---|
| Qui peut rechercher le groupe | **Tous les internautes** | le groupe doit être trouvable par un inconnu |
| Qui peut rejoindre le groupe | **Tout le monde peut rejoindre le groupe** | adhésion immédiate, sans votre validation |
| Qui peut afficher les conversations | **Tous les internautes** | sinon la page du groupe est **fermée aux non-membres** : le visiteur voit « Vous ne pouvez pas voir les contenus de ce groupe » et aucun bouton Rejoindre |
| Qui peut publier des messages | **Gestionnaires du groupe** (ou propriétaires) | un groupe ouvert en écriture devient une boîte à spam |
| Qui peut afficher la liste des membres | **Gestionnaires du groupe** | protège les adresses des testeurs |

Le réglage « conversations visibles par les membres seulement » paraît plus propre, et il casse
tout : Google Groupes masque alors la page entière aux non-membres. Un testeur qui clique sur le
lien ne voit pas un bouton « Rejoindre », il voit une erreur. La page doit être publique ; c'est
l'écriture qui doit rester privée.

Ne pas choisir « Tout le monde peut **demander à** rejoindre » : chaque adhésion attend votre
approbation, et un testeur qui attend une approbation le soir ne revient pas le lendemain.
« Rejoindre » direct, oui ; « demander à rejoindre », non.

### c. Récupérer le lien

Après création, le groupe a une URL de la forme :

```
https://groups.google.com/g/instant-pause-testers
```

C'est cette adresse qui remplace `<lien du groupe>` dans le post Reddit. Elle doit s'ouvrir dans
une fenêtre de navigation privée, sans être connecté, et proposer un bouton **Rejoindre le
groupe**. Si la page demande une connexion sans proposer le bouton, le paramètre « Qui peut
rejoindre » est encore sur « Utilisateurs invités uniquement ».

### d. Brancher le groupe dans la Play Console

1. `Tester et publier → Tests fermés → Gérer le canal`.
2. Onglet **Testeurs**.
3. Choisir **Google Groupes** (et non « Adresse e-mail »).
4. Coller `instant-pause-testers@googlegroups.com`.
5. **Enregistrer**, puis vérifier que le groupe apparaît bien dans la liste des accès.

Le compte Google utilisé pour installer l'app doit être **le compte membre du groupe**. Un
testeur qui rejoint le groupe avec une adresse et installe avec une autre verra « App not
available ».

### e. Vérifier avant de poster

- Se déconnecter (ou navigation privée), ouvrir le lien du groupe : le bouton **Rejoindre** est
  visible.
- Rejoindre avec un compte secondaire, puis ouvrir
  <https://play.google.com/apps/testing/com.instantpause.app> : la page doit proposer
  **Accepter et télécharger**, pas « App not available ».
- Compter quelques minutes après la création du groupe : la propagation entre Google Groupes et
  la Play Console n'est pas instantanée.

### f. Faut-il supprimer les adresses saisies à la main ?

**Non, pas avant d'avoir vérifié.** Trois règles.

1. **Ne supprimez jamais un testeur déjà installé.** Le retirer de la liste lui coupe l'accès au
   canal et peut remettre à zéro sa période de 14 jours. Un testeur perdu se remplace mal.
2. **Basculez la méthode d'accès, pas la liste.** L'onglet Testeurs propose « Adresse e-mail »
   ou « Google Groupes ». Choisir le groupe désactive la liste d'adresses sans la détruire :
   elle reste dans le compte et peut être réactivée plus tard.
3. **Faites rejoindre le groupe avant de basculer** à ceux qui testent déjà par adresse e-mail.
   Sinon ils perdent l'accès au prochain enregistrement.

Les adresses qui n'ont jamais installé l'application ne risquent rien : elles peuvent être
supprimées après coup, sans effet sur le quota.

### g. Le piège du test interne

Un compte qui a activé le **test interne** ne peut pas recevoir le test fermé. Il doit d'abord
désactiver le test interne dans l'application Play Store, puis accepter le test fermé. C'est
notamment le cas de la tablette de développement : tant qu'elle est en test interne, elle ne
compte pas parmi les 12.

---

## 10. Post final, prêt à publier

Les liens sont définitifs et les pays couvrent le monde entier (178), donc le texte ci-dessous
peut être collé tel quel. Il remplace les modèles des sections 5 et 8, qui contenaient des
espaces réservés.

**Titre** (à coller dans le champ titre) :

```
[Closed Test] Instant Pause – block impulsive outgoing calls (12 testers, 14 days, worldwide)
```

**Corps** (garder les **lignes vides** : Reddit ignore un simple retour à la ligne et collerait
tout le texte en un seul paragraphe) :

```
Hi everyone,

I'm looking for 12 testers for Instant Pause, an Android app that helps you resist impulsive calls. Happy to test your app in return — just drop your link.

WHAT IT DOES

During the time windows you choose (e.g. 22:30–07:30), any outgoing call is intercepted and replaced by a pause screen: breathe, calm down, then decide. Three trusted contacts stay reachable, with calls capped at 3 minutes. Emergency numbers are never intercepted.

WHAT IT IS NOT

Not a spam blocker. It never touches incoming calls and never mutes anything. Not a parental control tool. It slows down one specific impulse: calling someone you may regret calling.

PRIVACY

No account, no ads, no analytics, no server. The app has no INTERNET permission at all, so everything stays in its private storage on the device.

Privacy policy: https://ajanedouby.github.io/instant-pause-privacy/

PERMISSIONS YOU WILL BE ASKED FOR (and why)

- Call redirection role — Android's own API to intercept an outgoing call before it connects.
- Device admin — only to make uninstalling take a few extra taps. No policy, no remote wipe.
- Phone permission (ANSWER_PHONE_CALLS) — only to hang up at 3:00 on the three trusted contacts.
- Full-screen intent — to show the pause screen over the dialer.

SETUP (2 minutes)

On first launch the app asks you to CREATE a protection code. There is no default PIN.

Rules: at least 6 characters, with at least one letter and one digit — for example: Pause2026.

You will only be asked for it again when weakening the protection.

HOW TO JOIN

1. Join the Google Group with the Google account signed in on your phone: https://groups.google.com/g/instant-pause-testers
2. Then open: https://play.google.com/apps/testing/com.instantpause.app
3. Tap "Accept", install, and keep the app installed for 14 days.
4. Optional feedback form: https://docs.google.com/forms/d/e/1FAIpQLSc3fFeY1_BNaLdHvBmEXmYVzBwi8sRxuHi9osoyC4xuxxZS3g/viewform

IMPORTANT

Join the group FIRST, then open the Play link. If you see "App not available", wait 5–10 minutes and reload — propagation is not instant.

Please stay opted in for 14 consecutive days. If you leave the test, your counter resets. If you were already part of the internal test for this app, leave it first, otherwise the closed test will not count.

Available worldwide (178 countries). Requires Android 10+.

IN RETURN

Reply with your testing link and I'll join your test and keep your app installed for 14 days. I won't ask for reviews — test builds can't receive public reviews anyway.

Thanks!
```

### Réponses types aux commentaires

**« Interested » / « Send me the link »**

```
Thanks! Join the group first with the Google account on your phone:
https://groups.google.com/g/instant-pause-testers

Then open https://play.google.com/apps/testing/com.instantpause.app and tap Accept.
If you get "App not available", wait 5–10 min and reload. Drop your link and I'll test yours.
```

**Question sur les permissions**

```
Fair question. Device admin is used only to slow down uninstall — no policy, no screen lock, no wipe. ANSWER_PHONE_CALLS is used only to hang up at 3:00 on the three contacts you allow. The app has no INTERNET permission, so nothing can leave the device. You can verify it with any manifest viewer.
```

**Quelqu'un partage son lien**

```
Joined and installed. I'll keep it for 14 days. Mine:
Group: https://groups.google.com/g/instant-pause-testers
Test: https://play.google.com/apps/testing/com.instantpause.app
```

**Mise à jour du compteur** (en commentaire sous son propre post, pas en nouveau post)

```
Update: X/12 testers so far, still need Y. Thanks to everyone who joined — please keep the app installed until <date>.
```

### Vérifications après publication

- Ouvrir le post en **navigation privée** : s'il n'apparaît pas, il a été filtré automatiquement
  (compte trop récent ou trop peu de karma). Écrire aux modérateurs, ne pas reposter.
- Vérifier le **flair** : sans lui, de nombreux posts sont supprimés par Automod.
- Répondre à chaque commentaire dans l'heure : c'est ce qui maintient le post en tête.
- Ne pas créer un second post pour « relancer » : éditer le premier.

---

## 11. « Sorry, this post was removed by Reddit's filters »

Ce message vient du **filtre anti-spam de Reddit**, pas d'une règle du subreddit. Il ne signifie
pas que le post est interdit : il est en attente dans la **file de modération** (modqueue), où un
modérateur peut l'approuver en un clic.

### Ce qui le déclenche

| Facteur | Situation typique |
|---|---|
| Karma très faible | 1 karma, quelques contributions |
| Plusieurs liens dans un premier post | 4 URL (groupe, Play, formulaire, confidentialité) |
| Compte peu actif | aucune participation récente |
| VPN / IP partagée | trafic considéré comme suspect |

L'âge du compte (2 ans) n'est pas le problème : c'est le **karma** et le **nombre de liens**.

### Ordre des actions

1. **Ne supprimez pas le post et ne le repostez pas.** Un nouveau post repart dans le filtre et
   augmente le score de spam du compte.
2. **Écrivez aux modérateurs** via le bouton **Message the mods** du sub (pas en chat privé) :

```
Hi mods,

My post was removed by Reddit's automatic filters (not by a visible rule):
<coller le lien du post>

It is a closed test for an Android app (Instant Pause, com.instantpause.app). I am looking for
12 testers for 14 days, and I am happy to test other developers' apps in return.

Could you approve it from the modqueue? I am also building karma so this does not happen again.

Thanks!
```

3. **Gagnez du karma pendant l'attente** : commentez de façon utile dans `r/AndroidClosedTesting`
   (les auteurs de tests s'entraident), `r/AndroidQuestions`, `r/androidapps`. Une dizaine de
   commentaires sincères suffisent.
4. **Après 48 h sans réponse** : repostez avec **moins de liens** — le groupe et le lien de test
   uniquement. Le formulaire et la politique de confidentialité peuvent aller dans le premier
   commentaire, ou être indiqués en texte.
5. **Postez entre 14 h et 17 h UTC**, en semaine : le sub est surtout actif aux États-Unis et en
   Inde. Un post à 8 h UTC passe inaperçu.



