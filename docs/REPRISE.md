# Reprise du projet (après v0.7 Économie)

Document de reprise : à lire en premier. Il décrit l'état exact, ce qui est fait, ce qui reste et comment travailler.
Détails par version : `docs/STATUS.md`. Règles de design : `docs/DESIGN.md`. Contrats entre modules : `docs/ARCHITECTURE.md`.
Consignes pour les agents : `docs/AGENT_GUIDE.md`.

## 1. État exact

- Version : `mod_version=0.7.0` (NeoForge 21.1.267, Minecraft 1.21.1). v0.7 livrée.
- Branche : `claude/terres-vivantes-minecraft-g4o0hy`, poussée sur `origin`.
- Commits de la v0.7 :
  - `01f68d4` : intégration (fusion des branches market et guild, bonus de guilde, `/tv titles` dans la racine unique,
    section économie de la vitrine, version 0.7.0, `src/generated` régénéré).
  - Le commit qui ajoute ce fichier et les autres docs : `git log -1` (son message commence par « docs(v0.7) »).
- Arbre de travail propre après le commit de docs. Aucune fusion en cours. Worktrees de `.claude/worktrees` supprimés.
- Vérifications sur `01f68d4` :
  - `./gradlew build` : OK, 345 tests JUnit verts (dont `GeneratedResourcesTest`).
  - `./gradlew runData` : OK, `src/generated` régénéré, aucune erreur.
  - `./gradlew runGameTestServer` : 104/104 à trois passages consécutifs; aucune erreur « Parsing error » / « Couldn't parse ».
- Vérification des versions antérieures : build et GameTests rejoués sur le commit de livraison de chaque version
  (v0.1 à v0.6) : les chiffres de `docs/STATUS.md` sont exacts.
- Modules : 32, listés dans `docs/STATUS.md`. Configs COMMON : `terresvivantes-season`, `-soil`, `-compost`, `-crop`,
  `-water`, `-health`, `-orchard`, `-flora`, `-insects`, `-apiculture`, `-livestock`, `-husbandry`, `-vet`, `-duck`,
  `-automation`, `-pests`. Les modules v0.7 n'ont pas de config.

## 2. Fonctionnalités réalisées, par version

- **v0.1 Fondations** : saisons, climat local, sol (N/P/K, matière organique, compost, fumier, cendre), 12 cultures
  (graines, récolte à main vide), 10 plats, outils (faucille, arrosoir, sonde, almanach).
- **v0.2 Vergers, eau, santé** : serres et hiver, rotation des cultures, six espèces fruitières, arroseurs, tonneau de pluie,
  sécheresse, maladies et flétrissement, fongicide, `/tv showcase`.
- **v0.3 Biodiversité** : indice de pollinisation et de biodiversité, flore sauvage et haies, insectes (papillons, coccinelles,
  pucerons, lucioles), apiculture (apiaire, extracteur, miels typés).
- **v0.4 Élevage** : génome des animaux, besoins et reproduction, auge, nichoir, ensilage, santé animale (maladies,
  parasites, sel), canard avec œufs et productions.
- **v0.5 Transformation** : qualité générique (1 à 3 étoiles), infrastructure de machines (recettes JSON, conteneur, sortie),
  meule, pressoir, baratte, cuve à fromage, étagère d'affinage, tonneau, séchoir, fumoir, marmite (12 plats).
- **v0.6 Équipements** : grelinette, semoir, faucille en diamant, récolteuse, planteuse, caisse, silo, corbeau, épouvantail.
- **v0.7 Économie** : monnaie 9:1 (cuivre, argent, or) avec conversions dans les deux sens; prix (base, qualité, saison,
  saturation); réputation par paliers; tableau d'annonces avec contrats quotidiens; étal qui vend à un villageois de jour;
  guilde (marchand, échanges par niveau, verrou par réputation, titres de spécialisation à 256 unités); bonus de qualité
  des machines de la filière pour le dernier joueur; commandes `/tv price`, `/tv reputation`, `/tv titles`.

## 3. Travaux en cours

Aucun. Pause propre : tout est commité et poussé, aucun travail partiellement écrit.

## 4. Problèmes connus et dettes

