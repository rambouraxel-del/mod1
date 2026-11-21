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
    command/TVCommands.java    racine unique /tv (sous-arbres season, showcase)
    debug/                     DebugModule (lang), ShowcaseBuilder + ShowcaseCommands (/tv showcase)
    config/TVConfig.java       ModConfigSpec COMMON
    network/TVNetwork.java     payloads (version "1")
    registry/TVCreativeTab.java onglet créatif unique
    gametest/TVSmokeGameTests.java test de fumée du serveur gametest
  datagen/                     providers qui interrogent les hooks de tous les modules
  season/ climate/ soil/ crop/ compost/ food/ tools/ orchard/ water/ health/
  pollination/ flora/ insects/ apiculture/   (v0.3)
  livestock/ husbandry/ vet/ duck/           (v0.4)
  core.quality/ processing/ milling/ dairy/ preserving/ kitchen/   (v0.5)
  implement/ automation/ storage/ pests/     (v0.6)
  economy/ market/ guild/                    (v0.7)
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
- Chaque module a sa propre spec `<Nom>Config.SPEC` (type `COMMON`) avec son `FILE_NAME` (ex. `terresvivantes-crop.toml`). C'est l'intégrateur qui l'enregistre dans `TerresVivantes` : `container.registerConfig(ModConfig.Type.COMMON, X.SPEC, X.FILE_NAME)`. Enregistrées en v0.2 : season, soil, compost, crop, water, health, orchard. En v0.3 : flora, insects, apiculture. En v0.4 : livestock, husbandry, vet, duck. En v0.6 : automation, pests (le module storage n'a pas de config).
- Une valeur de défaut ne s'applique qu'à la création du fichier : un TOML existant garde ses valeurs.
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
| `GrowthListener { onGrowthAttempt(ServerLevel, BlockPos, BlockState, def, GrowthDecision, float factor); }` | `registerListener` | Appelé après chaque décision, une fois par tentative. TV : dans `TVCropBlock.randomTick`. Vanilla : seulement à `CropGrowEvent.Pre` (`CropEvents`), jamais à `Post` (le `Post` ne fait que le stade bonus). `def` vaut `null` pour le vanilla. |
| `YieldModifier { float modify(ServerLevel, BlockPos, BlockState, def); }` | `registerYieldModifier` | Multiplie le nombre de produits (`YieldModel.scaledCount`, arrondi probabiliste). TV : dans `TVCropBlock.getDrops`, donc récolte et casse. Vanilla : dans `CropApi.harvestAndReplant` seulement, à tous les drops. |
| `PlantListener { onPlanted(ServerLevel, BlockPos, BlockState); }` | `registerPlantListener` | Plantation et replantation. TV : `setPlacedBy` et replantation. Vanilla : `BlockEvent.EntityPlaceEvent` et replantation. |

### Serres (module climate, season)

- `ClimateApi.isSheltered` : en remontant jusqu'à 12 blocs, le premier bloc non-air est dans `c:glass_blocks` ou `c:glass_panes`, et la face supérieure de ce verre voit le ciel (le verre lui-même est dans la heightmap et ne voit jamais le ciel).
- `ClimateApi.temperatureAt` : +8 °C (`ClimateModel.GREENHOUSE_BONUS`) si abrité.
- `SeasonApi.seasonGrowthMultiplier(Level, BlockPos)` : en hiver sous serre, `SeasonConfig.greenhouseWinterMultiplier` (défaut 0.6). Les cultures TV et vanilla utilisent cette version.
- Limite : en hiver, l'affinité vaut `IMPOSSIBLE` (facteur 0) pour les cultures du mod dont l'hiver n'est pas listé. Oignon, ail et chou ont l'hiver en `UNFAVORABLE` (0.4) : seules ces cultures peuvent profiter de la serre en hiver.

### Rotation (module soil)

- `SoilApi.getLastCrop`, `setLastCrop`, `getRepeatCount`. La dernière culture et son compteur sont stockés dans l'attachment de chunk (`SoilChunkData`, champ `last_crops` optionnel). Le format v0.1 reste lisible.
- `setLastCrop` est appelé à la récolte (`CropApi.harvestAndReplant`) et à la casse d'une culture mûre (`CropEvents`). Répétition : +1 si la même culture, 0 sinon.
- `SoilRotation` (modifier interne) applique `RotationModel.factor` : monoculture (répétition ≥ 2 et culture actuelle = dernière culture) ×0.8 ; précédent légumineux et culture actuelle non légumineuse ×1.15.
- La sonde de sol affiche la dernière culture, sa répétition et l'effet de rotation.

