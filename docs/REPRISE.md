# Reprise du projet (après v0.5 Transformation)

## 1. État exact

- Version : `mod_version=0.5.0` (NeoForge 21.1.267, Minecraft 1.21.1). v0.5 « Transformation » livrée, voir `docs/STATUS.md`.
- Branche : `claude/terres-vivantes-minecraft-g4o0hy`. Dernier commit : celui qui ajoute la v0.5 (`git log -1`).
- Vérifications (sur ce commit) :
  - `./gradlew build` : OK, 263 tests JUnit verts (dont `GeneratedResourcesTest`).
  - `./gradlew runData` : OK, `src/generated` régénéré.
  - `./gradlew runGameTestServer` : 75/75 à chaque passage, trois passages de suite; aucune erreur de parsing de recette.
- Modules et statut :
  - season, climate, soil, crop, compost, food, tools, orchard, water, health, pollination : livrés (v0.1 à v0.3).
  - flora, insects, apiculture : livrés (v0.3).
  - livestock, husbandry, vet, duck : livrés (v0.4).
  - core.quality, processing, milling, dairy, preserving, kitchen : livrés (v0.5).
  - debug : `/tv showcase` (inclut fleurs, haie, apiaire, insectes, enclos d'élevage et rangée de transformation).

## 2. Problèmes connus et dettes

- Tonte et traite approximées : NeoForge 21.1 n'expose pas d'événement dédié.
- QA visuelle en client incomplète (élevage, canard, machines v0.5). Procédure : `xvfb-run` avec
  `LIBGL_ALWAYS_SOFTWARE=1`, monde superplat créatif, `allowCommands` écrit via nbtlib, `--quickPlaySingleplayer`, puis `/tv showcase`.
- Combustible du fumoir : bûches comprises (à confirmer en équilibrage).
- Sprites encore faibles : `bee_smoker`, `aphid`, `pollen`, sprinklers.
- Citrouille et pastèque hors pollinisation.
- Entités sans loot (papillons, coccinelles).
- Pas de GUI pour l'apiaire; pas de relâcher des insectes depuis les bocaux.
- Réseau : `maven.parchmentmc.org` redirige vers un hôte bloqué par le proxy; Parchment n'est pas utilisé.

## 3. Méthode d'orchestration (ce qui a fonctionné)

a. Agent « contrats » séquentiel, dans le dépôt principal : façades, hooks, modules vides enregistrés, documentation.
b. Agents modules en parallèle, en worktrees isolés. Règles : ne modifier que son package; les configs `XxxConfig` sont branchées
   par l'intégrateur; ne pas committer `src/generated` avant runData; `build`, `runData` et `runGameTestServer` doivent être verts.
c. Agent textures en worktree (`tools/textures`, Pillow, grilles 16x16).
d. Intégrateur : merges `--no-ff`, configs, `runData`, GameTests (trois passages), documentation, push, nettoyage des worktrees.
   Les commandes Gradle se lancent en premier plan avec timeout; un seul Gradle à la fois.

## 4. Prochaine étape : v0.6 Équipements & automatisation

Plan (à détailler dans `docs/DESIGN.md` avant de coder) :

- Semoir : plante graines et bulbes sur une ligne, consomme les graines de l'inventaire d'un coffre voisin.
- Charrue attelée : prépare la terre sur une bande (étend le labour), tirée par un animal de l'élevage.
- Silos et granges : stockage de récoltes et d'ensilage avec qualité conservée (composant `quality`).
- Convoyeurs et trémies agricoles : déplacement d'items entre machines (étend `ProcessingApi` et les trémies).
- Épouvantail : répulsif pour les ravageurs (branché sur `health.HealthApi.registerPestSuppressor`).
- Avant de commencer : QA visuelle en client de la v0.5 (machines, écrans, jus).