- **Machines invisibles en jeu (QA client)** : non confirmé. Analyse statique OK : `getRenderShape` des blocs du mod est
  `MODEL` (garde-fou `RenderShapeGameTests`), blockstates et modèles présents. Probablement un artefact du rendu logiciel
  Xvfb (`LIBGL_ALWAYS_SOFTWARE=1`). À vérifier sur une vraie GPU avant de chercher une cause dans le code.
- **QA visuelle client incomplète** : caisse, étal, tableau d'annonces, bureau de la guilde, silo, écrans (machines,
  apiaire, silo), canard, corbeau. Procédure : section 6.
- **Tonte et traite approximées** : NeoForge 21.1 n'expose pas d'événement dédié; la laine et le lait sont calculés à partir
  du multiplicateur.
- **Auge sans bord creux** : le modèle n'a pas de rebord évidé.
- **Bonus de lapins de l'épouvantail** : non fait (prévu au design).
- **Sprites faibles** : `bee_smoker`, `aphid`, `pollen`, `smoked_fish`, chapeau du marchand, soleil de la pièce d'or.
- **Économie** : bonus de qualité des machines testé seulement par le mapping (JUnit), pas de GameTest de bout en bout;
  réputation après mort non testée; achat PNJ et contrats testés par GameTest, pas en jeu.
- **Combustible du fumoir** : les bûches sont comprises (à confirmer en équilibrage).
- **Citrouille et pastèque** hors pollinisation. Entités sans loot (papillons, coccinelles).
- **Pas de GUI** pour l'apiaire ni pour le silo.
- **Historique git** : l'attribution `Co-Authored-By` des commits est hétérogène (plusieurs modèles selon les sessions).
- **Parchment non utilisé** : `maven.parchmentmc.org` redirige vers un hôte bloqué par le proxy. Les noms de paramètres sont
  ceux de Mojang.

## 5. Méthode d'orchestration (ce qui a fonctionné)

a. Contrats séquentiels dans le dépôt principal : façades, hooks, modules vides enregistrés, documentation.
b. Modules en parallèle, en worktrees isolés (`.claude/worktrees`). Règles : ne modifier que son package; les configs
   `XxxConfig` sont branchées par l'intégrateur; ne pas committer `src/generated` dans un worktree; `build`, `runData` et
   `runGameTestServer` doivent être verts avant de rendre la main.
c. Textures en worktree dédié (`tools/textures`, Pillow, grilles 16x16). Un seul script idempotent : `generate_all.py`.
d. Intégrateur (dans le dépôt principal) : fusions `--no-ff`, conflits sur les PNG de pièces résolus par la version de
   `textures-v07`, configs, `TVCommands` pour la racine `/tv`, `runData`, GameTests (trois passages), documentation, push,
   nettoyage des worktrees et des branches locales.
e. Gradle en premier plan, avec timeout; un seul Gradle à la fois. Une recette ou un tag manquant n'apparaît dans le log
   qu'après `runData`: le relancer après une fusion. `./gradlew --stop` en fin de session.
f. Les JUnit sont rejouables avec `./gradlew cleanTest test` (Gradle peut considérer la tâche à jour sans cela).

## 6. Procédure de QA visuelle client

- `xvfb-run` avec `LIBGL_ALWAYS_SOFTWARE=1`, monde superplat créatif, `allowCommands` via nbtlib, `--quickPlaySingleplayer`,
  puis `/tv showcase`. Les captures vont dans `docs/qa`.
- À vérifier dans l'ordre : machines de transformation (rangée z 17), marmite, silo, caisse, champs de la vitrine (z 23 à 31),
  section économie (z 34 à 39), enclos d'élevage.

## 7. Prochaines étapes (après la roadmap v0.1 à v0.7)

1. **v0.8 « Polish »** : passe qualité et équilibrage.
   - QA visuelle complète avec la procédure ci-dessus; corriger les sprites faibles.
   - Équilibrage prix, croissance et maladies via les configs.
   - Performances : profiler les ticks avec 500 cultures et 50 animaux dans un GameTest.
   - Serveur dédié : lancer `runServer` et vérifier qu'aucune classe client n'est chargée.
2. **Guide joueur en jeu** : livre « Manuel du fermier » (type Patchouli, simple, écrit généré), ou pages d'almanach.
3. **v0.9 Contenu** : vignes et vin (vigne grimpante, pressoir à raisin, cave); riz en rizière inondée; nouvelles races
   animales visibles (variantes de texture); apiculture avancée (reines, génétique).
4. **Préparation de release** : jar, README joueur, licence, captures.