### Verger (module orchard)

- `orchard.OrchardApi.registerFruitSetModifier(FruitSetModifier)` : `float modify(ServerLevel, BlockPos, FruitTreeDefinition)`. Multiplie la chance de nouer les fruits (feuillage stade 1 vers 2). 1.0 neutre, 0.0 empêche. Prévu pour la pollinisation (v0.3).
- Biome modifiers (`data/terresvivantes/neoforge/biome_modifier/`) : orange en `#minecraft:is_savanna` et `#minecraft:is_jungle`; olive en `#minecraft:is_savanna` seulement; les autres espèces gardent leurs biomes c:.
- Tag `terresvivantes:compostable` : fruits du verger (`FruitTreeDefinition.fruit()`, pomme vanille comprise), défini par le module compost.

### Eau (module water)

- `water.WaterApi` : `isRainBarrel(state)`, `tryDrawWater(level, pos, amount)`, `isCoveredByActiveSprinkler(level, soilPos)`, `CAN_FILL_LEVELS`.
- Facteur de croissance `WaterGrowth.MODIFIER` : ×1.1 si la terre est dans la zone d'un arroseur actif (`CropGrowthHooks.registerModifier`).
- Sécheresse `WaterGrowth.DROUGHT` (`registerListener`) : en été, sans pluie et sans arroseur, la terre perd 1 d'humidité avec la chance de `terresvivantes-water.toml` (défaut 0.15). Vanilla inclus.

### Santé (module health)

- `health.HealthApi` : `get`, `isSick`, `isImmune`, `immuneUntilDay`, `infect`, `treat`, `wither`, `clear`, `treatmentEffects`, `report` (sonde de sol). Données par position en `HealthChunkData`.
- Branchements : modifier de croissance et modifier de rendement (maladie : croissance ×0.3, rendement ×0.5), listener de croissance (stress, maladie, flétrissement).

### Commandes

- `/tv` : racine unique, `core/command/TVCommands` (permission 2). Sous-arbres : `season` (`SeasonCommands.node()`), `showcase [pos]` (`ShowcaseCommands.node()`).
- `/tv showcase [pos]` : `ShowcaseBuilder.build(level, origin)` pose la plateforme; `origin` est la surface du coin (x min, z min). Sans position, origine = joueur + (-8, 20, 0). Le joueur est téléporté sur la rampe (`viewPoint`), face à la plateforme. Depuis la console, la position est obligatoire.
- Disposition : colonnes 0..11 cultures du mod, 12..15 vanilla (z 0..3, âges 0, 2, 5, 7); z 5 blocs du mod; z 8 saplings; z 10..15 feuillages (stades 0..3, persistants). Air au-dessus.

### Contrats v0.3

- `pollination.PollinationApi.score(ServerLevel, BlockPos)` : score de pollinisation ∈ [0,1], mis en cache par (dimension, chunk, tranche de 16 blocs) pendant 600 ticks; `scoreUncached` sans cache. Abeilles, ruches vanilla, fleurs; `registerPollinatorSource(PollinatorSource)` pour les apiaires et papillons.
- `pollination.BiodiversityApi.index(ServerLevel, BlockPos)` : indice de biodiversité ∈ [0,1], même cache; `registerHabitatSource(HabitatSource)` pour les haies.
- `pollination.logic.PollinationModel` : score, `cropYieldFactor` (×0.6 à ×1.3), `fruitYieldFactor`, `fruitSetFactor` (×0.5 à ×1.5); logique pure.
- `pollination.logic.BiodiversityModel.index(flowers, fruitLeaves, bees, habitat)` : indice pur.
- `crop.logic.CropDefinition.needsPollination` : vrai pour concombre, fraise, aubergine, poivron; lu par le `YieldModifier` enregistré par `PollinationModule`.
- `orchard.OrchardApi.registerFruitYieldModifier(FruitYieldModifier)` : multiplicateur du rendement d'une feuille mûre (appliqué dans `FruitLeavesBlock`); `OrchardApi.isFruitFoliage(BlockState)`. `registerFruitSetModifier` reçoit ×(0.5 + score).
- `health.HealthApi.infest(Level, BlockPos)` / `cure(Level, BlockPos)` : pucerons (`Disease.APHIDS`, croissance ×0.5, rendement ×0.8, pas de flétrissement); `diseaseOf(Level, BlockPos)`.
- `health.HealthApi.registerPestSuppressor(PestSuppressor)` / `pestSuppression(ServerLevel, BlockPos)` : produit des multiplicateurs d'apparition des ravageurs (lu par insects).
- `health.logic.HealthModel.biodiversityFactor(index)` : ×(1 − 0.5 × indice), appliqué à la chance d'infection dans `HealthGrowth`.
- Almanach (`tools`) : affiche la pollinisation et la biodiversité locales.

