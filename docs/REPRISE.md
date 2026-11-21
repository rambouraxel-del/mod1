# Reprise du projet (après v0.8 Polish)

Document de reprise : à lire en premier. Il décrit l'état exact, ce qui est fait, ce qui reste et comment travailler.
Détails par version : `docs/STATUS.md`. Règles de design : `docs/DESIGN.md`. Contrats entre modules : `docs/ARCHITECTURE.md`.
Consignes pour les agents : `docs/AGENT_GUIDE.md`. Équilibrage : `docs/BALANCE.md`. Performances : `docs/PERFORMANCE.md`.

## 1. État exact

- Version : `mod_version=0.8.0` (NeoForge 21.1.267, Minecraft 1.21.1). v0.8 « Polish » livrée.
- Branche : `claude/terres-vivantes-minecraft-g4o0hy`, poussée sur `origin`. Le commit de tête est celui de cette
  documentation (`git log -1`).
- Intégration v0.8 : quatre fusions `--no-ff` dans la branche :
  - `polish-sprites` : passe de qualité sur les sprites faibles;
  - `worktree-agent-a25d7b83c04982174` : serveur dédié (`ServerClassIsolationTest`) et test de charge (`PerformanceGameTests`);
  - `worktree-agent-a03641d90f0c6defe` : manuel du fermier (`guide`);
  - `worktree-agent-adc93231179a0a342` : équilibrage et simulateur (`BalanceSimulationTest`).
  Seul conflit : `tools/textures/preview.png` (binaire), régénéré par le script.
- Corrections d'équilibrage et de vérification faites à l'intégration : prix du fromage de chèvre (16 -> 11, sous le fromage
  affiné à 14), famine (perte de santé de 5 -> 1 par cycle), manuel du fermier réécrit pour les valeurs finales.
- Vérifications sur ce commit :
  - `./gradlew build` : OK, 355 tests JUnit verts (dont `GeneratedResourcesTest`, `ServerClassIsolationTest`, `BalanceSimulationTest`).
  - `./gradlew runData` : OK, `src/generated` régénéré.
  - `./gradlew runGameTestServer` : 106/106 à trois passages consécutifs.
  - `./gradlew runServer` : « Done (0.546s) », arrêt par SIGTERM sans erreur.
- Arbre de travail propre. Branche `qa-v05` supprimée. Worktrees de `.claude/worktrees` et branches fusionnées supprimés.
- Modules : 33, listés dans `docs/STATUS.md`. Configs COMMON : `terresvivantes-season`, `-soil`, `-compost`, `-crop`,
  `-water`, `-health`, `-orchard`, `-flora`, `-insects`, `-apiculture`, `-livestock`, `-husbandry`, `-vet`, `-duck`,
  `-automation`, `-pests`, `-guide` (manuel). Les autres modules n'ont pas de config.

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
- **v0.8 Polish** : sprites faibles repris; manuel du fermier en jeu (item, livre fr et en, 11 chapitres, remise à la première
  connexion); équilibrage des cultures, de la maladie, des prix et des contrats (`docs/BALANCE.md`, simulateur JUnit);
  famine ramenée à 5 jours MC; serveur dédié sans classe client (test de garde-fou); test de charge serveur
  (`docs/PERFORMANCE.md`, surcoût de 3,6 ms/tick pour 500 cultures et 50 animaux, seuil 15).

## 3. Travaux en cours

Aucun. Pause propre : tout est commité et poussé, aucun travail partiellement écrit.

## 4. Problèmes connus et dettes

- **Machines invisibles en jeu (client)** : non confirmé. Analyse statique OK : `getRenderShape` des blocs du mod est
  `MODEL` (garde-fou `RenderShapeGameTests`), blockstates et modèles présents. Probablement un artefact du rendu logiciel
  Xvfb (`LIBGL_ALWAYS_SOFTWARE=1`). À vérifier sur une vraie carte graphique avant de chercher une cause dans le code.
- **QA visuelle client incomplète** : caisse, étal, tableau d'annonces, bureau de la guilde, silo, écrans (machines,
  apiaire, silo), canard, corbeau, livre du manuel du fermier. Procédure : section 6.
- **Tonte et traite approximées** : NeoForge 21.1 n'expose pas d'événement dédié; la laine et le lait sont calculés à
  partir du multiplicateur.
