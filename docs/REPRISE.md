# Reprise du projet (pause après v0.3)

## 1. État exact

- Version : `mod_version=0.3.0` (NeoForge 21.1.267, Minecraft 1.21.1).
- Branche : `claude/terres-vivantes-minecraft-g4o0hy`. Dernier commit : celui qui ajoute ce fichier (`git log -1`).
- Vérifications (sur ce commit) :
  - `./gradlew build` : OK, 180 tests JUnit, 0 échec.
  - `./gradlew runData` : OK, `src/generated` régénéré.
  - `./gradlew runGameTestServer` : 3 passages de suite, 40/40 à chaque fois.
- Modules et statut :
  - season, climate, soil, crop, compost, food, tools, orchard, water, health, pollination : livrés (v0.1 à v0.3).
  - flora : livré (4 fleurs sauvages, pots, haie d'aubépine).
  - insects : livré (papillons, coccinelle, pucerons, lucioles, filet, bocal à papillon).
  - apiculture : livré (apiaire, extracteur, enfumoir, miels typés).
  - debug : `/tv showcase` (inclut fleurs, haie, apiaire, insectes).

## 2. Problèmes connus et dettes

- QA visuelle en client incomplète. Le rendu logiciel (Xvfb) est lent et laisse des chunks noirs.
  Procédure qui fonctionne : `xvfb-run` avec `LIBGL_ALWAYS_SOFTWARE=1`, monde superplat créatif, `allowCommands`
  écrit via nbtlib, `--quickPlaySingleplayer`, puis `/tv showcase`.
- Sprites encore faibles : `bee_smoker`, `aphid` et `pollen` (particules), sprinklers.
- Citrouille et pastèque hors pollinisation (croissance sur tige, hors `CropGrowthHooks`).
- Entités sans loot (papillons, coccinelles).
- Pas de GUI pour l'apiaire.
- Pas de relâcher des insectes depuis les bocaux.
- Réseau : `maven.parchmentmc.org` redirige vers un hôte bloqué par le proxy; Parchment n'est pas utilisé.
  Les hôtes NeoForge et Mojang ont été ajoutés à l'environnement cloud.

## 3. Méthode d'orchestration (ce qui a fonctionné)

a. Agent « contrats » séquentiel, dans le dépôt principal : façades, hooks, modules vides enregistrés, documentation.
b. Agents modules en parallèle, en worktrees isolés. Démarrage par fetch puis reset sur la branche, plus merge de
   la branche textures. Règles : ne modifier que son package; les configs `XxxConfig` sont branchées par l'intégrateur;
   ne pas committer `src/generated`; `build`, `runData` et `runGameTestServer` doivent être verts.
c. Agent textures en worktree (`tools/textures`, Pillow, grilles 16x16).
d. Intégrateur : merges `--no-ff`, configs, `runData`, GameTests trois fois, documentation, push, nettoyage des
   worktrees.

## 4. Prochaine étape : v0.4 Élevage (plan prêt)

- Contrats : module `livestock`. Génome par Data Attachment d'entité : productivité, taille, résistance, fertilité
  (0 à 10); héritage moyen des parents avec mutation. Besoins : nourriture quotidienne, pâturage sur herbe hors hiver.
  Reproduction au printemps et en été, configurable. `LivestockApi`; item `livestock_ledger` qui affiche gènes et état.
- En parallèle :
  1. Bâtiments et alimentation : `feeding_trough` (foin, grain, ensilage; alimentation automatique dans un rayon),
     `nesting_box` (les poules pondent dedans), ensilage et `feed_mix` à partir d'orge, maïs et soja.
  2. Santé animale : maladies par surpopulation ou sous-alimentation, `veterinary_kit`.
  3. Productions et nouvelle espèce : canard (entité, œufs, plumes, nage); qualité du lait, de la laine et des œufs
     selon gènes et alimentation; lait de chèvre.
  4. Textures v0.4.
- Ensuite : v0.5 transformation, v0.6 équipements, v0.7 économie (voir `docs/ROADMAP.md`).
