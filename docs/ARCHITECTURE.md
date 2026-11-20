# Architecture

## Stack

- Minecraft 1.21.1, NeoForge 21.1.267
- ModDevGradle (`net.neoforged.moddev`) 2.0.148, Gradle 8.14.3, JDK 21
- Tests unitaires JUnit 5.11.4
- Parchment : non utilisé pour l'instant (dépôt inaccessible depuis l'environnement de build)

## Arborescence

```
src/main/java/fr/terresvivantes/
  TerresVivantes.java          @Mod, liste fixe des modules (seul point d'enregistrement)
  core/
    TVModule.java              contrat d'un module + hooks datagen
    config/TVConfig.java       ModConfigSpec COMMON
    network/TVNetwork.java     payloads (version "1")
    registry/TVCreativeTab.java onglet créatif unique
    gametest/TVSmokeGameTests.java test de fumée du serveur gametest
  datagen/                     providers qui interrogent les hooks de tous les modules
  season/ climate/ soil/ crop/ compost/ food/ tools/
    <Nom>Module.java           implémente TVModule, ses DeferredRegister
    logic/<Nom>Logic.java      logique pure, sans import net.minecraft
src/main/templates/META-INF/neoforge.mods.toml   métadonnées (placeholders gradle.properties)
src/main/resources/          assets statiques, structures de test (data/terresvivantes/structure)
src/generated/resources/     sortie de runData (versionnée)
src/test/java/               tests JUnit de la logique pure
```

## Règle : un module = un package

Chaque module :
- est un package `fr.terresvivantes.<module>`;
- a sa classe `<Nom>Module implements TVModule` avec ses propres `DeferredRegister` (blocs, items, etc.);
- expose ses hooks datagen (surcharges de `TVModule`);
- ne modifie jamais un fichier partagé.

`TerresVivantes.java` est la seule liste de modules. Ajouter un module = une ligne dans `MODULES`.

## Hooks de TVModule

```java
void register(IEventBus modBus);
default void registerLang(LangSink sink) {}                       // sink.add(clé, enUs, frFr)
default void registerItemModels(ItemModelProvider provider) {}
default void registerBlockStates(BlockStateProvider provider) {}
default void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {}
default void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                HolderLookup.Provider lookup) {}
default void registerBlockTags(Function<TagKey<Block>, TagsProvider.TagAppender<Block>> tags,
                               HolderLookup.Provider lookup) {}
```

Le hook de langue reçoit toujours les deux traductions : `TVLanguageProvider` produit `en_us` et `fr_fr` à partir du même appel.

## Logique pure testable

Le sous-package `logic` de chaque module ne dépend pas de Minecraft (aucun import `net.minecraft`). Il se teste en JUnit sans lancer le jeu. Exemple : `season/logic/SeasonLogic.java`, testé par `SeasonLogicTest`.

Les classes `Module` ne contiennent que l'enregistrement et les hooks; la règle métier vit dans `logic`.

## Performances

- Pas de `BlockEntity` qui tickent pour les cultures : elles évoluent par `randomTick`, ce qui évite un coût par bloc à chaque tick.
- Les données de sol (humidité, fertilité) sont stockées en **Data Attachment de chunk**, pas dans des BlockEntity.
- Les mises à jour de culture passent par des événements ponctuels, pas par des boucles globales.

## Réseau

- Un seul enregistrement dans `TVNetwork`, version `"1"`.
- Chaque module déclare ses payloads dans `onRegisterPayloads` via le `PayloadRegistrar` (serveur <-> client).
- Incrémenter `PROTOCOL_VERSION` à chaque changement incompatible.

## Configuration

- `TVConfig.COMMON` (type `COMMON`) enregistré par `TerresVivantes`.
- Chaque module ajoute sa sous-section avec `builder.push("nom") ... builder.pop()`.
- Vide en Phase 0.

## Datagen

`TVDataGen` (écouteur `GatherDataEvent`) crée :
- deux `LanguageProvider` (en_us, fr_fr);
- `TVItemModelProvider`, `TVBlockStateProvider` (côté client);
- `TVRecipeProvider`, `LootTableProvider` (`TVBlockLoot`), `TVBlockTagsProvider` (côté serveur).

Chaque provider appelle le hook correspondant de chaque module. Pour ajouter une ressource, le module implémente le hook; aucun fichier partagé n'est modifié.

## Contrats inter-modules

Un module n'appelle un autre que par sa façade statique `<Nom>Api`. Les implémentations marquées STUB sont provisoires.

- `season.SeasonApi` : saison courante, jour dans la saison, jours par saison, multiplicateur saisonnier (STUB, module season)
- `climate.ClimateApi` : `temperatureAt(Level, BlockPos)` en °C (STUB, module climate)
- `soil.SoilApi` : lecture, consommation, engrais, facteur de Liebig, effacement des données de sol (STUB, module soil)
- `crop.CropApi` : culture mûre, récolte et replantation, cultures optimales par saison (STUB, module crop)
- `season.logic.Season` : enum des saisons (`next()`, `langKey()`), logique pure
- `crop.logic.GrowthModel` : facteur total, facteur climatique et décision de croissance, logique pure réelle
- `crop.logic.Affinity` : affinité saisonnière d'une culture (multiplicateur 1.25 / 1.0 / 0.4 / 0)
- `soil.logic.SoilData` : record N/P/K/organique bornés à 0-100