- **Auge sans bord creux** : le modèle n'a pas de rebord évidé.
- **Bonus de lapins de l'épouvantail** : non fait (prévu au design).
- **Équilibrage, limites connues** (détail dans `docs/BALANCE.md`) :
  - la famine dure 5 jours MC à satiété nulle : une perte de 1,25 par cycle donnerait 4 jours exacts, mais la santé est un entier;
  - le lait ne varie qu'en qualité selon l'alimentation, jamais en quantité;
  - les étagères d'affinage (capacité) et la nuit (lumière insuffisante) ne sont pas simulées.
- **Économie** : bonus de qualité des machines testé seulement par le mapping (JUnit), pas de GameTest de bout en bout;
  réputation après mort non testée; achat PNJ et contrats testés par GameTest, pas en jeu.
- **Combustible du fumoir** : les bûches sont comprises (à confirmer en équilibrage).
- **Citrouille et pastèque** hors pollinisation. Entités sans loot (papillons, coccinelles).
- **Pas de GUI** pour l'apiaire ni pour le silo.
- **Historique git** : l'attribution `Co-Authored-By` des commits est hétérogène (plusieurs modèles selon les sessions).
- **Parchment non utilisé** : `maven.parchmentmc.org` redirige vers un hôte bloqué par le proxy. Les noms de paramètres sont
  ceux de Mojang.
- **Licence** : `mod_license=MIT` dans `gradle.properties`, mais aucun fichier `LICENSE` dans le dépôt (à ajouter avant la 1.0).

## 5. Méthode d'orchestration (ce qui a fonctionné)

a. Contrats séquentiels dans le dépôt principal : façades, hooks, modules vides enregistrés, documentation.
b. Modules en parallèle, en worktrees isolés (`.claude/worktrees`). Règles : ne modifier que son package; les configs
   `XxxConfig` sont branchées par l'intégrateur; ne pas committer `src/generated` dans un worktree; `build`, `runData` et
   `runGameTestServer` doivent être verts avant de rendre la main.
c. Textures en worktree dédié (`tools/textures`, Pillow, grilles 16x16). Un seul script idempotent : `generate_all.py`.
d. Intégrateur (dans le dépôt principal) : fusions `--no-ff`, conflits sur les binaires (PNG de pièces, aperçu des textures)
   résolus puis régénérés par le script, configs, `TVCommands` pour la racine `/tv`, `runData`, GameTests (trois passages),
   documentation, push, nettoyage des worktrees et des branches locales.
e. Gradle en premier plan, avec timeout; un seul Gradle à la fois. Une recette ou un tag manquant n'apparaît dans le log
   qu'après `runData`: le relancer après une fusion. `./gradlew --stop` en fin de session.
f. Les JUnit sont rejouables avec `./gradlew cleanTest test` (Gradle peut considérer la tâche à jour sans cela).
g. Contraintes de contenu testées : le manuel impose 3 à 6 pages par chapitre et 300 caractères au plus par page
   (`GuideContentTest`). Pour ajouter une information à un chapitre plein, réécrire une page plutôt qu'en ajouter une.

## 6. Procédure de QA visuelle client

- `xvfb-run` avec `LIBGL_ALWAYS_SOFTWARE=1`, monde superplat créatif, `allowCommands` via nbtlib,
  `--quickPlaySingleplayer`, puis `/tv showcase`. Les captures vont dans `docs/qa`.
- À vérifier dans l'ordre : machines de transformation (rangée z 17), marmite, silo, caisse, champs de la vitrine (z 23 à 31),
  section économie (z 34 à 39), enclos d'élevage, livre du manuel du fermier (clic droit sur l'item).

## 7. Prochaines étapes

1. **v0.9 Contenu** :
   - vignes et vin : vigne grimpante sur treillis, raisins, pressoir à raisin, jus de raisin, tonneau, puis vin dont la
     qualité et le millésime dépendent de la saison et de l'affinage;
   - riz en rizière inondée;
   - races animales visibles (variantes de texture de vache, mouton et poule selon le génome);
   - apiculture avancée (reines, génétique).
2. **Release 1.0** :
   - jar : `./gradlew build` produit `build/libs/terresvivantes-<version>.jar`;
   - README joueur : rédigé en v0.8, à relire après la QA visuelle;
   - captures : à produire pendant la QA visuelle (procédure section 6);
   - licence : ajouter le fichier `LICENSE` (MIT déclaré).
