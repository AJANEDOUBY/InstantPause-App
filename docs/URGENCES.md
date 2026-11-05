# Détection des numéros d'urgence — état réel

Question posée : l'app utilise-t-elle `libphonenumber` pour éviter une liste manuelle de
numéros d'urgence par pays ?

**Réponse courte : oui.** Mais avec deux limites qui méritent une correction, et un trou de
tests.

---

## 1. Ce qui est en place

Dépendance dans `app/build.gradle.kts` :

```kotlin
implementation("com.googlecode.libphonenumber:libphonenumber:9.0.41")
```

Dans `PhoneNumberNormalizer.kt` :

```kotlin
private val shortNumberInfo: ShortNumberInfo = ShortNumberInfo.getInstance()

override fun isEmergency(raw: String, regions: List<String>): Boolean {
    val cleaned = clean(raw) ?: return false
    if (cleaned in HARDCODED_EMERGENCY_NUMBERS) return true
    return (regions + SUPPORTED_REGIONS).distinct()
        .any { shortNumberInfo.isEmergencyNumber(cleaned, it) }
}
```

C'est bien la méthode officielle de Google, avec les métadonnées de tous les pays. Aucune liste
interminable n'est maintenue.

### Comment la région est déterminée

`resolveRegions()` prend, dans l'ordre : le pays du réseau, le pays de la SIM, filtrés par
`phoneUtil.supportedRegions`, puis la locale système, puis `MA` en dernier recours. Le service
de redirection passe ensuite `regions + ("MA", "FR")` à la détection.

Conséquence utile : un utilisateur en roaming est évalué avec **le pays du réseau** en plus de
celui de sa SIM. C'est le bon comportement.

### Le filet de sécurité manuel

Six numéros seulement, en dur :

```kotlin
private val HARDCODED_EMERGENCY_NUMBERS = setOf("112", "15", "17", "18", "19", "177")
```

`112` est le numéro d'urgence européen universel, joignable même sans SIM. Les autres sont les
numéros courts français. C'est un doublon assumé, pas une liste de pays.

---

## 2. Limite n°1 — le contrôle de la plateforme manque

`libphonenumber` embarque ses propres métadonnées. Android, lui, utilise **la configuration
opérateur** du téléphone, qui peut différer (numéros d'urgence locaux, mode urgence sans SIM,
pays détecté par le modem).

Depuis Android 10, une API publique donne cette vérité :

```kotlin
TelephonyManager.isEmergencyNumber(number)   // API 29+, aucune permission
```

Elle n'est pas utilisée. C'est une ceinture en plus de la bretelle, gratuite.

## 3. Limite n°2 — les numéros courts hors MA/FR

`isEmergency` teste `regions + MA + FR`. Si un numéro court est composé dans un pays dont la
région n'a pas été résolue — cas rare, mais réel sur un appareil sans SIM — il n'est comparé
qu'aux métadonnées de deux pays.

Or, dans le doute, **le sens de l'erreur compte** :

| Erreur | Effet | Gravité |
|---|---|---|
| Faux positif (numéro banal pris pour une urgence) | l'appel passe | bénin |
| Faux négatif (urgence prise pour un appel banal) | l'appel est **bloqué** | inacceptable |

Donc, pour un numéro **court** (≤ 6 chiffres), il faut comparer à **toutes** les régions connues
de `libphonenumber`, pas seulement aux deux pays du projet. Un faux positif coûte un appel
autorisé de trop. Un faux négatif coûte une vie.

## 4. Le trou de tests

Il n'existe **aucun test** sur `PhoneNumberNormalizer`. Les seuls tests d'urgence passent par
un faux analyseur (`ShouldBlockCallUseCaseTest`) qui répond sur un `Set<String>` — ils vérifient
l'ordre des décisions, pas la détection elle-même.

C'est pourtant la seule règle de l'app dont l'échec est irréversible.

---

## 5. Correction à embarquer en 1.5.0

### a. Double contrôle plateforme

```kotlin
override fun isEmergency(raw: String, regions: List<String>): Boolean {
    val cleaned = clean(raw) ?: return false

    // 1. Vérité de l'opérateur (API 29+), sans permission.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            context.getSystemService(TelephonyManager::class.java)
                ?.isEmergencyNumber(cleaned) == true
        }.getOrDefault(false).let { if (it) return true }
    }

    // 2. Filet manuel
    if (cleaned in HARDCODED_EMERGENCY_NUMBERS) return true

    // 3. Numéros courts : toutes les régions, le faux positif est préférable
    val short = cleaned.count { it.isDigit() } <= 6
    val scope = if (short) phoneUtil.supportedRegions + regions
                else (regions + SUPPORTED_REGIONS)
    return scope.distinct().any { shortNumberInfo.isEmergencyNumber(cleaned, it) }
}
```

`phoneUtil.supportedRegions` est un `Set<String>` de tous les pays connus : environ 250
comparaisons sur un numéro court, coût négligeable pour un appel.

### b. Test unitaire, sans téléphone

`libphonenumber` est une bibliothèque JVM : elle s'exécute dans `src/test`, sans émulateur.
Table à couvrir au minimum :

| Numéro | Région | Attendu |
|---|---|---|
| `112` | FR, MA, US, GB | urgence |
| `15` | FR | urgence |
| `17`, `18` | FR | urgence |
| `911` | US | urgence |
| `999` | GB | urgence |
| `19` | MA | urgence |
| `114` | FR | urgence (SMS pour sourds) |
| `+212661132713` | MA | **pas** urgence |
| `0612345678` | FR | **pas** urgence |

Les deux dernières lignes sont les plus importantes : elles vérifient que la détection ne
déborde pas sur des numéros ordinaires. Un faux positif permanent autoriserait tous les appels
et viderait le produit.

### c. Ne rien changer maintenant

La 14 est en attente d'examen. Ces modifications sont dans `PhoneNumberNormalizer`, donc dans
le code livré : elles imposent une 15. À faire après le test fermé, avec le reste de la 1.5.0.
