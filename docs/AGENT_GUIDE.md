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
