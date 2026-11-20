# Guide agent

## Commandes

| Action | Commande |
|---|---|
| Compiler + tests | `./gradlew build` |
| Tests seuls | `./gradlew test` |
| Générer les ressources | `./gradlew runData` |
| Tests de jeu (serveur) | `./gradlew runGameTestServer` (s'arrête seul) |
| Client / serveur de dev | `./gradlew runClient` / `./gradlew runServer` |

Toujours lancer `./gradlew build` avant un commit. Si un changement touche la datagen, lancer aussi `./gradlew runData`.

## Conventions

- **Noms de registre** : snake_case anglais (`wheat_seeds_crop`, `compost_bin`).
- **Lang** : toute clé de traduction a une valeur `en_us` et une valeur `fr_fr`. Passer par `LangSink.add(clé, enUs, frFr)`.
- **Textures** : PNG 16x16 dans `src/main/resources/assets/terresvivantes/textures/` (`block/`, `item/`).
- **Textures générées** : les textures sont produites par des scripts Python dans `tools/` (Pillow). Ne pas les dessiner à la main sans script reproductible.
- **Logique** : une règle métier vit dans `<module>/logic/`, sans import `net.minecraft`, avec un test JUnit dans `src/test/java`.
- **Pas de BlockEntity tickant** pour les cultures : utiliser `randomTick`.

## Règles anti-conflit

Plusieurs agents travaillent en parallèle. Chacun ne modifie que son module.

- Un agent de module modifie uniquement `src/main/java/fr/terresvivantes/<module>/`, ses tests et ses ressources propres (textures, modèles, lang via hook).
- Fichiers **réservés à l'intégrateur**, à ne pas modifier depuis un module :
  - `TerresVivantes.java` (liste des modules, sauf demande explicite)
  - `datagen/`
  - `core/`
  - `build.gradle`, `gradle.properties`, `settings.gradle`
  - `docs/ARCHITECTURE.md`, `docs/AGENT_GUIDE.md`
- Si un module a besoin d'une modification réservée, il le signale dans son rapport au lieu de la faire.
- Pour un fichier partagé non réservé (ex. `docs/STATUS.md`), ajouter une ligne dans sa section; ne pas réécrire le fichier.

## Format de rapport compact

Chaque rapport de fin de tâche tient en quelques lignes :

```
Module : <nom>
Fait : <liste courte>
Vérifié : build OK / tests OK / runData OK (ou non lancé, avec raison)
Fichiers partagés demandés : <aucun ou liste>
Problèmes : <aucun ou liste>
```

## Créer une machine de transformation (v0.5)

1. Déclarer la machine : `public static final MachineType X_TYPE = ProcessingApi.registerMachine("x", 2, false);`
   (identifiant unique, 1 à 4 entrées, conteneur oui ou non).
2. Bloc : `public final class XBlock extends ProcessingBlock` avec `CODEC = BlockBehaviour.simpleCodec(XBlock::new)`,
   `codec()` qui le renvoie, et `newBlockEntity` qui crée `XBlockEntity`.
3. BlockEntity : `public final class XBlockEntity extends ProcessingBlockEntity` avec le constructeur
   `super(X_BE.get(), pos, state, X_TYPE)`; surcharger `craftingTick` et `craftFinished` pour les sons et particules.
4. Dans le module : `DeferredBlock`, `BlockItem`, `BlockEntityType.Builder.of(XBlockEntity::new, X.get()).build(null)`;
   sur le bus mod, `modBus.addListener(e -> ProcessingApi.registerItemHandler(e, X_BE.get()))`.
5. Recettes : fichiers JSON `data/terresvivantes/recipe/<nom>.json` avec `"type": "terresvivantes:processing"` et
   `"machine": "x"` (format dans `ProcessingRecipeSerializer`).
6. Datagen : `registerBlockStates` (modèles avec `ProcessingBlock.ACTIVE`), `registerItemModels` (modèle d'item autonome,
   sans dépendre d'un modèle de bloc créé plus tard), `registerLootTables` (drop du bloc), `registerLang` (en et fr).
7. Tests : un GameTest de mouture (voir `milling/gametest/MillingGameTests`) et la logique pure en JUnit.