### Contrats apiculture (v0.3)

- `apiculture.ApicultureApi.registerNectarSource(Predicate<BlockState>, HoneyType)` : déclare un état qui apporte un
  type de nectar. Appelé à l'initialisation par `TerresVivantes` pour les fleurs de la flore (trèfle, lavande).
- `ApicultureApi.nectarOf(BlockState)` : type de miel d'un état, dans l'ordre suivant : sources enregistrées,
  feuillage fruitier en fleur (`OrchardApi.isFruitFoliage`, stade 1), tag `minecraft:flowers` (miel sauvage). Null sinon.
- `ApicultureApi.populatedApiaries(ServerLevel, BlockPos, radius)` : nombre d'apiaires peuplés proches.
- `apiculture.logic.HoneyType` (enum, sans import Minecraft) : WILDFLOWER, CLOVER, LAVENDER, ORCHARD.

### Contrats v0.4 (livestock)

- `livestock.LivestockApi` (façade) :
  - `Genome getGenome(Animal)`; `LivestockState getState(Animal)`; `void setState(Animal, LivestockState)`;
  - `void feed(Animal, int amount)` (borné à 100, note le jour du repas);
  - `void setAilment(Animal, Ailment)`;
  - `float productionMultiplier(Animal)` = formule `LivestockLogic.productionMultiplier` × modificateurs enregistrés;
  - `void runNeedsTick(ServerLevel, Animal)` (cycle de besoins; appelé par le module toutes les 1200 ticks);
  - `boolean isLivestock(Entity)`, `boolean isGrazer(Entity)`, `LIVESTOCK_TAG`.
