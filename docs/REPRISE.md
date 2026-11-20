# Reprise du projet (après v0.6 Équipements & automatisation)

## 1. État exact

- Version : `mod_version=0.6.0` (NeoForge 21.1.267, Minecraft 1.21.1). v0.6 livrée, voir `docs/STATUS.md`.
- Branche : `claude/terres-vivantes-minecraft-g4o0hy`. Dernier commit : celui qui ajoute la v0.6 (`git log -1`).
- Vérifications (sur ce commit) :
  - `./gradlew build` : OK, 309 tests JUnit verts (dont `GeneratedResourcesTest`).
  - `./gradlew runData` : OK, `src/generated` régénéré.
  - `./gradlew runGameTestServer` : 93/93 à chaque passage, trois passages de suite; aucune erreur de parsing de recette.
- Modules et statut :
  - season, climate, soil, crop, compost, food, tools, orchard, water, health, pollination : livrés (v0.1 à v0.3).
  - flora, insects, apiculture : livrés (v0.3).
  - livestock, husbandry, vet, duck : livrés (v0.4).
  - core.quality, processing, milling, dairy, preserving, kitchen : livrés (v0.5).
  - implement, automation, storage, pests : livrés (v0.6).
  - debug : `/tv showcase` (fleurs, haie, apiaire, insectes, enclos d'élevage, rangée de transformation, section équipements).

## 2. Problèmes connus et dettes

- **Machines invisibles en jeu (QA client)** : cause non trouvée. Le `getRenderShape` des blocs du mod est `MODEL`
  (garde-fou `RenderShapeGameTests`), blockstates et modèles présents. À diagnostiquer en client (BlockEntityRenderer,
  textures, couche de rendu).
- Rendu client non vérifié : caisse, silo, champs de la vitrine (v0.6), machines et écrans (v0.5).
- Tonte et traite approximées : NeoForge 21.1 n'expose pas d'événement dédié.
- Bonus de lapins de l'épouvantail : non fait.
- Combustible du fumoir : bûches comprises (à confirmer en équilibrage).
- Sprites encore faibles : `bee_smoker`, `aphid`, `pollen`, sprinklers.
- Citrouille et pastèque hors pollinisation. Entités sans loot (papillons, coccinelles).
- Pas de GUI pour l'apiaire ni pour le silo.
- Réseau : `maven.parchmentmc.org` redirige vers un hôte bloqué par le proxy; Parchment n'est pas utilisé.

## 3. Méthode d'orchestration (ce qui a fonctionné)

a. Agent « contrats » séquentiel, dans le dépôt principal : façades, hooks, modules vides enregistrés, documentation.
b. Agents modules en parallèle, en worktrees isolés. Règles : ne modifier que son package; les configs `XxxConfig` sont branchées
   par l'intégrateur; ne pas committer `src/generated` avant runData; `build`, `runData` et `runGameTestServer` doivent être verts.
c. Agent textures en worktree (`tools/textures`, Pillow, grilles 16x16).
d. Intégrateur : merges `--no-ff` (conflits sur `TerresVivantes.java` : garder toutes les lignes), configs, `runData`,
   GameTests (trois passages), documentation, push, nettoyage des worktrees.
   Les commandes Gradle se lancent en premier plan avec timeout; un seul Gradle à la fois.
   Un tag ou une recette manquant(e) dans le log n'apparaît qu'après `runData` : le relancer après une fusion.

## 4. Prochaine étape : v0.7 Économie

Plan (à détailler dans `docs/DESIGN.md` avant de coder) :

- Marché de village et étal : vente des produits (cultures, transformation, élevage) à des prix liés à la saison et à la qualité.
- Contrats de livraison : commandes de villageois avec délai et quantité, rémunérés en monnaie.
- Réputation : progression par livraisons tenues, débloque des contrats plus rentables.
- Monnaie : une unité simple, sans économie complexe; gains et dépenses visibles à l'almanach.
- Spécialisation : bonus de métier selon les modules utilisés (élevage, verger, transformation).
- Avant de commencer : QA visuelle en client v0.5 et v0.6 (machines, caisse, silo, vitrine), puis polish des sprites faibles.
