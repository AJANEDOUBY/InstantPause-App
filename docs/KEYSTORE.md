# Keystore de signature - procédure et rotation

Tout ce qui concerne la clé de téléversement d'Instant Pause (`com.instantpause.app`) :
où elle est, comment la sauvegarder, comment changer son mot de passe, et ce qu'il faut
vérifier après.

> **Aucun mot de passe n'est écrit dans ce fichier, ni dans aucun fichier versionné.**

---

## 1. Où sont les choses

| Élément | Emplacement | Versionné ? |
|---|---|---|
| Keystore | `keystore/cooldown-release.jks` (PKCS12, 4 396 octets) | Non (`.gitignore` → `/keystore/`) |
| Alias | `cooldown` | - |
| Identifiants | `local.properties` → `signing.storePassword`, `signing.keyPassword` | Non (`.gitignore` → `local.properties`) |
| Lecture côté Gradle | `app/build.gradle.kts` → `signingValue(...)` | Oui (sans valeur) |
| Certificat | SHA-256 `4F:14:D6:6F:67:70:61:BC:C9:37:38:CF:62:51:B8:7B:DA:57:92:D0:F4:28:9E:27:1E:3F:D8:1B:73:1A:F0:F5` | - |

Gradle accepte deux sources, dans cet ordre : la clé `signing.*` de `local.properties`, puis la
variable d'environnement équivalente (`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_PASSWORD`,
`SIGNING_KEY_ALIAS`, `SIGNING_STORE_FILE`). Pratique pour un CI, inutile en local.

`keytool` **n'est pas dans le PATH** sur cette machine. Utiliser celui d'Android Studio :

```
C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe
```

---

## 2. Ce qu'il faut retenir sur le format PKCS12

Le fichier commence par les octets `30 82` : c'est du **PKCS12**, pas du JKS.

Conséquence : **il n'y a qu'un seul mot de passe.** Celui du magasin protège aussi la clé
privée. La commande `-keypasswd` est donc inutile :

```
Warning: Different store and key passwords not supported for PKCS12 KeyStores.
```

`-storepasswd` change les deux à la fois. C'est aussi pour cela que
`signing.keyPassword` doit rester **égal** à `signing.storePassword` dans `local.properties` :
si la valeur est présente mais périmée, la signature échoue.

---

## 3. Sauvegarder avant toute manipulation

```powershell
Copy-Item keystore\cooldown-release.jks "keystore\cooldown-release.backup-$(Get-Date -Format yyyyMMdd-HHmmss).jks"
```

Puis déplacer la copie **hors du projet** (archive chiffrée, support hors ligne). Un keystore
perdu sans sauvegarde signifie : plus aucune mise à jour possible de l'application avec la même
clé de téléversement.

---

## 4. Changer le mot de passe

### 4.1 Relever l'empreinte du certificat avant

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore keystore\cooldown-release.jks
```

Noter la ligne `SHA256:`. Elle doit être **identique** après l'opération : le mot de passe change,
le certificat ne change pas.

### 4.2 Lancer le changement

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -storepasswd -keystore keystore\cooldown-release.jks
```

Trois invites, aucune ne s'affiche à l'écran :

```
Enter keystore password:          <- ancien mot de passe
New keystore password:            <- nouveau
Re-enter new keystore password:    <- nouveau, à l'identique
```

**Ne jamais passer le mot de passe en argument** (`-storepass xxx`, `-new yyy`) : il resterait dans
l'historique PowerShell et dans la liste des processus. Laisser keytool le demander.

### 4.3 Mettre à jour `local.properties`

```properties
signing.storeFile=keystore/cooldown-release.jks
signing.storePassword=LE_NOUVEAU_MOT_DE_PASSE
signing.keyAlias=cooldown
signing.keyPassword=LE_NOUVEAU_MOT_DE_PASSE
```

Les deux valeurs de mot de passe doivent être identiques (voir section 2).

### 4.4 Vérifier

```powershell
# Le nouveau mot de passe est accepté
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore keystore\cooldown-release.jks

# Le build signe
.\gradlew.bat bundleRelease

# La signature du bundle est valide
& "C:\Program Files\Android\Android Studio\jbr\bin\jarsigner.exe" -verify -certs app\build\outputs\bundle\release\app-release.aab
```

Attendu : `jar verified.` L'avertissement `certificate chain is invalid` est **normal** pour un
certificat auto-signé Android.

---

## 5. Rotation après une fuite

Procédure appliquée le 7 novembre 2026, après la découverte du mot de passe en clair au commit
`2fe34c4` de l'historique Git (voir section 7).

| # | Action | Fait |
|---|---|---|
| 1 | Sauvegarde du keystore | Oui |
| 2 | `keytool -storepasswd` avec un mot de passe long et unique | Oui |
| 3 | `local.properties` mis à jour (les deux lignes) | Oui |
| 4 | Ancien mot de passe vérifié comme **refusé** | Oui |
| 5 | Empreinte SHA-256 du certificat inchangée | Oui |
| 6 | `bundleRelease` + `jarsigner -verify` | Oui |
| 7 | Sauvegarde déplacée hors ligne puis supprimée du projet | À faire |
| 8 | Historique du dépôt public réécrit (`filter-repo` + force-push) | Oui |
| 9 | Purge des vues en cache demandée à GitHub Support | À faire |

Une rotation **ne nécessite aucune démarche Play Console** : Play App Signing conserve la clé de
signature de l'application, on ne change que le mot de passe local de la clé de téléversement.

---

## 6. Ce qui ne change pas lors d'une rotation