- Hooks (listes `CopyOnWriteArrayList`, enregistrées à l'initialisation) :
  - `LivestockApi.ProductionModifier { float modify(Animal animal); }` via `registerProductionModifier`;
  - `LivestockApi.FeedSource { int feed(ServerLevel level, Animal animal); }` via `registerFeedSource`
    (points de satiété ajoutés à chaque cycle, 0 si rien).
- Attachments d'entité (`LivestockModule.GENOME`, `LivestockModule.STATE`) : sérialisés, sans copyOnDeath.
  Codecs dans `livestock.LivestockCodecs`; records purs dans `livestock.logic` (`Genome`, `LivestockState`,
  `Ailment`), sans codec, pour que les tests JUnit n'aient pas besoin de DataFixerUpper.
- Logique pure : `livestock.logic.GenomeLogic` (génome initial, hérédité), `livestock.logic.LivestockLogic`
  (cycle de besoins, multiplicateur, reproduction, jumeaux, arrondi probabiliste).
- Config : `LivestockConfig` (`terresvivantes-livestock.toml`, `breedingSeasonsOnly`), enregistrée par `TerresVivantes`.
- Modules `husbandry.HusbandryModule`, `vet.VetModule`, `duck.DuckModule` enregistrés dans `TerresVivantes.MODULES`;
  leurs configs `HusbandryConfig`, `VetConfig`, `DuckConfig` sont enregistrées par l'intégrateur.
- Tag d'entités `terresvivantes:livestock` : fichier statique `data/terresvivantes/tags/entity_type/livestock.json`
  (l'entrée `terresvivantes:duck` est optionnelle).

### Contrats v0.4 suite (husbandry, vet, duck)

- `LivestockApi.registerNeedsListener(NeedsListener)` : `onNeedsTick(ServerLevel, Animal, LivestockState)`, appelé après
  chaque cycle de besoins (`runNeedsTick`). Utilisé par `vet` : maladies, parasites, contagion, sel, mort à santé 0.
- `vet.VetApi` : `isNearSaltLick(level, pos)`, `treat(animal)`, `roll()`. Le tirage est remplaçable en test par
  `setRandomForTest(DoubleSupplier)`, à remettre avec `resetRandom()`.
- `husbandry.HusbandryEvents` : intercepte la ponte des poules (`Chicken.eggTime`) et des canards
  (`Duck.getEggTime` / `setEggTime`) vers le nichoir le plus proche (rayon `HusbandryConfig.nestingRadius`); sinon l'œuf tombe.
  Les espèces d'alimentation reconnaissent `DuckModule.DUCK`.
- `duck.logic.ProductionLogic` (pure) : `milkQuality(multiplier)` de 1 à 3 étoiles (composant générique `terresvivantes:quality`, v0.5),
  `extraWool(...)`.
- Tests de jeu : une zone de test n'est pas toujours « entity ticking », donc ses entités ne tickent pas ou sont
  invisibles aux requêtes. Les coccinelles du test de nettoyage sont ticquées explicitement (`Ladybug.tick()`); la vitrine
  vérifie ses animaux d'après le retour de `ShowcaseBuilder.build` (`Result(blocks, fauna)`), pas par requête.

## Contrats v0.5 (transformation)

- `core.quality.QualityModule` (module, enregistré en premier) : composant `terresvivantes:quality` (entier 1 à 3, persistant
  et synchronisé), infobulle générique. Façade statique : `QualityModule.QUALITY`, `qualityOf(ItemStack)` (renvoie
  `QualityLogic.ABSENT` = 0 si absent), `setQuality(ItemStack, int)` (borne 1 à 3).
- `core.quality.logic.QualityLogic` (pure) : `inheritMin(int[])`, `inheritAvg(int[])` (0 = absent, compté comme 1; tout
  absent donne absent), `ageQuality(base, daysAged)` (+1 étoile tous les 6 jours), `stars(q)`, `clamp(q)`.
- `processing.ProcessingApi` (façade) :
  - `MachineType registerMachine(String id, int inputSlots, boolean container)` (1 à 4 entrées; identifiant unique);
  - `Optional<MachineType> machine(String id)`, `Collection<MachineType> machines()`;
  - `registerItemHandler(RegisterCapabilitiesEvent, BlockEntityType<T>)` : capacité d'items par face (haut = entrées,
    côtés = conteneur, bas = sortie). À appeler sur le bus mod.
- `processing.MachineType` (record) : `id`, `inputSlots`, `container`; `containerSlot()` (-1 si absent), `outputSlot()`
  (toujours le dernier), `slotCount()`.
- `processing.ProcessingBlock` (abstrait, étend `HorizontalDirectionalBlock`) : propriétés `FACING` et `ACTIVE`; clic droit
  ouvre le menu; ticker serveur; éjection du contenu à la casse. Le module fournit `newBlockEntity` et `codec()`.
- `processing.ProcessingBlockEntity` (abstrait) : constructeur `(type, pos, state, MachineType)`; `serverTick`;
  `inventory()` (`ProcessingInventory`); `itemHandler(Direction)`; `dataAccess()` (progression, durée); hooks
  `craftingTick(level, pos)` (son ou particules pendant la mouture) et `craftFinished(level, pos, recipe)`.
- `processing.ProcessingRecipe` (type `terresvivantes:processing`, sérialiseur `ProcessingModule.SERIALIZER`) :
  `plan`, `assemble` (sortie avec qualité, sans consommer), `craft(input, registries, leftovers)` (sortie et consommation;
  les restes des ingrédients consommés, par exemple le seau vide, sont ajoutés à `leftovers`), `machine()`, `time()`,
  `quality()`, `container()` (`Optional<Ingredient>`). `ProcessingBlockEntity` place les restes : slot conteneur si libre,
  sinon sortie si elle accepte, sinon éjection au-dessus de la machine.
- Format JSON final (`ProcessingRecipeSerializer`) : `type`, `machine`, `ingredients` (`{ingredient, count}`, `ingredient`
  est un `Ingredient` : `{item}`, `{tag}`, ou un type `neoforge:*`), `container` (optionnel : `Ingredient`, ou l'ancienne
  chaîne d'id d'item), `result` (`{id, count}`), `time` (1 à 2 400 000), `quality` (`none`, `inherit_min`, `inherit_avg`, `age`).
- Lait de vache (dairy) : `{"type":"neoforge:difference","base":{"item":"minecraft:milk_bucket"},"subtracted":{"type":"neoforge:components","items":"minecraft:milk_bucket","components":{"terresvivantes:milk_source":"goat"}}}`.
  Lait de chèvre : `neoforge:components` avec `milk_source=goat`.
- Combustible du fumoir : tag d'items `terresvivantes:smoker_fuel` (`PreservingModule.SMOKER_FUEL`, généré par
  `registerItemTags` : `#minecraft:logs_that_burn`, charbon, charbon de bois).
