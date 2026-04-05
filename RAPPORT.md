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
   - [Commit 1 — Nombres magiques dans JsonWriter](#commit-1--nombres-magiques-dans-jsonwriter)
   - [Commit 2 — Nombres magiques dans JsonReader](#commit-2--nombres-magiques-dans-jsonreader)
   - [Commit 3 — Nombres magiques dans JsonWriter](#commit-3--nombres-magiques-dans-jsonwriter)
   - [Commit 4 — Réorganisation de la structure de GsonBuilder](#commit-4--réorganisation-de-la-structure-de-gsonbuilder)
   - [Commit 5 — Reduction de la complexité de methodes dans TypeAdapters](#commit-5--reduction-de-la-complexité-de-methodes-dans-typeadapters)
   - [Commit 6 — Décomposition de nextString()](#commit-6--décomposition-de-nextstring)
   - [Commit 7 — Décomposition de la méthode doPeek](#commit-7--décomposition-de-la-méthode-dopeek)
   - [Commit 8 — Refactoring de duplication de code dans JsonReader](#commit-8--refactoring-de-duplication-de-code-dans-jsonreader)
   - [Commit 9 — SuperClasse AbstractSqlDateTypeAdapter](#commit-9--superclasse-abstractsqldatetypeadapter)
   - [Commit 10 — Tests pour AbstractSqlDateTypeAdapter](#commit-10--tests-pour-abstractsqldatetypeadapter)
   - [Commit 11 — Décomposition de la god class TypeAdapters](#commit-11--décomposition-de-la-god-class-typeadapters)

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


### Commit 6 — Décomposition de `nextString()`

**Lien du commit :** `https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/d4ed109a`

#### Problème

Dans `gson/src/main/java/com/google/gson/stream/JsonReader.java`, la méthode `nextString()` mélangeait
deux responsabilités :

- lire la valeur selon le token courant
- mettre à jour l’état interne du lecteur après consommation

Cette accumulation rendait la méthode plus longue et moins lisible que nécessaire.

#### Contexte environnant

`nextString()` est une méthode publique de `JsonReader`, utilisée pour lire une chaîne depuis le flux JSON.
Comme elle fait partie des points d’entrée importants de l’API, il est préférable qu’elle reste courte et
facile à comprendre.

#### Correction

La logique a été séparée en deux méthodes privées :

- `readNextStringValue(int peekedToken)` pour lire la valeur selon le token détecté
- `consumePeekedValue()` pour réinitialiser `peeked` et mettre à jour `pathIndices`

La méthode `nextString()` devient ainsi une méthode d’orchestration qui délègue ces deux tâches.

#### Pourquoi c'est mieux

- `nextString()` est plus courte et plus claire
- La lecture de la valeur et la mise à jour de l’état sont bien séparées
- Le code est plus simple à maintenir et à faire évoluer

---

### Commit 7 — Décomposition de la méthode doPeek

**Lien du commit :** `https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/b56a05d1528e78e1853a08796912291b1f6ef3b6`

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

### Commit 8 — Refactoring de duplication de code dans JsonReader

**Lien du commit :** `https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/a824786d2578e8d52c9e06ef1da9c40790ed034f`

#### Problème

Dans `gson/src/main/java/com/google/gson/stream/JsonReader.java`, les méthodes `beginArray()`,
`endArray()`, `beginObject()` et `endObject()` contenaient le même schéma de vérification :

- lire `peeked`
- appeler `doPeek()` si nécessaire
- vérifier que le token obtenu correspond bien à celui attendu
- sinon lever `unexpectedTokenError(...)`

Cette répétition créait de la duplication dans quatre méthodes publiques proches.

#### Contexte environnant

Ces quatre méthodes font partie de l’API publique de `JsonReader` et servent à consommer les délimiteurs
structuraux du JSON. Leur logique métier est différente ensuite, mais leur phase de validation du token était
identique.

#### Correction

Le code commun a été extrait dans une méthode privée :

```java
private int requirePeeked(int expectedToken, String expectedDescription) throws IOException
```

Chaque méthode publique appelle désormais ce helper pour centraliser la vérification du token attendu.

#### Pourquoi c'est mieux

- La duplication est supprimée dans `beginArray`, `endArray`, `beginObject` et `endObject`
- La vérification du token attendu est centralisée dans une seule méthode
- Le code devient plus court et plus cohérent
- Une future modification de cette logique n’aura plus à être faite à plusieurs endroits

---

### Commit 9 — SuperClasse AbstractSqlDateTypeAdapter

**Lien du commit :** `https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/eb52fb8ca862f68d9bd7370c541fa492c6413e5c`

#### Problème

`SqlDateTypeAdapter` et `SqlTimeTypeAdapter` contenaient presque la même logique de lecture et d’écriture,
ce qui créait de la duplication.

#### Contexte environnant

Ces deux classes appartiennent au package `internal.sql` et gèrent des types SQL proches.

#### Correction

Création d’une classe abstraite `AbstractSqlDateTypeAdapter` qui factorise la logique commune. Les classes
`SqlDateTypeAdapter` et `SqlTimeTypeAdapter` héritent désormais de cette base et ne gardent que leur partie
spécifique.

#### Pourquoi c'est mieux

- La duplication est supprimée
- Le code commun est centralisé dans une seule classe
- Les deux adaptateurs deviennent plus simples à maintenir

---

### Commit 10 — Tests pour AbstractSqlDateTypeAdapter

**Lien du commit :** `https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/3e49780953ab7727820bb2571f159df9496beb67`

#### Motivation

Après le commit 9 (création de `AbstractSqlDateTypeAdapter`), la logique commune — notamment la conversion `ParseException → JsonSyntaxException` et l'utilisation de `getTypeName()` dans le message d'erreur — n'était pas couverte par des tests dédiés. Les tests existants dans `SqlTypesGsonTest` testaient les adaptateurs concrets mais pas le comportement générique de la classe abstraite.

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

### Commit 11 — Décomposition de la god class TypeAdapters

**Lien du commit :** `https://gitlab-etu.fil.univ-lille.fr/metogbe-espera.yebe.etu/gl/-/commit/7b9e1cfefd7df6ce137839aef58755d2e4d2d364`

#### Modification

Ce commit extrait la logique de `FloatAdapter` et `DoubleAdapter` hors de `TypeAdapters.java` dans une
nouvelle classe `FloatingPointTypeAdapters`.

#### Pourquoi c'est utile

- `TypeAdapters.java` devient plus léger
- La logique des adaptateurs flottants est regroupée dans un fichier dédié
- Le code est plus simple à lire et à maintenir

---

## 4. Tentative ratée documentée

Les tentatives ratées etaient plus axés au niveau de la suppression des codes morts où j'ai dû supprimer des methodes signalées comme non utilisée en interne, mais qui était 
important pour la Javadoc comme `getAsJsonNull` vu que Gson est une API. Ce qui m'a obligé à reprendre mon projet vu que mon projet sur git etait cassé .

## 5. Ce que j'ai appris

Ce projet m'a permis de consolider plusieurs notions théoriques vues en cours en les appliquant sur un vrai projet open-source Java professionnel.

**Sur la lisibilité du code :** J'ai réalisé que beaucoup de défauts de lisibilité passent inaperçus quand on écrit le code (on sait ce que signifie `32` quand on l'écrit), mais deviennent immédiatement problématiques quand quelqu'un d'autre lit le code des semaines plus tard. Les constantes nommées et les bons noms de variables sont un investissement faible pour un gain de compréhension élevé.

**Sur la duplication :** L'extraction d'une superclasse abstraite pour `SqlDateTypeAdapter` et `SqlTimeTypeAdapter` m'a montré concrètement comment la duplication crée une fragilité cachée. Le code semblait correct — et il l'était — mais toute évolution future (nouveau format, nouvelle gestion d'erreur) aurait nécessité deux modifications synchronisées, avec risque d'oubli.

**Sur la décomposition des méthodes :** Les méthodes longues comme `doPeek()` ou `nextString()` sont souvent le résultat d'une évolution progressive du code. Extraire des sous-méthodes nommées est un geste simple qui améliore drastiquement la navigabilité et la testabilité.

**Sur la vérification avant modification :** La tentative ratée de suppression de `newFactoryForMultipleTypes` est la leçon pratique la plus mémorable : jamais de suppression sans vérification exhaustive des usages. Un outil comme `grep` ou la fonctionnalité « Find Usages » d'un IDE est indispensable avant tout refactoring destructif.

**Sur les commits atomiques :** Travailler commit par commit m'a forcé à réfléchir à chaque modification de manière isolée, à valider qu'elle est correcte avant de passer à la suivante. Cette discipline est directement transférable en contexte professionnel.
