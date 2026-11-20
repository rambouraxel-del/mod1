# Reprise du projet (après v0.4 Élevage)

## 1. État exact

- Version : `mod_version=0.4.0` (NeoForge 21.1.267, Minecraft 1.21.1).
- Branche : `claude/terres-vivantes-minecraft-g4o0hy`. Dernier commit : celui qui ajoute la v0.4 (`git log -1`).
- Vérifications (sur ce commit) :
  - `./gradlew build` : OK, 232 tests JUnit verts.
  - `./gradlew runData` : OK, `src/generated` régénéré, `GeneratedResourcesTest` vert.
  - `./gradlew runGameTestServer` : 54/54, trois passages de suite exigés, 30 passages consécutifs verts sur la version finale.
- Modules et statut :
  - season, climate, soil, crop, compost, food, tools, orchard, water, health, pollination : livrés (v0.1 à v0.3).
  - flora, insects, apiculture : livrés (v0.3).
  - livestock, husbandry, vet, duck : livrés (v0.4).
  - debug : `/tv showcase` (inclut fleurs, haie, apiaire, insectes, et l'enclos d'élevage).

## 2. Problèmes connus et dettes

- Tonte et traite approximées : NeoForge 21.1 n'expose pas d'événement dédié.
- Auge sans bord creux (modèle).
- QA visuelle en client incomplète, canard et élevage compris. Procédure qui fonctionne : `xvfb-run` avec
  `LIBGL_ALWAYS_SOFTWARE=1`, monde superplat créatif, `allowCommands` écrit via nbtlib, `--quickPlaySingleplayer`, puis `/tv showcase`.
- Sprites encore faibles : `bee_smoker`, `aphid` et `pollen` (particules), sprinklers.
- Citrouille et pastèque hors pollinisation (croissance sur tige, hors `CropGrowthHooks`).
- Entités sans loot (papillons, coccinelles).
- Pas de GUI pour l'apiaire; pas de relâcher des insectes depuis les bocaux.
- Tests de jeu : une zone de test n'est pas toujours « entity ticking ». Les tests qui dépendent du tick des entités les pilotent explicitement.
- Réseau : `maven.parchmentmc.org` redirige vers un hôte bloqué par le proxy; Parchment n'est pas utilisé.

## 3. Méthode d'orchestration (ce qui a fonctionné)

a. Agent « contrats » séquentiel, dans le dépôt principal : façades, hooks, modules vides enregistrés, documentation.
b. Agents modules en parallèle, en worktrees isolés. Règles : ne modifier que son package; les configs `XxxConfig` sont branchées
   par l'intégrateur; ne pas committer `src/generated`; `build`, `runData` et `runGameTestServer` doivent être verts.
c. Agent textures en worktree (`tools/textures`, Pillow, grilles 16x16).
d. Intégrateur : merges `--no-ff`, configs, `runData`, GameTests (plusieurs passages), documentation, push, nettoyage des worktrees.

## 4. Prochaine étape : v0.5 Transformation

- Voir `docs/ROADMAP.md` (v0.5 transformation, puis v0.6 équipements, v0.7 économie).
- Avant de commencer : QA visuelle en client de la v0.4 (canard, élevage, auge, nichoir, pierre à sel).
- Reprendre le contrat `milk_quality` (composant déjà défini, utilisé en v0.5 selon `DuckModule`).
