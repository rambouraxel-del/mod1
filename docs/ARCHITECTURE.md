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
default void registerItemTags(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tags,
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

- `TVConfig.COMMON` (type `COMMON`) enregistré par `TerresVivantes`; il reste vide.
- Chaque module a sa propre spec `<Nom>Config.SPEC` (type `COMMON`) avec son `FILE_NAME` (ex. `terresvivantes-crop.toml`). C'est l'intégrateur qui l'enregistre dans `TerresVivantes` : `container.registerConfig(ModConfig.Type.COMMON, X.SPEC, X.FILE_NAME)`.
- Chaque spec ajoute ses sous-sections avec `builder.push("nom") ... builder.pop()`.

## Datagen

`TVDataGen` (écouteur `GatherDataEvent`) crée :
- deux `LanguageProvider` (en_us, fr_fr);
- `TVItemModelProvider`, `TVBlockStateProvider` (côté client);
- `TVRecipeProvider`, `LootTableProvider` (`TVBlockLoot`), `TVBlockTagsProvider` et `TVItemTagsProvider` (côté serveur, ce dernier lit les tags d'items via `registerItemTags`).

Chaque provider appelle le hook correspondant de chaque module. Pour ajouter une ressource, le module implémente le hook; aucun fichier partagé n'est modifié.

## Décisions

- **Récolte à main vide** : la récolte d'une culture mûre se fait par clic droit en main principale avec la main vide (`CropEvents`). Un outil ou un aliment en main ne récolte pas, ce qui évite les récoltes accidentelles pendant la plantation ou la consommation.
- **Vitesse = probabilité de tentative** : la vitesse de croissance n'est pas un compteur de ticks. Chaque randomTick tente une croissance avec la probabilité `GROWTH_STEPS / (growthDays * RANDOM_TICKS_PER_DAY)` (`CropLogic.attemptChance`), puis le facteur total (saison, affinité, climat, sol, humidité) décide si la tentative réussit (`GrowthModel.decide`). Une culture de `growthDays` jours MC franchit ainsi ses 7 stades à facteur 1 en moyenne.
- **Configs par module** : chaque module possède sa spec COMMON et son fichier TOML, enregistrés par l'intégrateur. Un module ne modifie pas `TVConfig`.
- **Tags d'items par module** : le tag `terresvivantes:compostable` est défini par le module compost (hook `registerItemTags`), qui y inclut les produits et graines du module crop. Pas de JSON manuel dans `src/main/resources` pour les tags générés.

## Contrats inter-modules

Un module n'appelle un autre que par sa façade statique `<Nom>Api`. Les implémentations marquées STUB sont provisoires.

- `season.SeasonApi` : saison courante, jour dans la saison, jours par saison, multiplicateur saisonnier, `seasonGrowthMultiplier(Level, BlockPos)` positionnel avec serre (v0.2)
- `climate.ClimateApi` : `temperatureAt(Level, BlockPos)` en °C, `isSheltered(Level, BlockPos)` (serre, v0.2)
- `soil.SoilApi` : lecture, consommation, engrais, facteur de Liebig, effacement des données de sol, dernière culture et répétitions (v0.2)
- `crop.CropApi` : culture mûre, récolte et replantation, cultures optimales par saison, et par saison + température locale (`optimalCropIdsFor(Season, float)`) (STUB, module crop)
- `season.logic.Season` : enum des saisons (`next()`, `langKey()`), logique pure
- `crop.logic.GrowthModel` : facteur total, facteur climatique et décision de croissance, logique pure réelle
- `crop.logic.Affinity` : affinité saisonnière d'une culture (multiplicateur 1.25 / 1.0 / 0.4 / 0)
- `soil.logic.SoilData` : record N/P/K/organique bornés à 0-100

## Contrats v0.2

### Points d'extension de croissance (`crop.CropGrowthHooks`)

Enregistrés par les modules au démarrage (listes `CopyOnWriteArrayList`). `CropDefinition def` vaut `null` pour une culture vanilla.

| Contrat | Enregistrement | Effet |
|---|---|---|
| `GrowthModifier { float modify(ServerLevel, BlockPos, BlockState, def); }` | `registerModifier` | Facteur total = produit de `GrowthModel.totalFactor` et de tous les modifiers (`GrowthModel.combineModifiers`, négatif ramené à 0). TV et vanilla. |
| `GrowthListener { onGrowthAttempt(ServerLevel, BlockPos, BlockState, def, GrowthDecision, float factor); }` | `registerListener` | Appelé après chaque décision. TV : une fois par tentative. Vanilla : une fois à `CropGrowEvent.Pre` et une fois à `Post`. |
| `YieldModifier { float modify(ServerLevel, BlockPos, BlockState, def); }` | `registerYieldModifier` | Multiplie le nombre de produits (`YieldModel.scaledCount`, arrondi probabiliste). TV : dans `TVCropBlock.getDrops`, donc récolte et casse. Vanilla : dans `CropApi.harvestAndReplant` seulement, à tous les drops. |
| `PlantListener { onPlanted(ServerLevel, BlockPos, BlockState); }` | `registerPlantListener` | Plantation et replantation. TV : `setPlacedBy` et replantation. Vanilla : `BlockEvent.EntityPlaceEvent` et replantation. |

### Serres (module climate, season)

- `ClimateApi.isSheltered` : en remontant jusqu'à 12 blocs, le premier bloc non-air est dans `c:glass_blocks` ou `c:glass_panes`, et la face supérieure de ce verre voit le ciel (le verre lui-même est dans la heightmap et ne voit jamais le ciel).
- `ClimateApi.temperatureAt` : +8 °C (`ClimateModel.GREENHOUSE_BONUS`) si abrité.
- `SeasonApi.seasonGrowthMultiplier(Level, BlockPos)` : en hiver sous serre, `SeasonConfig.greenhouseWinterMultiplier` (défaut 0.6). Les cultures TV et vanilla utilisent cette version.
- Limite : `Affinity` vaut `IMPOSSIBLE` en hiver pour toute culture du mod (aucune n'a l'hiver dans ses saisons), donc la serre n'a aucun effet sur les cultures du mod tant que leurs affinités ne changent pas.

### Rotation (module soil)

- `SoilApi.getLastCrop`, `setLastCrop`, `getRepeatCount`. La dernière culture et son compteur sont stockés dans l'attachment de chunk (`SoilChunkData`, champ `last_crops` optionnel). Le format v0.1 reste lisible.
- `setLastCrop` est appelé à la récolte (`CropApi.harvestAndReplant`) et à la casse d'une culture mûre (`CropEvents`). Répétition : +1 si la même culture, 0 sinon.
- `SoilRotation` (modifier interne) applique `RotationModel.factor` : monoculture (répétition ≥ 2 et culture actuelle = dernière culture) ×0.8 ; précédent légumineux et culture actuelle non légumineuse ×1.15.
- La sonde de sol affiche la dernière culture, sa répétition et l'effet de rotation.

### Modules vides

`orchard.OrchardModule`, `water.WaterModule`, `health.HealthModule` : enregistrés dans `TerresVivantes.MODULES`, sans contenu.

### Dépendances entre packages

`crop` ↔ `soil` et `season` ↔ `climate` forment un cycle (facades statiques). Il est accepté en v0.2 : aucune de ces classes ne dépend d'un état initialisé à l'import.