- `processing.logic` (pure) : `QualityMode` (`none`, `inherit_min`, `inherit_avg`, `age`), `ProcessingLogic.outputQuality`,
  `allocate` (affectation des entrées aux slots, retour arrière), `isComplete`.
- `processing.ProcessingMenu` / `processing.client.ProcessingScreen` : menu et écran génériques (`MenuType` dans
  `ProcessingModule.MENU`).
- Recette de référence : `data/terresvivantes/recipe/millstone_wheat_flour.json` (module `milling`, `MillingModule.MILLSTONE_TYPE`).
- Modules `milling` (meule, pressoir, farines, jus, huile), `dairy` (baratte, cuve, étagère), `preserving` (tonneau,
  séchoir, fumoir), `kitchen` (marmite) : enregistrés dans `TerresVivantes.MODULES`. Aucune config nouvelle en v0.5.
- Pour une nouvelle machine : voir `docs/AGENT_GUIDE.md` (section « Créer une machine de transformation »).
- Restes de contenants : `ProcessingRecipe.craft` les rend (seau vide); aucun module ne les éjecte lui-même.

## Contrats v0.9 (vignoble, riz, races, APIs publiques)

Nouvelles entrées publiques et branchements. Règles : `docs/DESIGN.md` §22.

### Hooks de culture (`crop.CropGrowthHooks`)

Rendus publics pour les cultures hors `TVCropBlock` (riz, et récoltes vanilla) :

- `applyModifiers(level, pos, state, def)` : produit des `GrowthModifier` (1.0 sans modifier). `def` vaut `null` pour le riz et les cultures vanilla.
- `yieldModifierProduct(...)` et `scaleYield(level, pos, state, def, count)` : rendement, arrondi probabiliste.
- `notifyGrowthAttempt(level, pos, state, def, decision, factor)` : à appeler à chaque tentative de croissance. Un listener peut remplacer la culture; l'appelant doit vérifier son état après (comme `TVCropBlock`).
- `notifyPlanted(level, pos, state)` : à appeler à chaque plantation.

Le riz les utilise (`RiceCropBlock.randomTick`, `RiceApi.plant`). `WaterGrowth` ignore les cultures immergées (`WATERLOGGED`).

### Vignoble (`vineyard.VineyardApi`)

- `isRipe(BlockState)` : vrai pour une vigne au stade 3.
- `harvest(ServerLevel, BlockPos)` : récolte une vigne mûre, retourne les grappes (qualité `VineyardLogic.GRAPE_QUALITY`), à déposer par l'appelant. Liste vide sinon.
- Appelée par `GrapevineBlock` (clic droit) et par `HarvesterBlockEntity` (récolteuse).
- Écouteur de sortie de `ProcessingApi` : affinage du vin rouge et millésime (`VineyardModule`).

### Riz (`rice.RiceApi`)

- `plant(ServerLevel, soilPos)` : plante sur l'eau au-dessus d'une terre admise; notifie les `PlantListener`.

### Vet (`vet.VetApi`)

- `registerDiseaseChanceModifier(DiseaseChanceModifier)` : `Animal → float`, multiplie la chance de maladie par cycle (`VetNeeds`). Enregistré au démarrage.
- `diseaseChanceFactor(Animal)` : produit des modifiers (1.0 sans modifier).
- Branchement : `breeds.BreedsApi.diseaseChanceFactor` (Sussex ×0,7).