- Le **certificat** et son empreinte SHA-256 : Google reconnaît la même clé de téléversement.
- Les **bundles déjà publiés** : ils restent valides, rien à re-téléverser.
- L'**alias** (`cooldown`) et le chemin du fichier.
- Les **secrets de CI** : il n'y a pas de dossier `.github` dans le projet. Si un CI est ajouté un
  jour, il faudra y reporter `SIGNING_STORE_PASSWORD` et `SIGNING_KEY_PASSWORD`.

---

## 7. Le dépôt public `InstantPause-App`

`AJANEDOUBY/CoolDown` et `AJANEDOUBY/InstantPause-App` sont **le même dépôt** : l'ancien nom
redirige vers le nouveau. Il est **public**.

Le mot de passe y était exposé au commit `2fe34c4` (`storePassword` et `keyPassword` en clair, 28
caractères), dans l'historique complet du projet. Le fichier `.jks` et `local.properties` n'ont
**jamais** été commités : le mot de passe seul ne permettait pas de signer.

### Ce qui a été fait le 8 novembre 2026

| Étape | Résultat |
|---|---|
| Rotation du mot de passe (7 nov.) | ancien mot de passe refusé par keytool |
| Réécriture (`git filter-repo --replace-text`) | les 2 valeurs remplacées par `REDACTED_KEYSTORE_PASSWORD` |
| Vérification | 0 occurrence du secret dans les 64 commits et les 13 tags |
| Intégrité | nombre de commits et de tags identiques, arbre de HEAD inchangé |
| Force-push | `3b5e292...24b59b1`, tous les SHAs ont changé |
| Visibilité du dépôt | inchangée : toujours public |

### Limite à connaître

GitHub conserve les anciens commits accessibles par leur SHA, et sert encore les vues en cache.
Constaté après le push : `raw.githubusercontent.com/.../2fe34c4/app/build.gradle.kts` renvoie
toujours `200` avec le mot de passe en clair.

Pour les faire disparaître, il faut ouvrir un ticket chez **GitHub Support** :
<https://support.github.com/contact>. Se connecter avec le compte `AJANEDOUBY`, puis choisir le
sujet lié au dépôt et à la suppression de données sensibles. Joindre :

- le nom du dépôt : `AJANEDOUBY/InstantPause-App` ;
- le nombre de pull requests affectées : **0** (aucune PR, aucun fork) ;
- le premier commit modifié : `7b505debbad1a4b379794936712d6ae0b5b324f7` (réécriture de `2fe34c4`) ;
- l'absence d'objets LFS orphelins.

Message type :

```
Hello,

I need the cached views and unreachable objects purged for AJANEDOUBY/InstantPause-App.

A keystore password was committed by mistake in app/build.gradle.kts (storePassword and
keyPassword) and I rewrote the history with git filter-repo --replace-text, then force-pushed
refs/heads/main and all tags.

Details from your documentation:
- Repository: AJANEDOUBY/InstantPause-App
- Affected pull requests: 0 (no pull requests, no forks)
- First changed commit: 7b505debbad1a4b379794936712d6ae0b5b324f7 (rewritten from 2fe34c4)
- LFS objects orphaned: none

The old commits are still reachable by SHA, and
https://raw.githubusercontent.com/AJANEDOUBY/InstantPause-App/2fe34c4/app/build.gradle.kts still
serves the file containing the password.

I have already rotated the password, so it is no longer valid. I am asking for the cached views
and old objects to be removed anyway.

Thank you.
```

⚠️ La documentation GitHub précise que le support n'intervient que si le risque **ne peut pas**
être écarté par une rotation du secret. La rotation ayant déjà été faite, il est possible que la
demande soit refusée. Dans ce cas, la protection repose entièrement sur le nouveau mot de passe.

Tant que la purge n'est pas faite, considérer l'ancien mot de passe comme public (il l'est), et ne
jamais le réutiliser.

### Conséquence de la réécriture

Tous les commits ont de nouveaux SHAs : les clones existants doivent être refaits, et les anciens
liens vers des commits précis ne résolvent plus. Le contenu, lui, est identique.

---

## 8. Erreurs fréquentes

| Message | Cause | Solution |
|---|---|---|
| `keystore password was incorrect` | Ancien mot de passe erroné | Rien n'a été modifié, relancer |
| `Alias <name> does not exist` | Mauvais alias | L'alias est `cooldown` |
| `Warning: Different store and key passwords not supported for PKCS12` | Usage de `-keypasswd` | Normal, inutile : `-storepasswd` suffit |
| `Keystore was tampered with, or password was incorrect` | Mauvais fichier ou mot de passe | Vérifier le chemin, puis restaurer la sauvegarde |
| Build signé mais Play refuse le bundle | `signing.keyPassword` périmé | Aligner les deux valeurs sur le nouveau mot de passe |
| `jarsigner: unable to open jar file` | Chemin de l'AAB erroné | `app\build\outputs\bundle\release\app-release.aab` |

---

## 9. Règles d'hygiène

- Le mot de passe vit dans `local.properties` et dans un gestionnaire de mots de passe. **Nulle
  part ailleurs.**
- Ne jamais le coller dans un ticket, un chat, une capture d'écran ou un fichier de documentation.
  S'il a circulé, refaire une rotation.
- Le fichier `keystore/KEYSTORE-INFO.txt` ne doit contenir **aucun** mot de passe : uniquement
  l'alias, la date de création et la référence de la sauvegarde hors ligne (gestionnaire de mots de
  passe).
- Ne jamais versionner `keystore/` ni `local.properties` : vérifier avec `git status` avant chaque
  commit destiné à un dépôt public.
- Un mot de passe par usage : celui du keystore ne doit être réutilisé nulle part (GitHub, e-mail,
  Play Console).
- Conserver **deux** sauvegardes du `.jks` hors ligne, dans deux endroits distincts, chacune
  protégée par un mot de passe à jour.
