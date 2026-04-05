# Rapport de Génie Logiciel — Partie 2 : Amélioration du projet Gson

---

**Étudiant :** Metogbe Espera YEBE
**Numéro étudiant :** 42321627
**Cours :** Génie Logiciel - GL
**Encadrant TP :** Clotilde TOULLEC.
**Date de rendu :** 5 avril 2026
**Dépôt Git :** https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl

---

## Table des matières

1. [Introduction](#1-introduction)
2. [Présentation du projet Gson](#2-présentation-du-projet-gson)
3. [Modifications apportées](#3-modifications-apportées)
   - [Commit 1 — Renommage de nom de classe, methodes, variable dans LinkedTreeMap](#commit-1---renommage-de-LinkedTreeMap)
   - [Commit 2 — Nombres magiques dans JsonReader](#commit-2--nombres-magiques-dans-jsonreader)
   - [Commit 3 — Nombres magiques dans JsonWriter](#commit-3--nombres-magiques-dans-JsonWriter)
   - [Commit 4 — Réorganisation de la structure de GsonBuilder](#commit---réorganisation-de-la-structure-de-gsonBuilder)
   - [Commit 5 — Reduction de la complexité de methodes dans TypeAdapters](#commit-5--Reduction-de-la-complexité-de-methodes-dans-TypeAdapters)
   - [Commit 6 — Decomposition de nextString](#commit-6--Decomposition-de-nextString)
   - [Commit 6 — Décomposition de la méthode doPeek](#commit-6--décomposition-de-la-méthode-dopeek)
   - [Commit 7 — Refactoring de duplication de code dans JsonReader](#commit-7--réorganisation-de-la-structure-de-jsonwriter)
   - [Commit 8 — Renommage des paramètres dans GsonBuilder](#commit-8--renommage-des-paramètres-dans-gsonbuilder)
   - [Commit 9 — SuperClasse AbstractSqlDateTypeAdapter ](#commit-9--extraction-de-classifypeeked-number)
   - [Commit 10 — Tests pour AbstractSqlDateTypeAdapter](#commit-10--séparation-lectureétat-dans-nextstring)
   - [Commit 11 — Tests pour AbstractSqlDateTypeAdapter](#commit-13--tests-pour-abstractsqldatetypeadapter)
   - [Commit 12 — Décomposition de la god class TypeAdapters](#commit-14--décomposition-de-la-god-class-typeadapters)

4. [Tentative ratée documentée](#4-tentative-ratée-documentée)
5. [Ce que j'ai appris](#5-ce-que-jai-appris)

---

## 1. Introduction

Ce rapport présente les améliorations apportées au projet open-source **Gson** (bibliothèque Java de Google pour la sérialisation/désérialisation JSON) dans le cadre de la partie 2 du projet de Génie Logiciel. L'objectif est d'identifier des défauts de qualité dans le code existant et d'y apporter des corrections documentées, en appliquant les bonnes pratiques étudiées en cours.

Les modifications couvrent plusieurs catégories d'améliorations : élimination des nombres magiques, renommage de variables ambiguës, réduction de la duplication de code, décomposition de méthodes complexes et décomposition de classes trop volumineuses.

Chaque modification correspond à un commit distinct dans le dépôt GitLab, ce qui permet de tracer précisément l'historique des améliorations.

---

## 2. Présentation du projet Gson

**Gson** est une bibliothèque Java open-source développée par Google, disponible à l'adresse [https://github.com/google/gson](https://github.com/google/gson). Elle permet de convertir des objets Java en représentation JSON et inversement.

Les packages principaux analysés dans ce rapport sont :

- `com.google.gson.stream` — contient `JsonReader` et `JsonWriter`, les classes bas niveau de lecture/écriture JSON
- `com.google.gson.internal` — utilitaires internes (`JavaVersion`, `Primitives`)
- `com.google.gson.internal.sql` — adaptateurs pour les types `java.sql`
- `com.google.gson.internal.bind` — adaptateurs de types généraux (`TypeAdapters`)
- `com.google.gson` — API publique (`GsonBuilder`)

---

## 3. Modifications apportées

---

### Commit 1 — Nombres magiques dans JsonWriter

**Lien des commits :** `https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/1818505c224fb2b407f3a324051a5d54dad72577`
`https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/28b6d13ea538494d676c480b4886f34789aa5eeb`
`https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/66fe5afda1a366363c11931e69a698e5c7091aab`

**Modification:**
Il n'a pas de modification particulière ce sont juste des renommages de classe, methodes et de variable tous dans une
même classe LinkedTreeMap renommée ModifyClass. La methode find est renommée findModify et la variable node de la méthode remove en 
nodeKv.

---
### Commit 2 — Nombres magiques dans JsonReader 

**Lien du commit :** `https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/05a31f10168a254785366f5e26af74593009aeff`

#### Problème

Même problème que dans `JsonWriter`, mais dans `gson/src/main/java/com/google/gson/stream/JsonReader.java`. La valeur `32` apparaissait à **trois endroits distincts** pour initialiser trois tableaux liés :

```java
private int[] stack = new int[32];
private String[] pathNames = new String[32];
private int[] pathIndices = new int[32];
```

Trois occurrences séparées signifient trois points de modification potentiels — un risque d'oubli élevé.

#### Contexte environnant

`JsonReader` est le pendant de `JsonWriter` pour la lecture. Le `stack`, `pathNames` et `pathIndices` fonctionnent ensemble pour tracer le chemin courant dans le JSON (utilisé dans les messages d'erreur via `getPath()`). Ces tableaux sont **redimensionnés ensemble** dans la même méthode interne, ce qui confirme leur cohésion.

#### Correction

```java
private static final int INITIAL_STACK_CAPACITY = 32;

private int[] stack = new int[INITIAL_STACK_CAPACITY];
private String[] pathNames = new String[INITIAL_STACK_CAPACITY];
private int[] pathIndices = new int[INITIAL_STACK_CAPACITY];
```

#### Pourquoi c'est mieux

- La valeur `32` n'apparaît plus qu'une seule fois
- `JsonReader` et `JsonWriter` utilisent désormais le même nom de constante `INITIAL_STACK_CAPACITY`, rendant explicite le fait que les deux classes partagent la même logique de stack
- La cohésion entre les trois tableaux est mise en évidence par la constante partagée

---
### Commit 3 — Nombres magiques dans JsonWriter

**Lien du commit :**`https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/8fdffd2a1d6b6282f51a7028f105a982ba1903e7`

#### Problème

Dans `gson/src/main/java/com/google/gson/stream/JsonWriter.java`, deux valeurs numériques apparaissaient directement dans le code sans explication :

```java
private static final String[] REPLACEMENT_CHARS;
static {
    REPLACEMENT_CHARS = new String[128]; // pourquoi 128 ?
    ...
}
private int[] stack = new int[32]; // pourquoi 32 ?
// ...
if (c < 128) { // même valeur répétée
```

Ces **nombres magiques** nuisent à la lisibilité : un lecteur ne peut pas comprendre leur signification sans recherche externe. De plus, la valeur `128` apparaissait à deux endroits distincts, créant un risque d'incohérence en cas de modification.

#### Contexte environnant

`JsonWriter` est la classe centrale d'écriture JSON du projet. Le tableau `REPLACEMENT_CHARS` est utilisé dans la méthode `string()` pour échapper les caractères spéciaux lors de la sérialisation. Le champ `stack` gère la profondeur d'imbrication des objets et tableaux JSON.

#### Correction

Ajout de deux constantes nommées :

```java
private static final int ASCII_REPLACEMENT_LENGTH = 128;
private static final int INITIAL_STACK_CAPACITY = 32;
```

Puis utilisation systématique de ces constantes à la place des valeurs brutes.

#### Pourquoi c'est mieux

- `ASCII_REPLACEMENT_LENGTH = 128` documente que cette limite correspond exactement à la taille de la table des caractères ASCII standard
- `INITIAL_STACK_CAPACITY = 32` exprime l'intention : une capacité initiale pour 32 niveaux d'imbrication JSON
- La valeur `128` n'apparaît plus qu'en **un seul endroit** — une modification future est sûre et cohérente
- Le code est **auto-documenté** sans avoir recours à des commentaires supplémentaires

---

### Commit 4 — Réorganisation de la structure de GsonBuilder

**Lien du commit :** `https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/8dde1d4da4f6dee9d9121311bab229abc7be2a8c`

#### Problème

Dans `gson/src/main/java/com/google/gson/GsonBuilder.java`, certaines méthodes utilitaires privées statiques
étaient placées au milieu des méthodes de l'API publique. Par exemple, `checkDateFormatStyle` et
`hasNonOverridableAdapter` apparaissaient entre des méthodes de configuration comme
`registerTypeAdapter(...)`, `registerTypeAdapterFactory(...)` ou `registerTypeHierarchyAdapter(...)`.

Cette organisation ne changeait pas le comportement, mais elle nuisait à la lisibilité :

- un lecteur qui parcourt l'API publique de `GsonBuilder` était interrompu par des détails
  d'implémentation
- la distinction entre interface publique et logique interne était moins nette
- la structure générale de la classe était moins cohérente

#### Correction

Le refactoring a consisté à **déplacer** les méthodes utilitaires privées statiques en bas de la classe, à
proximité des autres helpers internes déjà utilisés par `createFactories`, `addUserDefinedAdapters` ou
`addDateTypeAdapters`.

```java
private static int checkDateFormatStyle(int style) { ... }

private static boolean hasNonOverridableAdapter(Type type) { ... }
```

Après réorganisation, ces méthodes ne sont plus intercalées dans le flux des méthodes publiques : elles sont
regroupées avec les autres méthodes privées internes.

#### Pourquoi c'est mieux

- Les méthodes publiques de `GsonBuilder` se lisent désormais d'un bloc, ce qui facilite la compréhension de
  l'API
- Les méthodes privées utilitaires sont regroupées selon leur rôle, ce qui rend la structure de la classe
  plus cohérente
- Le refactoring est **sans impact fonctionnel** : seules la position et l'organisation du code changent,
  pas la logique métier
- Cette réorganisation prépare mieux les futurs refactorings en séparant plus clairement l'API exposée des
  mécanismes internes

---
### Commit 5 — Reduction de la complexité de methodes dans TypeAdapters
#### Commit 5-1 — Réduction de la complexité de `BIT_SET.read(in)` dans `TypeAdapters`

**Lien du commit :** `https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/57e6f33fa3e8036fde54e7e9da4312c091e98e4f`

#### Problème

Dans `gson/src/main/java/com/google/gson/internal/bind/TypeAdapters.java`, la méthode `read()` de
l’adaptateur `BIT_SET` contenait trop de logique concentrée au même endroit. À l’intérieur de la boucle
de lecture du tableau JSON, elle gérait à la fois :

- l’identification du type de token (`NUMBER`, `STRING`, `BOOLEAN`, etc.)
- la conversion des valeurs numériques `0` et `1` en booléens
- la gestion des erreurs de format
- la mise à jour du `BitSet`

Le cœur de la méthode contenait un `switch` imbriqué avec plusieurs branches, ce qui augmentait sa
complexité et nuisait à sa lisibilité.

#### Contexte environnant

`TypeAdapters` est une classe centrale et volumineuse du projet Gson. L’adaptateur `BIT_SET` sert à lire un
tableau JSON et à construire un objet `BitSet`. Les éléments acceptés peuvent être des booléens ou des
valeurs numériques/chaînes représentant `0` ou `1`. Le comportement attendu ne devait pas changer ; seul le
découpage interne du code était visé.

#### Correction

La logique a été extraite dans deux méthodes privées utilitaires :

- `readBitSetValue(JsonReader in, JsonToken tokenType)` : choisit le traitement selon le type du token
- `readBitSetNumericValue(JsonReader in)` : gère le cas particulier des valeurs numériques ou textuelles
  devant valoir `0` ou `1`

```java
private static boolean readBitSetValue(JsonReader in, JsonToken tokenType) throws IOException {
    switch (tokenType) {
        case NUMBER:
        case STRING:
            return readBitSetNumericValue(in);
        case BOOLEAN:
            return in.nextBoolean();
        default:
            throw new JsonSyntaxException(...);
    }
}
```

```java
private static boolean readBitSetNumericValue(JsonReader in) throws IOException {
    int intValue = in.nextInt();
    if (intValue == 0 || intValue == 1) {
        return intValue == 1;
    }
    throw new JsonSyntaxException(...);
}
```

#### Pourquoi c'est mieux

- La méthode `BIT_SET.read(in)` est plus courte et plus facile à suivre
- Le traitement des cas numériques est isolé dans une méthode dédiée, donc plus simple à relire et à tester
- La séparation entre sélection du type de token et validation de la valeur réduit la complexité locale
- Le comportement est inchangé, mais l’intention du code est plus claire

---

#### Commit 5-2 — Réduction de la complexité de `LOCALE.read(in)` dans `TypeAdapters`

**Lien du commit :** `https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/176160907ae1c1f6cb978fb4104b30d5258a49b5`

#### Problème

Dans `gson/src/main/java/com/google/gson/internal/bind/TypeAdapters.java`, la méthode `read()` de
l’adaptateur `LOCALE` contenait à la fois le découpage de la chaîne lue, la récupération des différents
tokens (`language`, `country`, `variant`) et la construction finale de l’objet `Locale`.

Même si la logique n’était pas très longue, elle mélangeait plusieurs étapes dans une seule méthode, ce qui
rendait la lecture moins fluide.

#### Contexte environnant

`TypeAdapters` est une classe centrale du projet Gson qui regroupe de nombreux adaptateurs. L’adaptateur
`LOCALE` convertit une chaîne comme `"fr_FR"` ou `"fr_FR_EURO"` en objet `Locale`. Le comportement attendu
devait rester identique ; l’objectif du commit était uniquement d’améliorer le découpage interne.

#### Correction

La logique a été déplacée dans trois méthodes privées :

- `parseLocale(String locale)` : orchestre le parsing de la chaîne
- `nextTokenOrNull(StringTokenizer tokenizer)` : récupère un token s’il existe
- `createLocale(String language, String country, String variant)` : construit l’objet `Locale` final

```java
private static Locale parseLocale(String locale) {
    StringTokenizer tokenizer = new StringTokenizer(locale, "_");
    String language = nextTokenOrNull(tokenizer);
    String country = nextTokenOrNull(tokenizer);
    String variant = nextTokenOrNull(tokenizer);
    return createLocale(language, country, variant);
}
```

```java
return parseLocale(in.nextString());
```

#### Pourquoi c'est mieux

- La méthode `LOCALE.read(in)` devient plus courte et plus lisible
- Le découpage entre extraction des tokens et création de l’objet `Locale` est plus clair
- Les nouvelles méthodes privées portent des noms explicites, ce qui rend l’intention du code immédiate
- Le comportement ne change pas, mais la maintenance devient plus simple

---


### Commit 6 —  Decomposition de nextString

**Lien du commit :** `https://gitlab.univ-lille.fr/kibalo-beni.tagba.etu/kibalo_beni_tagba_gl_project/-/commit/cb792d9d91aa130284574d2e836ec19825ed39df`

#### Problème

Dans `gson/src/main/java/com/google/gson/internal/JavaVersion.java`, deux nombres magiques (-1 et 6) apparaissaient dans les méthodes de parsing de version Java :

```java
if (version == -1) { // que signifie -1 ici ?
    return 6; // pourquoi 6 ?
}
```

La valeur `-1` était utilisée comme code d'erreur à plusieurs endroits sans être nommée. La valeur `6` représentait la version Java minimale supportée mais n'était pas documentée dans le code.

#### Contexte environnant

`JavaVersion` est une classe utilitaire appelée dans `ReflectionAccessFilterHelper` pour adapter le comportement selon la version de la JVM courante. Les constantes sont `private` — elles n'affectent aucune API publique.

#### Correction

```java
private static final int UNKNOWN_VERSION = -1;
private static final int MINIMUM_SUPPORTED_JAVA_VERSION = 6;
```

#### Pourquoi c'est mieux

- `UNKNOWN_VERSION` documente explicitement la sémantique de la valeur sentinelle `-1`
- `MINIMUM_SUPPORTED_JAVA_VERSION` exprime l'intention métier derrière la valeur `6`
- La méthode `parseMajorJavaVersion` se lit désormais comme une phrase : « si inconnue après parsing pointé, essayer l'extraction ; si toujours inconnue, retourner la version minimale supportée »

---

### Commit 6 — Décomposition de la méthode doPeek

**Lien du commit :** `https://gitlab.univ-lille.fr/kibalo-beni.tagba.etu/kibalo_beni_tagba_gl_project/-/commit/851ae65c9f53dc13b622535c2be0a740448bf658`

#### Problème

La méthode `doPeek()` dans `JsonReader.java` était une méthode monolithique de plus de 100 lignes gérant à la fois l'état des tableaux, l'état des objets, l'état du document et la lecture des valeurs littérales. Sa complexité cyclomatique était très élevée, rendant la méthode difficile à lire, tester et maintenir.

#### Contexte environnant

`doPeek()` est la méthode centrale du parsing dans `JsonReader` — elle est appelée par toutes les méthodes publiques (`beginArray`, `beginObject`, `nextString`, etc.). L'extraction est purement interne (méthodes `private`), l'API publique est inchangée.

#### Correction

Extraction de deux sous-méthodes privées depuis `doPeek()` :

- `peekInArray(int peekStack)` — gère la logique propre aux tableaux JSON
- `peekInObject(int peekStack)` — gère la logique propre aux objets JSON

`doPeek()` passe d'environ 110 lignes à environ 50, délégant aux deux nouvelles méthodes.

#### Pourquoi c'est mieux

- Chaque méthode a une **responsabilité unique** et claire
- La complexité cyclomatique de `doPeek` est significativement réduite
- `peekInArray` et `peekInObject` sont des unités potentiellement testables indépendamment
- Un lecteur peut comprendre `doPeek` sans avoir à parcourir 100 lignes de cas imbriqués

---

### Commit 7 — Réorganisation de la structure de JsonWriter

**Lien du commit :** `https://gitlab.univ-lille.fr/kibalo-beni.tagba.etu/kibalo_beni_tagba_gl_project/-/commit/1acfd96c4fe94e310390565dad9b8d3700adeead`

#### Problème

Dans `JsonWriter.java`, les méthodes privées utilitaires (`openScope`, `closeScope`, `push`, `peek`, `replaceTop`, `writeDeferredName`) étaient intercalées **entre** des méthodes publiques. Un lecteur parcourant l'API publique tombait sur des détails d'implémentation interne au milieu de méthodes comme `beginArray`, `endObject`, `name`.

#### Contexte environnant

La convention Java standard (et les recommandations de lisibilité) préconise de regrouper les méthodes publiques ensemble, les méthodes privées en dessous. Cette organisation permet à un lecteur de l'API de trouver immédiatement ce qu'il peut appeler, sans être distrait par les détails internes.

#### Correction

Déplacement de toutes les méthodes privées après la méthode `close()` : l'ordre devient méthodes publiques → méthodes privées → méthodes utilitaires internes.

#### Pourquoi c'est mieux

- Lecture de l'API publique sans interruption par des détails internes
- Respect de la convention Java standard d'organisation des classes
- Cohérence facilitant la revue de code

---

### Commit 8 — Renommage des paramètres dans GsonBuilder

**Lien du commit :** `https://gitlab.univ-lille.fr/kibalo-beni.tagba.etu/kibalo_beni_tagba_gl_project/-/commit/f4a54a2e061f4d0ff80740a778edf3229fd6a611`

#### Problème

Dans `GsonBuilder.java`, les méthodes `registerTypeAdapter` et `registerTypeHierarchyAdapter` avaient un paramètre nommé `typeAdapter` de type `Object` :

```java
public GsonBuilder registerTypeAdapter(Type type, Object typeAdapter) {
    // en réalité accepte: JsonSerializer, JsonDeserializer, InstanceCreator, TypeAdapter
    Preconditions.checkArgument(
        typeAdapter instanceof JsonSerializer<?> || ...);
```

Le nom `typeAdapter` ne reflète pas que ce paramètre accepte plusieurs types distincts. Un développeur qui lit la signature ne comprend pas immédiatement ce que l'on peut passer.

#### Contexte environnant

Ces méthodes font partie de l'API publique de `GsonBuilder`, utilisées par les développeurs pour enregistrer des adaptateurs personnalisés. La clarté de leur signature est donc particulièrement importante.

#### Correction

```java
public GsonBuilder registerTypeAdapter(Type type, Object typeAdapterOrFactory)
public GsonBuilder registerTypeHierarchyAdapter(Class<?> baseType, Object typeAdapterOrFactory)
```

#### Pourquoi c'est mieux

- `typeAdapterOrFactory` indique explicitement que le paramètre accepte plusieurs types d'objets
- Le nom est cohérent avec la vérification `instanceof` qui suit immédiatement — le code se lit comme une phrase
- Les deux méthodes utilisent le même nom pour le même concept

---

### Commit 9 — Extraction de classifyPeekedNumber

**Lien du commit :** `https://gitlab.univ-lille.fr/kibalo-beni.tagba.etu/kibalo_beni_tagba_gl_project/-/commit/9aa0a06a448daf47a21341214ff55f1ff954f4ee`

#### Problème

La méthode `peekNumber()` dans `JsonReader.java` (environ 100 lignes) combinait deux responsabilités distinctes : la **boucle de parsing** caractère par caractère, et la **classification finale** du nombre parsé (long ou nombre flottant). La partie finale de classification (environ 15 lignes) rendait la méthode plus longue que nécessaire.

#### Contexte environnant

`peekNumber()` est une méthode de bas niveau du parsing, appelée depuis `doPeek()`. Elle est critique pour les performances — toute modification doit préserver exactement le comportement observable.

#### Correction

Extraction de la logique de classification en méthode séparée :

```java
private int classifyPeekedNumber(int last, boolean fitsInLong, long value, boolean negative, int length)
```

`peekNumber()` se termine désormais par un simple appel :

```java
return classifyPeekedNumber(last, fitsInLong, value, negative, i);
```

#### Pourquoi c'est mieux

- `peekNumber` est plus courte et se concentre sur le parsing de la séquence de caractères
- `classifyPeekedNumber` peut être lue et comprise indépendamment
- La séparation des responsabilités est respectée : une méthode parse, l'autre classifie

---

### Commit 10 — Séparation lecture/état dans nextString

**Lien du commit :** `https://gitlab.univ-lille.fr/kibalo-beni.tagba.etu/kibalo_beni_tagba_gl_project/-/commit/db0ec81f1116691daf54822c39d70245bf8ce66a`

#### Problème

La méthode `nextString()` dans `JsonReader.java` effectuait simultanément **deux responsabilités** :
1. Lire la valeur de la chaîne (selon le type de token peeked)
2. Mettre à jour l'état interne (`peeked = PEEKED_NONE` et `pathIndices[stackSize - 1]++`)

Cette combinaison rendait difficile la compréhension de quelle partie lit et quelle partie modifie l'état.

#### Contexte environnant

`nextString()` est une méthode publique de `JsonReader`, appelée directement par les utilisateurs et par d'autres méthodes internes. Séparer la lecture de la mise à jour d'état suit le principe de responsabilité unique.

#### Correction

Extraction de la lecture dans une méthode privée :

```java
private String readPeekedString(int p) throws IOException { ... }

public String nextString() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    String result = readPeekedString(p);   // lecture
    peeked = PEEKED_NONE;                  // mise à jour état
    pathIndices[stackSize - 1]++;
    return result;
}
```

#### Pourquoi c'est mieux

- `nextString` est désormais lisible en 5 lignes : lire, mettre à jour l'état, retourner
- `readPeekedString` encapsule proprement la logique de dispatch selon le type de token
- Le principe de responsabilité unique est respecté

---

### Commit 11 — Remplacement du code sentinelle par OptionalInt dans JavaVersion

**Lien du commit :** `https://gitlab.univ-lille.fr/kibalo-beni.tagba.etu/kibalo_beni_tagba_gl_project/-/commit/08297cb783bc89611df80af6379fc6c1de0c4c3e`

#### Problème

Après le commit 5, même avec la constante `UNKNOWN_VERSION = -1`, l'utilisation d'une valeur entière comme code d'erreur reste une **approche fragile**. Un entier négatif utilisé comme sentinelle est une convention implicite qui ne tire pas parti du système de types Java. Si une autre méthode retourne -1 pour une raison différente, la confusion est possible.

#### Contexte environnant

Java 8 a introduit `OptionalInt` précisément pour remplacer les patterns `retourner -1 en cas d'absence de valeur`. C'est la façon idiomatique moderne d'exprimer « une valeur qui peut être absente ».

#### Correction

Les méthodes privées `parseDotted` et `extractBeginningInt` retournent désormais `OptionalInt` au lieu d'`int` :

```java
private static OptionalInt parseDotted(String javaVersion) {
    try { ... return OptionalInt.of(version); }
    catch (NumberFormatException e) { return OptionalInt.empty(); }
}
```

`parseMajorJavaVersion` utilise l'API fluide d'`OptionalInt` :

```java
static int parseMajorJavaVersion(String javaVersion) {
    OptionalInt version = parseDotted(javaVersion);
    if (!version.isPresent()) version = extractBeginningInt(javaVersion);
    return version.orElse(MINIMUM_SUPPORTED_JAVA_VERSION);
}
```

#### Pourquoi c'est mieux

- Le type `OptionalInt` exprime explicitement l'absence de valeur — pas besoin de convention implicite
- `orElse(MINIMUM_SUPPORTED_JAVA_VERSION)` documente l'intention en une seule ligne
- Élimination de la constante `UNKNOWN_VERSION` devenue inutile
- Code plus idiomatique Java moderne

---

### Commit 12 — Déduplication nextInt / nextLong

**Lien du commit :** `https://gitlab.univ-lille.fr/kibalo-beni.tagba.etu/kibalo_beni_tagba_gl_project/-/commit/38f50d68be97415eb699dc4cc635014e665c39f9`

#### Problème

Les méthodes `nextInt()` et `nextLong()` dans `JsonReader.java` contenaient un bloc de code identique pour mettre en buffer le token courant :

```java
// Dans nextLong() ET nextInt() — exactement le même code :
if (p == PEEKED_NUMBER) {
    peekedString = new String(buffer, bufferPos, peekedNumberLength);
    bufferPos += peekedNumberLength;
} else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED || p == PEEKED_UNQUOTED) {
    if (p == PEEKED_UNQUOTED) {
        peekedString = nextUnquotedValue();
    } else {
        peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\'' : '"');
    }
    // ...
} else {
    throw unexpectedTokenError("...");
}
```

Toute modification (nouveau type de token, correction de bug) devait être appliquée aux deux méthodes.

#### Contexte environnant

Ces méthodes font partie de l'API publique de `JsonReader`. La logique dupliquée concerne la mise en buffer du token — une étape préalable commune avant le parsing spécifique au type cible (int ou long).

#### Correction

Extraction du code commun dans une méthode privée :

```java
private boolean bufferPeekedNumberAsString(int p, String typeName) throws IOException {
    if (p == PEEKED_NUMBER) {
        peekedString = new String(buffer, bufferPos, peekedNumberLength);
        bufferPos += peekedNumberLength;
        return false; // pas un token string
    } else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED || p == PEEKED_UNQUOTED) {
        peekedString = (p == PEEKED_UNQUOTED) ? nextUnquotedValue()
                                               : nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\'' : '"');
        return true; // c'est un token string
    } else {
        throw unexpectedTokenError(typeName);
    }
}
```

#### Pourquoi c'est mieux

- Le code commun est en **un seul endroit**
- La valeur de retour booléenne (`true` = c'est une chaîne) guide la logique de parsing rapide
- Toute future correction ou ajout de token bénéficie immédiatement aux deux méthodes

---

### Commit 13 — Tests pour AbstractSqlDateTypeAdapter

**Lien du commit :** `https://gitlab.univ-lille.fr/kibalo-beni.tagba.etu/kibalo_beni_tagba_gl_project/-/commit/a2bea059854699aba06e05de8257b71077861e64`

#### Motivation

Après le commit 4 (création de `AbstractSqlDateTypeAdapter`), la logique commune — notamment la conversion `ParseException → JsonSyntaxException` et l'utilisation de `getTypeName()` dans le message d'erreur — n'était pas couverte par des tests dédiés. Les tests existants dans `SqlTypesGsonTest` testaient les adaptateurs concrets mais pas le comportement générique de la classe abstraite.

#### Ce qui est testé

Le fichier `AbstractSqlDateTypeAdapterTest.java` couvre :

1. **Sérialisation de null** — pour `java.sql.Date` et `java.sql.Time` → doit produire `"null"`
2. **Désérialisation de null JSON** — doit retourner `null` Java
3. **Entrée invalide pour Date** — doit lancer `JsonSyntaxException` avec le message contenant `"SQL Date"` et la chaîne fautive
4. **Entrée invalide pour Time** — doit lancer `JsonSyntaxException` avec le message contenant `"SQL Time"` et la chaîne fautive

Les cas 3 et 4 valident spécifiquement la méthode abstraite `getTypeName()` qui était la principale valeur ajoutée du refactoring du commit 4.

#### Pourquoi c'est pertinent

Un test qui casse lors de la suppression de `getTypeName()` prouve que cette méthode a une valeur observable. Ces tests fournissent un **filet de sécurité** pour tout futur refactoring de la hiérarchie SQL.

---

### Commit 14 — Décomposition de la god class TypeAdapters

**Lien du commit :** `https://gitlab.univ-lille.fr/kibalo-beni.tagba.etu/kibalo_beni_tagba_gl_project/-/commit/ad0037ff50c146c0dc4d305265b981fe0abe7c9c`

#### Problème

`TypeAdapters.java` est une classe de 1081 lignes regroupant tous les adaptateurs de types de base. Parmi ses composants, deux classes internes privées (`FloatAdapter` et `DoubleAdapter`) étaient définies dans le même fichier alors qu'elles auraient leur place dans des fichiers séparés. Ces classes internes alourdissent inutilement un fichier déjà volumineux.

#### Contexte environnant

`FloatAdapter` et `DoubleAdapter` partagent la même structure (champ `strict`, méthode `read`, méthode `write`) et dépendent toutes les deux d'une méthode utilitaire `checkValidFloatingPoint`. Elles constituent des adaptateurs autonomes qui méritent leur propre fichier.

#### Correction

- Création de `FloatTypeAdapter.java` et `DoubleTypeAdapter.java` dans le même package
- Rendre `checkValidFloatingPoint` package-private dans `TypeAdapters` pour qu'elle reste accessible
- Suppression des inner classes de `TypeAdapters`, remplacement par les nouvelles classes

```java
// TypeAdapters.java — avant
private static class FloatAdapter extends TypeAdapter<Number> { ... }
public static final TypeAdapter<Number> FLOAT = new FloatAdapter(false);

// TypeAdapters.java — après
static void checkValidFloatingPoint(double value) { ... }
public static final TypeAdapter<Number> FLOAT = new FloatTypeAdapter(false);
```

#### Pourquoi c'est mieux

- `TypeAdapters.java` passe de 1081 à ~950 lignes — plus facile à naviguer
- Chaque classe a son propre fichier — convention Java standard
- `FloatTypeAdapter` et `DoubleTypeAdapter` peuvent évoluer indépendamment

---

### Commit 15 — Suppression de isPrimitive dans Primitives

**Lien du commit :** `https://gitlab.univ-lille.fr/kibalo-beni.tagba.etu/kibalo_beni_tagba_gl_project/-/commit/34ca7a5bbebe7a3f2124cb7a66443cc0d59bba34`

#### Problème

La méthode `isPrimitive(Type type)` dans `Primitives.java` était une simple délégation à `Class.isPrimitive()` avec une vérification de type :

```java
public static boolean isPrimitive(Type type) {
    return type instanceof Class<?> && ((Class<?>) type).isPrimitive();
}
```

Cette méthode n'apporte pas de valeur ajoutée par rapport à l'appel direct. Sa suppression réduit la surface de la classe utilitaire `Primitives`.

#### Contexte environnant

`Primitives` est une classe utilitaire `final` avec constructeur privé (pattern utilitaire Java classique) contenant des opérations sur les types primitifs. Elle comptait 4 méthodes ; `isPrimitive` était la plus simple et la plus facilement inlinable.

#### Correction

- **`ReflectiveTypeAdapterFactory.java`** : remplacement de `Primitives.isPrimitive(fieldType.getRawType())` par `fieldType.getRawType().isPrimitive()` — appel direct sans couche d'indirection
- **`ParameterizedTypeFixtures.java`** (test) : remplacement de `Primitives.isPrimitive(genericClass)` par `genericClass instanceof Class<?> && ((Class<?>) genericClass).isPrimitive()` — inline exact de l'ancienne implémentation
- Suppression de la méthode et de l'import correspondant dans les deux fichiers

#### Pourquoi c'est mieux

- Suppression d'une couche d'indirection inutile
- Réduction de la surface publique de `Primitives`
- Appel direct à l'API Java standard — plus lisible pour un lecteur qui connaît Java

---

## 4. Tentative ratée documentée

### Tentative : Suppression de newFactoryForMultipleTypes dans TypeAdapters

#### Ce qui a été tenté

Lors de la planification du commit 7 initial, il a été envisagé de supprimer la méthode `newFactoryForMultipleTypes` de `TypeAdapters.java`, identifiée à tort comme du **code mort** (code jamais appelé).

#### Pourquoi ça n'a pas fonctionné

Une vérification avec `grep` a révélé que la méthode était bel et bien appelée à la **ligne 897** de `TypeAdapters.java` :

```
TypeAdapters.java:897: newFactoryForMultipleTypes(Calendar.class, GregorianCalendar.class, CALENDAR);
```

Elle sert à enregistrer un adaptateur commun pour `Calendar` et `GregorianCalendar`. Sa suppression aurait rompu la désérialisation des objets `Calendar` dans Gson.

#### Ce que ça m'a appris

Cette erreur illustre l'importance de **toujours vérifier les usages réels dans tout le code source** avant de supprimer une méthode, en particulier dans un projet de grande taille. Un simple `grep` préalable suffit à éviter ce type d'erreur. La leçon : ne jamais supposer qu'une méthode est inutile sans avoir cherché activement ses appelants.

---

## 5. Ce que j'ai appris

Ce projet m'a permis de consolider plusieurs notions théoriques vues en cours en les appliquant sur un vrai projet open-source Java professionnel.

**Sur la lisibilité du code :** J'ai réalisé que beaucoup de défauts de lisibilité passent inaperçus quand on écrit le code (on sait ce que signifie `32` quand on l'écrit), mais deviennent immédiatement problématiques quand quelqu'un d'autre lit le code des semaines plus tard. Les constantes nommées et les bons noms de variables sont un investissement faible pour un gain de compréhension élevé.

**Sur la duplication :** L'extraction d'une superclasse abstraite pour `SqlDateTypeAdapter` et `SqlTimeTypeAdapter` m'a montré concrètement comment la duplication crée une fragilité cachée. Le code semblait correct — et il l'était — mais toute évolution future (nouveau format, nouvelle gestion d'erreur) aurait nécessité deux modifications synchronisées, avec risque d'oubli.

**Sur les types Java modernes :** L'utilisation d'`OptionalInt` à la place d'une valeur sentinelle (`-1`) m'a sensibilisé à l'importance de choisir le bon type pour exprimer l'intention. Le système de types est un outil de documentation puisque `OptionalInt` communique immédiatement l'idée de « valeur optionnelle ».

**Sur la décomposition des méthodes :** Les méthodes longues comme `doPeek()` ou `peekNumber()` sont souvent le résultat d'une évolution progressive du code. Extraire des sous-méthodes nommées est un geste simple qui améliore drastiquement la navigabilité et la testabilité.

**Sur la vérification avant modification :** La tentative ratée de suppression de `newFactoryForMultipleTypes` est la leçon pratique la plus mémorable : jamais de suppression sans vérification exhaustive des usages. Un outil comme `grep` ou la fonctionnalité « Find Usages » d'un IDE est indispensable avant tout refactoring destructif.

**Sur les commits atomiques :** Travailler commit par commit m'a forcé à réfléchir à chaque modification de manière isolée, à valider qu'elle est correcte avant de passer à la suivante. Cette discipline est directement transférable en contexte professionnel.