### Livestock (`livestock.LivestockApi`)

- `TwinListener` : `onTwin(twin, parentA, parentB)`, enregistré par `registerTwinListener`.
- `notifyTwin(twin, parentA, parentB)` : appelé par `LivestockEvents` pour chaque jumeau, après sa création et avant son ajout au monde. Le premier bébé passe toujours par `BabyEntitySpawnEvent`.
- Branchement : `breeds.BreedEvents.onTwin` (héritage de race).

### Races (`breeds`)

- Écouteur du cycle de besoins `BreedEvents.winterCompensation` (Highland) : `registerNeedsListener`, et non `registerFeedSource` (le `lastFedDay` ne doit pas changer).
- `BreedLogic.diseaseChanceFactor(Breed)` (Sussex ×0,7) : pur, testé en JUnit.

### Compost (`compost.CompostModule`)

- Le tag `terresvivantes:compostable` ajoute, directement dans `registerItemTags`, les raisins, les boutures de vigne, le riz, les panicules et les plants de riz (pas de hook).

### Dépendances ajoutées

- `automation` → `vineyard` (`VineyardApi`).
- `breeds` → `vet` (`VetApi`) et → `livestock` (`LivestockApi`, déjà présent).
- `rice` → `crop` (hooks), `vineyard` → `core.quality` (qualité des raisins et du vin).

## Dépendances entre packages

`crop` ↔ `soil` et `season` ↔ `climate` forment un cycle (facades statiques). Il est accepté en v0.2 : aucune de ces classes ne dépend d'un état initialisé à l'import.

## Contrats v0.6 (équipements & automatisation)

- `orchard.OrchardApi.harvestRipeLeaves(ServerLevel, BlockPos)` : `List<ItemStack>`. Récolte la feuille fruitière mûre
  (stade 3) comme le clic droit à main vide : la feuille revient au stade 0, les fruits sont rendus (rendement de la
  pollinisation compris). Liste vide si ce n'est pas un feuillage mûr. Ne range pas les fruits : l'appelant le fait.
  Implémentation dans `FruitLeavesBlock.harvestRipe`, partagée avec le clic droit.
- `soil.SoilApi.aerate(Level, BlockPos, int dOrganic)` : `boolean`. Aère une terre labourée, au plus une fois par bloc et
  par jour MC (`SoilModel.canAerate`, `SoilModel.mcDay` = temps de monde / 24000). Mémorisé dans `SoilChunkData` au champ
  optionnel `aerated_day` (position vers jour MC; absent avant v0.6, lecture rétrocompatible). `SoilApi.lastAeratedDay`.
- `automation` : `AutomationBlockEntity` (zone de travail `automation.logic.WorkArea`, curseur, inventaire borné).
  `HarvesterBlockEntity` parcourt un volume 5x5x3 (couche 0 au niveau de la machine, deux couches au-dessus) et appelle
  `CropApi.harvestAndReplant` ou `OrchardApi.harvestRipeLeaves`. `PlanterBlockEntity` plante sur la zone 5x5 au même niveau.
  Configuration `AutomationConfig` (`enabled`, `interval`, `radius`).
- `storage` : `CrateBlockEntity` (un type d'item, `capacity()`, `setContents`, `itemHandler()`), `SiloBlockEntity`
  (colonne de 1 à 8 blocs, `controller()`, `height()`, `itemHandler(side)`). Logique pure : `storage.logic.CrateLogic`,
  `SiloLogic`. Tag d'items `terresvivantes:silo_storable`.
- `pests.PestsApi` : `track`/`untrack` (épouvantails), `coveringScarecrow`, `protectsCrop`, `pestMultiplier` (branché sur
  `HealthApi.registerPestSuppressor`). Le corbeau est `pests.entity.Crow`; ses règles pures sont dans `pests.logic`.
- `implement` : grelinette (`BroadforkItem`), semoir (`SeederItem`), faucille en diamant (`DiamondSickleItem`); règles dans
  `implement.logic.ImplementsLogic`. Aucune façade publique.
- Garde-fou de rendu : `core.gametest.RenderShapeGameTests` vérifie que tout bloc `terresvivantes:*` a
  `RenderShape.MODEL` (aucune exception déclarée).

## Contrats v0.7 (économie, marché, guilde)

### Monnaie (module economy)

- Trois pièces : `copper_coin`, `silver_coin`, `gold_coin`. Ratio **9:1** : 9 cuivre = 1 argent, 9 argent = 1 or
  (`economy.logic.CoinLogic`, `COPPER_PER_SILVER` = `SILVER_PER_GOLD` = 9).
- Conversions dans les deux sens, en recettes sans forme : 1 argent donne 9 cuivre et 9 cuivre donnent 1 argent (idem
  or et argent). Fichiers `recipe/copper_from_silver`, `silver_from_copper`, `gold_from_silver`, `silver_from_gold`.
- `EconomyApi.coinsToCopper(copper, silver, gold)` et `copperToCoins(int)` (or, argent, cuivre; piles de 64).

### Économie (module economy)

- `economy.EconomyApi` (façade) :
  - `int priceOf(ServerLevel, ItemStack)` : cuivre par unité = base x qualité x saison x saturation; 0 si non vendable.
  - `void recordSale(ServerLevel, ItemStack)` : +1 point de saturation sur le marché de la dimension (sans effet si non vendable).
  - `int saturationPercent(ServerLevel, ItemStack)` : part du prix de base laissée par le marché (100 à 40).
  - `int reputation(ServerPlayer)`, `void addReputation(ServerPlayer, int)` (jamais négative), `String reputationTier(ServerPlayer)`
    (libellé français de référence), `ReputationLogic.Tier reputationTierOf(ServerPlayer)` (clé de traduction `langKey()`).
- `economy.EconomyModule.REPUTATION` : `Supplier<AttachmentType<Integer>>`, `copyOnDeath()`.
- `economy.MarketData` : SavedData par dimension (`get(ServerLevel)`, `pointsAt(id, day)`, `sell(id, day)`).
- Logique pure, sans `net.minecraft` : `economy.logic.PriceModel`, `MarketLogic` (saturation et jour MC, décroissance
  0.9 par jour), `CoinLogic`, `ReputationLogic` (`Tier`), `BasePrices` (lecture du JSON plat par expression régulière).
- Table de prix : `src/main/resources/data/terresvivantes/economy/base_prices.json`, chargée une fois par `EconomyApi`.
- Commandes : `/tv price`, `/tv reputation` (`economy.EconomyCommands`).

### Marché (module market)

- `market.MarketModule` : blocs `market_board` (tableau d'annonces, `MarketBoardBlock`, orienté) et `market_stall`
  (`MarketStallBlock` + `MarketStallBlockEntity`, `STALL_BE`, `STALL_MENU`); objets `contract` (`ContractItem`) et
  `market_ledger` (`LedgerItem`, grand livre des prix). Composant `terresvivantes:contract_data` (`ContractData`).
- `market.ContractBoard` (logique d'état, statique) : `onLevelTick(ServerLevel)` renouvelle le tableau au premier tick d'un
  nouveau jour MC (`MarketLogic.dayOf`), `openBoard`, `accept`, `deliverHeld`, `deliverMatching`, `deliver`, `onPlayerLogin`.
  Données par niveau : `ContractBoardData` (SavedData `terresvivantes_contracts`; `add`, `replace`, `remove`, `nextId`,
  `markRefreshed`, `addPenalty`).
- Contrats : `market.logic.ContractGenerator` (70 % de produits de saison, durée 2 à 5 jours, quantité selon le palier,
  réputation de 5 à 25; récompense = 1,5 x quantité x prix de base, arrondie); `ContractLogic.TARGET_OPEN` = 3 contrats ouverts, `EXPIRY_PENALTY` = 10 de
  réputation si un contrat accepté expire. Une livraison exige la qualité minimale (`ContractLogic.meetsQuality`).
- Étal : `StallLogic.SALE_INTERVAL_TICKS` = 2400; l'étal vend un item de ses 9 cases de vente à un villageois à 16 blocs au plus
  (`VILLAGER_RANGE`), de jour seulement; le paiement est `PAYMENT_PERCENT` = 90 % du prix, versé dans la caisse (case 9,
  `TillLogic` en or, argent et cuivre).
- Réseau : `market.MarketNetwork` (écran du tableau et de l'étal).
- Logique pure : `market.logic.Contract`, `ContractGenerator`, `ContractLogic`, `LedgerLogic` (`LINES` = 10), `StallLogic`,
  `TillLogic`.

### Guilde (module guild)

- `guild.GuildApi` (façade) :
  - `boolean isGuildMerchant(Villager)` : profession `GUILD_MERCHANT` (POI `guild_desk`).
  - `boolean applyTierLock(MerchantOffer, Tier)`, `void refreshOffers(ServerPlayer, Villager)` : une offre au-dessus du palier
    est mise en rupture (`setToOutOfStock`); elle se réapprovisionne avec le villageois.
  - `float qualityBonusChance(Player, String filiere)` : 0 sans titre, sinon `SpecializationLogic.QUALITY_BONUS_CHANCE` (0.05).
  - `float machineQualityBonusChance(Player, String machineId)` : la même chose, pour une machine de transformation
    (filière lue dans `SpecializationLogic.filiereOfMachine`). Appelée par le module processing (voir ci-dessous).
  - `units`, `hasTitle`, `recordSale`, `titleKey`.
- `guild.GuildTrades` : table des échanges par niveau 1 à 5 (`listingsAt(level)` pour `VillagerTradesEvent`, `levelOf(offer)`).
  Niveau 5 (vente) : exige en plus le sceau de la guilde (`guild_seal`). Chaque item n'apparaît que dans une offre.
- `guild.GuildEvents` : trades (`onVillagerTrades`), interaction (`onEntityInteract`), achat (`onTradeWithVillager`) : un achat
  donne +1 de réputation; le produit donné au marchand (coût A de l'offre) compte dans sa filière (`GuildApi.recordSale`).
- `guild.GuildModule.SPECIALIZATION` : `AttachmentType<Map<String, Integer>>` (unités vendues par filière, par joueur).
- Logique pure : `guild.logic.GuildLogic` (niveau n exige le palier d'index n-1 : `requiredTierIndex`, `isLocked`),
  `SpecializationLogic` (filières : maraicher, arboriculteur, eleveur, fromager, apiculteur; seuil `THRESHOLD` = 256 unités;
  `filiereOf(itemId)`, `filiereOfMachine(machineId)`, `add`, `reachedThreshold`, `hasTitle`).
- Commande `/tv titles` : `guild.GuildCommands.titles()` (permission 0), branchée dans la racine unique de `core.command.TVCommands`.

### Bonus de guilde dans le framework processing

- `ProcessingBlockEntity.rememberUser(Player)` : mémorise le dernier joueur qui a ouvert la machine (clic droit; UUID
  dans la BE, champ `LastUser`, sauvegardé).
- À la fin d'une recette (`serverTick`), `applyGuildBonus` : si le joueur est en ligne, que la machine a une filière et que
  la sortie est de qualité 1 ou 2, la qualité passe à +1 avec la probabilité `GuildApi.machineQualityBonusChance`
  (plafonnée à 3). Une sortie sans qualité n'est pas modifiée.
- Mapping machine vers filière : `cheese_vat` et `aging_shelf` vers fromager; `fruit_press` vers arboriculteur;
  `millstone` vers maraicher; `churn` vers eleveur. `honey_extractor` (apiculture, pas processing) et les autres machines n'ont pas de bonus.
  Test JUnit pur : `SpecializationLogicTest.machinesMapToTheirFiliere`.

### Dépendances et configuration

- `economy` lit `season.SeasonApi`, `core.quality.QualityModule`, `crop.logic.CropDefinitions`, `orchard.logic.FruitTreeDefinitions`.
  `market` et `guild` appellent `economy` (prix, réputation, saturation). `processing` appelle `guild` pour le bonus.
- Racine de commandes : `core.command.TVCommands` (`season`, `showcase`, `price`, `reputation`, `titles`); `season` et `showcase`
  demandent le niveau 2, les autres sont en permission 0.
- Aucune config nouvelle en v0.7 : market et guild n'ont pas de `XxxConfig` (rien à enregistrer dans `TerresVivantes`).
- Vitrine `/tv showcase` : section économie en z 34 à 39 (`ShowcaseBuilder.DEPTH` = 40), tableau (x2), étal (x6), bureau
  de la guilde (x12) et un villageois marchand persistant (x12, z33).
